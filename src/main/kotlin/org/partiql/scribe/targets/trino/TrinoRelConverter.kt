package org.partiql.scribe.targets.trino

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

public open class TrinoRelConverter(transform: TrinoPlanToAst, context: ScribeContext, outer: List<Locals> = emptyList()) : RelConverter(
    transform,
    context,
    outer,
) {
    internal companion object {
        val MARKER_UNNEST = "${org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX}UNNEST"
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

        // If RHS is not a scan (e.g., complex plan), fall back to base
        if (rhsScan == null) {
            return super.visitCorrelate(rel, ctx)
        }

        // If the scan expression is a subquery, produce INNER JOIN LATERAL (subquery) ON condition
        if (rhsScan.rex is org.partiql.plan.rex.RexSelect || rhsScan.rex is org.partiql.plan.rex.RexSubquery) {
            // Build LHS
            val lhs = visitRelSFW(rel.left, ctx)
            val lhsFrom = assertNotNull(lhs.from)

            val lhsLocals = Locals(rel.left.type.fields.toList(), outer = outer)
            val rhsLocals = Locals(rel.right.type.fields.toList(), outer = outer + listOf(lhsLocals))
            val rexConverter = transform.getRexConverter(rhsLocals)
            val subqueryExpr = rexConverter.apply(rhsScan.rex)

            val alias = Identifier.Simple.delimited(rel.right.type.fields[0].name)

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
                        transform.getRexConverter(rhsLocals).apply(predicate)
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

        // Path lateral: check type
        val rexType = rhsScan.rex.type.pType
        if (rexType.code() != PType.ARRAY && rexType.code() != PType.BAG) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                    message = "Trino does not support correlated join on non-collection type: $rexType",
                ),
            )
        }

        // Build LHS FROM
        val lhs = visitRelSFW(rel.left, ctx)
        val lhsFrom = assertNotNull(lhs.from)

        // Build the UNNEST expression
        val lhsLocals = Locals(rel.left.type.fields.toList(), outer = outer)
        val rhsLocals = Locals(rel.right.type.fields.toList(), outer = outer + listOf(lhsLocals))
        val rexConverter = transform.getRexConverter(rhsLocals)
        val arrayExpr = rexConverter.apply(rhsScan.rex)

        // Create UNNEST(arrayExpr)
        val unnestCall =
            org.partiql.ast.Ast.exprCall(
                function = Identifier.regular(MARKER_UNNEST),
                args = listOf(arrayExpr),
                setq = null,
            )

        val itemAlias = rel.right.type.fields[0].name

        val unnestExpr =
            fromExpr(
                expr = unnestCall,
                fromType = FromType.SCAN(),
                asAlias = Identifier.Simple.delimited("_$itemAlias"),
            )

        lhs.from =
            if (rel.joinType.code() == org.partiql.plan.JoinType.LEFT) {
                // LEFT JOIN UNNEST(...) AS alias(item) ON true
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
                                rhs = unnestExpr,
                                joinType = org.partiql.ast.JoinType.LEFT(),
                                condition = org.partiql.ast.expr.ExprLit(org.partiql.ast.Literal.bool(true)),
                            ),
                        ),
                )
            } else {
                // CROSS JOIN UNNEST(...)
                from(
                    tableRefs = lhsFrom.tableRefs + listOf(unnestExpr),
                )
            }

        // Add WHERE clause if there's a non-trivial filter
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
