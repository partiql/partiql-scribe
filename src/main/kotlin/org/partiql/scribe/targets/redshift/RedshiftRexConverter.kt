package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.DataType
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlArg
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import kotlin.math.absoluteValue

/**
 * Redshift-specific [Rex] plan node to [Expr] ast node converter.
 *
 * Major differences include
 * - wrapping [RexCall]s, [RexNullIf], and [RexCase] using datetime paths and variable references with
 * explicit casts
 */
public open class RedshiftRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context) {
    override fun visitCall(
        rex: RexCall,
        ctx: Unit,
    ): Expr {
        val fn = rex.function
        val args = rex.args.map { SqlArg(normalizeArgument(it), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitDispatch(
        rex: RexDispatch,
        ctx: Unit,
    ): Expr {
        val fn = rex.functions.first()
        val args = rex.args.map { SqlArg(normalizeArgument(it), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitNullIf(
        rex: RexNullIf,
        ctx: Unit,
    ): Expr {
        val v1 = normalizeArgument(rex.v1)
        val v2 = normalizeArgument(rex.v2)
        return exprNullIf(v1, v2)
    }

    override fun visitCase(
        rex: RexCase,
        ctx: Unit,
    ): Expr {
        val matchExpr = rex.match?.let { normalizeArgument(it) }
        val default = rex.default?.let { normalizeArgument(it) }
        val branches =
            rex.branches.map {
                val condition = normalizeArgument(it.condition)
                val result = normalizeArgument(it.result)
                exprCaseBranch(condition, result)
            }
        return exprCase(matchExpr, branches, default)
    }

    override fun visitLit(rex: RexLit, ctx: Unit): Expr {
        val type = rex.datum.type
        val datum = rex.datum
        return if (type.code() == PType.INTERVAL_DT) {
            val days = datum.days
            val hours = datum.hours
            val minutes = datum.minutes
            val seconds = datum.seconds
            val nanos = datum.nanos
            val dataType =
                type.toDataType() ?: listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.INVALID_PLAN,
                        message = "Cannot convert $type to a DataType",
                    ),
                )
            val literal = when (type.intervalCode) {
                // For daytime intervals, there is a space in the string literal between DAY and TIME.
                // Redshift requires minus sign for each part of the string literal.
                // E.g., INTERVAL '-10 3' DAY To HOUR is evaluated `minus 10 days and 3 hours` in PartiQL,
                // but is evaluated as `minus 9 days and 21 hours` in the Redshift.
                // So Redshift we transcribe to INTERVAL '-10 -3' DAY To HOUR instead.
                IntervalCode.DAY_HOUR -> {
                    Literal.typedString(
                        dataType,
                        "$days ${hours}",
                    )
                }
                IntervalCode.DAY_MINUTE -> {
                    Literal.typedString(
                        dataType,
                        "$days ${hours}:${minutes.absoluteValue}",
                    )
                }
                IntervalCode.DAY_SECOND -> {
                    val fracPrecision =
                        if (type.unspecifiedFractionalPrecision()) {
                            null
                        } else {
                            type.fractionalPrecision
                        }
                    val intervalValue =
                        if (fracPrecision != null && fracPrecision > 0) {
                            val nanosTruncated = nanos.absoluteValue.toString().substring(0, fracPrecision)
                            "$days ${hours}:${minutes.absoluteValue}:${seconds.absoluteValue}.$nanosTruncated"
                        } else {
                            "$days ${hours}:${minutes.absoluteValue}:${seconds.absoluteValue}"
                        }
                    Literal.typedString(
                        dataType,
                        intervalValue,
                    )
                }
                else ->
                    return super.visitLit(rex, ctx)
            }
            exprLit(literal)
        } else {
            super.visitLit(rex, ctx)
        }
    }

    /**
     * Wrap the [rex] in an explicit cast if the [rex] is a date time type.
     */
    private fun normalizeArgument(rex: Rex): Expr {
        val expr = visitRex(rex, Unit)
        val type = rex.type
        if (rex is RexCast && type.pType.code() == PType.DYNAMIC) {
            return normalizeArgument(rex.operand)
        }
        return when (rex) {
            is RexPathKey, is RexPathIndex, is RexPathSymbol, is RexVar -> {
                return when (type.pType.code()) {
                    PType.DATE -> exprCast(expr, DataType.DATE())
                    PType.TIME -> exprCast(expr, DataType.TIME())
                    PType.TIMEZ -> exprCast(expr, DataType.TIME_WITH_TIME_ZONE())
                    PType.TIMESTAMP -> exprCast(expr, DataType.TIMESTAMP())
                    PType.TIMESTAMPZ -> exprCast(expr, DataType.TIMESTAMP_WITH_TIME_ZONE())
                    else -> expr
                }
            }
            else -> expr
        }
    }
}
