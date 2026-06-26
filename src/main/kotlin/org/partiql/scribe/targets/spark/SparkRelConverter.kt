package org.partiql.scribe.targets.spark

import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelScan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.ExprQuerySetFactory
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter
import org.partiql.spi.types.PType

public open class SparkRelConverter(transform: SparkPlanToAst, context: ScribeContext, outer: List<Locals> = emptyList()) : RelConverter(
    transform,
    context,
    outer,
) {
    internal companion object {
        val MARKER_EXPLODE = "${org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX}EXPLODE"
        val MARKER_LATERAL = "${org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX}LATERAL"
    }

    override fun visitCorrelate(
        rel: RelCorrelate,
        ctx: Unit,
    ): ExprQuerySetFactory {
        // Unwrap: RHS may be RelFilter(RelScan) or just RelScan
        val rhsScan =
            when (val rhs = rel.right) {
                is RelScan -> rhs
                is RelFilter -> rhs.input as? RelScan
                else -> null
            }

        // If RHS is not a scan (e.g., correlated subquery), fall back to base which produces INNER JOIN
        if (rhsScan == null) {
            return super.visitCorrelate(rel, ctx)
        }

        val rexType = rhsScan.rex.type.pType
        if (rexType.code() != PType.ARRAY && rexType.code() != PType.BAG) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                    message = "Spark does not support correlated join on non-collection type: $rexType",
                ),
            )
        }

        // If the scan expression is a subquery, produce INNER JOIN LATERAL (subquery) ON true
        if (rhsScan.rex is org.partiql.plan.rex.RexSelect || rhsScan.rex is org.partiql.plan.rex.RexSubquery) {
            // Build LHS
            val lhs = visitRelSFW(rel.left, ctx)
            val lhsFrom = assertNotNull(lhs.from)

            // Build RHS subquery expression using correlated scope
            val lhsLocals = Locals(rel.left.type.fields.toList(), outer = outer)
            val rhsLocals = Locals(rel.right.type.fields.toList(), outer = outer + listOf(lhsLocals))
            val rexConverter = transform.getRexConverter(rhsLocals)
            val subqueryExpr = rexConverter.apply(rhsScan.rex)

            // Get alias
            val alias = Identifier.Simple.delimited(rel.right.type.fields[0].name)

            // Create a FromExpr for the subquery, marked with LATERAL prefix
            // We'll use a special function call wrapper "LATERAL" that SparkAstToSql will detect
            val lateralSubquery =
                fromExpr(
                    expr =
                        org.partiql.ast.Ast.exprCall(
                            function = Identifier.regular(MARKER_LATERAL),
                            args = listOf(subqueryExpr),
                            setq = null,
                        ),
                    fromType = FromType.SCAN(),
                    asAlias = alias,
                )

            val lhsTableRef =
                if (lhsFrom.tableRefs.size == 1) {
                    lhsFrom.tableRefs.first()
                } else {
                    lhsFrom.tableRefs.reduce { acc, ref ->
                        org.partiql.ast.Ast.fromJoin(
                            lhs = acc,
                            rhs = ref,
                            joinType = org.partiql.ast.JoinType.INNER(),
                            condition = org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true)),
                        )
                    }
                }

            val joinType =
                when (rel.joinType.code()) {
                    org.partiql.plan.JoinType.INNER -> org.partiql.ast.JoinType.INNER()
                    org.partiql.plan.JoinType.LEFT -> org.partiql.ast.JoinType.LEFT()
                    else -> org.partiql.ast.JoinType.INNER()
                }

            // Extract condition from filter if present
            val condition: org.partiql.ast.expr.Expr =
                if (rel.right is RelFilter) {
                    val filter = rel.right as RelFilter
                    val predicate = filter.predicate
                    val isTrivialTrue =
                        predicate is org.partiql.plan.rex.RexLit &&
                            predicate.datum.type.code() == PType.BOOL && predicate.datum.boolean
                    if (isTrivialTrue) {
                        org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true))
                    } else {
                        val filterRexConverter = transform.getRexConverter(rhsLocals)
                        filterRexConverter.apply(predicate)
                    }
                } else {
                    org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true))
                }

            lhs.from =
                from(
                    tableRefs =
                        listOf(
                            org.partiql.ast.Ast.fromJoin(
                                lhs = lhsTableRef,
                                rhs = lateralSubquery,
                                joinType = joinType,
                                condition = condition,
                            ),
                        ),
                )
            return ExprQuerySetFactory(queryBody = lhs)
        }

        // Build LHS FROM
        val lhs = visitRelSFW(rel.left, ctx)
        val lhsFrom = assertNotNull(lhs.from)

        // Build the EXPLODE expression for the RHS scan
        val lhsLocals = Locals(rel.left.type.fields.toList(), outer = outer)
        val rhsLocals = Locals(rel.right.type.fields.toList(), outer = outer + listOf(lhsLocals))
        val rexConverter = transform.getRexConverter(rhsLocals)
        val arrayExpr = rexConverter.apply(rhsScan.rex)

        // Create EXPLODE(arrayExpr) call
        val explodeCall =
            org.partiql.ast.Ast.exprCall(
                function = Identifier.regular(MARKER_EXPLODE),
                args = listOf(arrayExpr),
                setq = null,
            )

        // Get the alias for the unnested items
        val itemAlias = rel.right.type.fields[0].name

        // Build FROM with LATERAL VIEW EXPLODE
        val lateralViewExpr =
            fromExpr(
                expr = explodeCall,
                fromType = FromType.SCAN(),
                asAlias = Identifier.Simple.delimited("_$itemAlias"),
            )

        lhs.from =
            if (rel.joinType.code() == org.partiql.plan.JoinType.LEFT) {
                // LEFT: use fromJoin so SparkAstToSql can detect and render LATERAL VIEW OUTER
                val lhsTableRef =
                    if (lhsFrom.tableRefs.size == 1) {
                        lhsFrom.tableRefs.first()
                    } else {
                        lhsFrom.tableRefs.reduce { acc, ref ->
                            org.partiql.ast.Ast.fromJoin(
                                lhs = acc,
                                rhs = ref,
                                joinType = org.partiql.ast.JoinType.INNER(),
                                condition = org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true)),
                            )
                        }
                    }
                from(
                    tableRefs =
                        listOf(
                            org.partiql.ast.Ast.fromJoin(
                                lhs = lhsTableRef,
                                rhs = lateralViewExpr,
                                joinType = org.partiql.ast.JoinType.LEFT(),
                                condition = org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true)),
                            ),
                        ),
                )
            } else {
                // INNER: append as separate table ref (rendered with space, no comma)
                from(
                    tableRefs = lhsFrom.tableRefs + listOf(lateralViewExpr),
                )
            }

        // If there's a filter predicate (ON condition), add it as WHERE clause (skip if just TRUE)
        if (rel.right is RelFilter) {
            val filter = rel.right as RelFilter
            val predicate = filter.predicate
            val isTrivialTrue =
                predicate is org.partiql.plan.rex.RexLit &&
                    predicate.datum.type.code() == PType.BOOL && predicate.datum.boolean
            if (!isTrivialTrue) {
                val filterRexConverter = transform.getRexConverter(rhsLocals)
                lhs.where = filterRexConverter.apply(predicate)
            }
        }

        return ExprQuerySetFactory(queryBody = lhs)
    }
}
