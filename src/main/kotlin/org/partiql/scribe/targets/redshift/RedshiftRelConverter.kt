package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.windowPartition
import org.partiql.ast.Ast.windowSpecification
import org.partiql.ast.expr.Expr
import org.partiql.plan.WindowFunctionNode
import org.partiql.plan.rel.RelWindow
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.ExprQuerySetFactory
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter
import org.partiql.scribe.sql.RexConverter

public open class RedshiftRelConverter(transform: RedshiftPlanToAst, context: ScribeContext) : RelConverter(transform, context)
{
    // Redshift does not support window clause. So we convert
    override fun visitWindow(rel: RelWindow, ctx: Unit): ExprQuerySetFactory {
        val sfw = visitRelSFW(rel.input, ctx)
        val rexConverter = transform.getRexConverter(Locals(rel.input.type.fields.toList()))

        // Convert window functions to AST expressions
        val windowFunctionExprs =
            rel.windowFunctions.map { windowFunction ->
                convertWindowFunctionToExpr(windowFunction, rel, rexConverter)
            }

        // Store window functions for projection reference
        val combined = (sfw.windowFunctions ?: listOf()) + windowFunctionExprs
        sfw.windowFunctions = combined

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

        // Always create inline window specification for Redshift
        val partitionClause =
            if (rel.partitions.isNotEmpty()) {
                rel.partitions.map { partition ->
                    windowPartition(rexConverter.apply(partition))
                }
            } else {
                emptyList()
            }

        val orderClause =
            if (rel.collations.isNotEmpty()) {
                val sorts =
                    rel.collations.map { collation ->
                        val orderByField = rexConverter.apply(collation.column)
                        val order = convertCollationOrder(collation.order)
                        val nullOrder = convertCollationNulls(collation.nulls)
                        sort(orderByField, order, nullOrder)
                    }
                orderBy(sorts)
            } else {
                null
            }

        val windowSpec = windowSpecification(
            existingName = null,
            partitionClause = partitionClause,
            orderByClause = orderClause,
        )

        return exprWindowFunction(windowType, windowSpec)
    }
}
