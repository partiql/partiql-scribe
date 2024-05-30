package org.partiql.scribe.targets.trino

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprCall
import org.partiql.ast.exprCast
import org.partiql.ast.exprCollection
import org.partiql.ast.exprInCollection
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.identifierSymbol
import org.partiql.ast.typeCustom
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
public open class TrinoCalls(private val log: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        this.remove("bitwise_and")
        this["cast_row"] = ::castrow
        this["transform"] = ::transform
    }

    override fun eqFn(args: SqlArgs): Expr {
        val t0 = args[0].type
        val t1 = args[1].type
        if (!TrinoTypes.comparable(t0, t1)) {
            log.error("Types $t0 and $t1 are not comparable in trino")
        }
        return super.eqFn(args)
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

    // Trino gives names to ROW fields by a call to `CAST`. See docs: https://trino.io/docs/current/language/types.html?highlight=row#row.
    // Here, we model this ROW cast as a custom type cast with the row field names encoded in the custom type string.
    // Note this `CAST(ROW(...))` call is only performed when the `ROW` has one field.
    //
    // CAST(ROW(<value>) AS <custom type with ROW field names>)
    private fun castrow(args: SqlArgs): Expr {
        val castValue = args.first().expr
        val rowCall = exprCall(
            id("ROW"), listOf(castValue)
        )
        val asType = ((((args.last().expr) as Expr.Lit).value) as StringValue).value!!
        val customType = typeCustom(name = asType)
        return exprCast(rowCall, customType)
    }

    // transform(<array>, <func>) where func transforms each array element w/ syntax elem -> <result value>
    // docs: https://trino.io/docs/current/functions/array.html#transform
    // This function is used for transpilation of `EXCLUDE` collection wildcards. It is similar to a functional map but
    // uses some special syntax (same as Spark's `transform` function).
    // e.g. SELECT transform(array(1, 2, 3), x -> x + 1) outputs [2, 3, 4]
    // encode as `transform(<arrayExpr>, <elementVar>, <elementExpr>)`
    // which gets translated to `transform(<arrayExpr>, <elementVar> -> <elementExpr>)` in RexConverter
    private fun transform(sqlArgs: List<SqlArg>): Expr {
        val fnName = id("transform")
        val arrayExpr = sqlArgs[0].expr
        val elementVar = sqlArgs[1].expr
        val elementExpr = sqlArgs[2].expr
        return exprCall(fnName, listOf(arrayExpr, elementVar, elementExpr))
    }

    /**
     * SQL IN predicate is defined as `IN (...)`.
     */
    override fun inCollection(args: List<SqlArg>): Expr {
        val lhs = args[0].expr
        var rhs = args[1].expr
        rhs = when (rhs) {
            is Expr.Collection -> exprCollection(Expr.Collection.Type.LIST, rhs.values)
            is Expr.SFW -> rhs
            else -> error("IN predicate expected expression list or subquery, found ${rhs::class.qualifiedName}")
        }
        return exprInCollection(lhs, rhs, false)
    }

    private fun id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
