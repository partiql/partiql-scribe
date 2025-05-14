package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.utils.unquotedStringExpr

public class TrinoCalls(context: ScribeContext) : SqlCalls(context) {
    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        this.remove("bitwise_and")
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_add
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        val call = Identifier.regular("date_add")
//        log.info("arg0 of date_add went from type `symbol` to `string`")
        val arg0 = exprLit(Literal.string(part.name().lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_diff
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        val call = Identifier.regular("date_diff")
//        log.info("arg0 of date_diff went from type `symbol` to `string`")
        val arg0 = exprLit(Literal.string(part.name().lowercase()))
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
        val call = Identifier.regular("at_timezone")
//        log.info("PartiQL `utcnow()` was replaced by Trino `at_timezone(current_timestamp, 'UTC')`")
        val arg0 = unquotedStringExpr("current_timestamp")
        val arg1 = exprLit(Literal.string("UTC"))
        return exprCall(call, listOf(arg0, arg1))
    }
}
