package org.partiql.scribe.targets.redshift

import org.partiql.ast.Expr
import org.partiql.ast.exprCase
import org.partiql.ast.exprCaseBranch
import org.partiql.ast.exprCast
import org.partiql.ast.exprNullIf
import org.partiql.ast.typeDate
import org.partiql.ast.typeTime
import org.partiql.ast.typeTimestamp
import org.partiql.plan.Rex
import org.partiql.scribe.asNonAbsent
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlTransform
import org.partiql.types.StaticType

/**
 * Redshift-specific [Rex] plan node to [Expr] ast node converter.
 *
 * Major differences include
 * - wrapping [Rex.Op.Call]s, [Rex.Op.Nullif], and [Rex.Op.Case] using datetime paths and variable references with
 * explicit casts
 */
public open class RedshiftRexConverter(
    private val transform: SqlTransform,
    private val locals: Locals
): RexConverter(transform, locals) {
    override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType): Expr {
        val (name, args) = when (node) {
            is Rex.Op.Call.Static -> {
                val name = node.fn.signature.name
                val args = node.args.map { SqlArg(wrapInDatetimeCast(it), it.type) }
                name to args
            }
            is Rex.Op.Call.Dynamic -> {
                val name = node.candidates.first().fn.signature.name
                val args = node.args.map { SqlArg(wrapInDatetimeCast(it), it.type) }
                name to args
            }
        }
        return transform.getFunction(name, args)
    }

    override fun visitRexOpNullif(node: Rex.Op.Nullif, ctx: StaticType): Expr {
        val v1 = wrapInDatetimeCast(node.value)
        val v2 = wrapInDatetimeCast(node.nullifier)
        return exprNullIf(v1, v2)
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType): Expr {
        val default = wrapInDatetimeCast(node.default)
        val branches = node.branches.map {
            val condition = wrapInDatetimeCast(it.condition)
            val result = wrapInDatetimeCast(it.rex)
            exprCaseBranch(condition, result)
        }
        return when (branches.isEmpty()) {
            true -> default
            false -> exprCase(expr = null, branches = branches, default = default)
        }
    }

    private fun wrapInDatetimeCast(rex: Rex): Expr {
        val op = rex.op
        val expr = visitRex(rex, StaticType.ANY)
        val type = rex.type.asNonAbsent()
        return if (op is Rex.Op.Path || op is Rex.Op.Var) {
            return when (type) {
                StaticType.DATE -> exprCast(expr, typeDate())
                StaticType.TIME -> exprCast(expr, typeTime(null))
                StaticType.TIMESTAMP -> exprCast(expr, typeTimestamp(null))
                else -> expr
            }
        } else {
            expr
        }
    }
}
