package org.partiql.scribe.targets.trino

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprCall
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.identifierSymbol
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.types.BoolType
import org.partiql.types.IntType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
public class TrinoCalls(private val log: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        this.remove("bitwise_and")
    }

    override fun eqFn(args: SqlArgs): Expr {
        val t0 = args[0].type
        val t1 = args[1].type
        if (!typesAreComparable(t0, t1)) {
            log.error("Types $t0 and $t1 are not comparable in trino")
        }
        return super.eqFn(args)
    }

    private fun typesAreComparable(t0: StaticType, t1: StaticType): Boolean {
        if (t0 == t1 || t0.toString() == t1.toString()) {
            return true
        }
        if (t0 is BoolType && t1 is BoolType) {
            return true
        }
        if (t0 is IntType && t1 is IntType) {
            return true
        }
        return false
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_add
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        val call = identifierSymbol("date_add", Identifier.CaseSensitivity.INSENSITIVE)
        log.info("arg0 of date_add went from type `symbol` to `string`")
        val arg0 = exprLit(stringValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_diff
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        val call = identifierSymbol("date_diff", Identifier.CaseSensitivity.INSENSITIVE)
        log.info("arg0 of date_diff went from type `symbol` to `string`")
        val arg0 = exprLit(stringValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#current_timestamp
     * https://trino.io/docs/current/functions/datetime.html#at_timezone
     *
     * at_timezone(current_timestamp, 'UTC')
     */
    private fun utcnow(args: SqlArgs): Expr {
        val call = id("at_timezone")
        log.info("PartiQL `utcnow()` was replaced by Trino `at_timezone(current_timestamp, 'UTC')`")
        val arg0 = exprVar(id("current_timestamp"), Expr.Var.Scope.DEFAULT)
        val arg1 = exprLit(stringValue("UTC"))
        return exprCall(call, listOf(arg0, arg1))
    }

    private fun id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
