package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.DataType
import org.partiql.ast.expr.Expr
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlArg
import org.partiql.spi.types.PType

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
        val args = rex.args.map { SqlArg(wrapInDateTimeCast(it), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitDispatch(
        rex: RexDispatch,
        ctx: Unit,
    ): Expr {
        val fn = rex.functions.first()
        val args = rex.args.map { SqlArg(wrapInDateTimeCast(it), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitNullIf(
        rex: RexNullIf,
        ctx: Unit,
    ): Expr {
        val v1 = wrapInDateTimeCast(rex.v1)
        val v2 = wrapInDateTimeCast(rex.v2)
        return exprNullIf(v1, v2)
    }

    override fun visitCase(
        rex: RexCase,
        ctx: Unit,
    ): Expr {
        val matchExpr = rex.match?.let { wrapInDateTimeCast(it) }
        val default = rex.default?.let { wrapInDateTimeCast(it) }
        val branches =
            rex.branches.map {
                val condition = wrapInDateTimeCast(it.condition)
                val result = wrapInDateTimeCast(it.result)
                exprCaseBranch(condition, result)
            }
        return exprCase(matchExpr, branches, default)
    }

    private fun wrapInDateTimeCast(rex: Rex): Expr {
        val expr = visitRex(rex, Unit)
        val type = rex.type
        if (rex is RexCast && type.pType.code() == PType.DYNAMIC) {
            return wrapInDateTimeCast(rex.operand)
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
