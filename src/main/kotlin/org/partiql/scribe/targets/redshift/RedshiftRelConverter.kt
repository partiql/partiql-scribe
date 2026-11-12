package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.expr.Expr
import org.partiql.plan.WindowFunctionNode
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rex.RexLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.ExprQuerySetFactory
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.utils.isUnknown
import org.partiql.spi.function.Agg
import org.partiql.spi.types.PType

public open class RedshiftRelConverter(
    transform: RedshiftPlanToAst,
    context: ScribeContext,
    outer: List<Locals> = emptyList(),
) : RelConverter(
        transform,
        context,
        outer,
    ) {
    override val selectValueSupported: Boolean = false

    override fun visitCorrelate(
        rel: RelCorrelate,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val rhsScan =
            when (val rhs = rel.right) {
                is RelScan -> rhs
                is RelFilter -> rhs.input as? RelScan
                else -> null
            }

        // Redshift does not support lateral correlated subqueries
        if (rhsScan == null ||
            rhsScan.rex is org.partiql.plan.rex.RexSelect ||
            rhsScan.rex is org.partiql.plan.rex.RexSubquery
        ) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.UNSUPPORTED_OPERATION,
                    message = "Redshift does not support lateral correlated subqueries",
                ),
            )
        }

        // Reject scalar types
        val rexType = rhsScan.rex.type.pType
        if (rexType.code() != PType.ARRAY && rexType.code() != PType.BAG) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                    message = "Redshift does not support correlated join on non-collection type: $rexType",
                ),
            )
        }

        // Redshift SUPER unnest join only supports ON TRUE — reject non-trivial conditions
        if (rel.right is RelFilter) {
            val predicate = (rel.right as RelFilter).predicate
            val isLiteralTrue =
                predicate is RexLit &&
                    predicate.datum.type.code() == PType.BOOL &&
                    !predicate.datum.isUnknown() &&
                    predicate.datum.boolean
            if (!isLiteralTrue) {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_OPERATION,
                        message = "Redshift does not support correlated join with a condition other than ON TRUE for SUPER unnest joins",
                    ),
                )
            }
        }

        // Redshift supports PartiQL-style path navigation natively
        return super.visitCorrelate(rel, ctx)
    }

    /**
     * Redshift aggregate window functions do not support DISTINCT, and with an ORDER BY require a ROWS frame, which
     * cannot express the default frame (through the current row's peers).
     * See https://docs.aws.amazon.com/redshift/latest/dg/c_Window_functions.html.
     */
    override fun checkAggregateWindowFunction(
        windowFunction: WindowFunctionNode,
        aggregate: Agg,
        rel: RelWindow,
    ) {
        val name = aggregate.signature.name.uppercase()
        if (windowFunction.isDistinct) {
            unsupported(
                "Unsupported aggregate window function $name: Redshift does not support DISTINCT in aggregate window " +
                    "functions. Remove DISTINCT or use another target",
            )
        }
        if (rel.collations.isNotEmpty()) {
            unsupported(
                "Unsupported aggregate window function $name: Redshift requires a ROWS frame with ORDER BY, which cannot " +
                    "express the default frame. Remove ORDER BY from the OVER clause or use another target",
            )
        }
    }

    // Redshift does not support the SQL window clause. So we convert window clauses to inline window specifications.
    override fun visitWindow(
        rel: RelWindow,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val sfw = visitRelSFW(rel.input, ctx)
        val locals =
            Locals(
                env = rel.input.type.fields.toList(),
                aggregations = constructAggregationSchema(sfw),
            )

        val rexConverter = transform.getRexConverter(locals)

        val windowFunctionExprs =
            rel.windowFunctions.map { windowFunction ->
                convertWindowFunctionToExpr(windowFunction, rel, rexConverter)
            }

        if (windowFunctionExprs.isNotEmpty()) {
            sfw.windowFunctions =
                sfw.windowFunctions?.apply { addAll(windowFunctionExprs) } ?: windowFunctionExprs.toMutableList()
        }

        return ExprQuerySetFactory(
            queryBody = sfw,
        )
    }

    private fun convertWindowFunctionToExpr(
        windowFunction: WindowFunctionNode,
        rel: RelWindow,
        rexConverter: RexConverter,
    ): Expr {
        val windowType = createWindowFunctionType(windowFunction, rel, rexConverter)
        val windowSpec = createInlineWindowSpecification(rel, rexConverter)
        return exprWindowFunction(windowType, windowSpec)
    }
}
