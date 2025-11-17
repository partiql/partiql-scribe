package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.expr.Expr
import org.partiql.plan.WindowFunctionNode
import org.partiql.plan.rel.RelWindow
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.ExprQuerySetFactory
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter
import org.partiql.scribe.sql.RexConverter

public open class RedshiftRelConverter(transform: RedshiftPlanToAst, context: ScribeContext) : RelConverter(transform, context) {
    // Redshift does not support the SQL window clause. So we convert window clauses to inline window specifications.
    override fun visitWindow(
        rel: RelWindow,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val sfw = visitRelSFW(rel.input, ctx)
        val rexConverter = transform.getRexConverter(Locals(rel.input.type.fields.toList()))

        // Convert window functions to AST expressions
        val windowFunctionExprs =
            rel.windowFunctions.map { windowFunction ->
                convertWindowFunctionToExpr(windowFunction, rel, rexConverter)
            }

        // Store window functions for projection reference
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
        val windowType = createWindowFunctionType(windowFunction, rexConverter)

        // Always create inline window specification for Redshift, window clause is not supported.
        val windowSpec = createInlineWindowSpecification(rel, rexConverter)
        return exprWindowFunction(windowType, windowSpec)
    }
}
