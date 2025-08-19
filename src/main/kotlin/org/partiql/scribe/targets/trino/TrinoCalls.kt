package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.utils.unquotedStringExpr
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType

public open class TrinoCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            this.remove("bitwise_and")
            this["cast_row"] = ::castrow
            this["transform"] = ::transform
        }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_add
     */
    override fun dateAdd(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val call = Identifier.regular("date_add")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL's `date_add` has been modified for translation to Trino. Converted first argument " +
                    "of `date_add` from an unquoted keyword to a string literal (${part.name()} -> " +
                    "'${part.name().lowercase()}').",
            ),
        )
        val arg0 = exprLit(Literal.string(part.name().lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_diff
     */
    override fun dateDiff(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val call = Identifier.regular("date_diff")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL's `date_diff` has been modified for translation to Trino. Converted first argument " +
                    "of `date_add` from an unquoted keyword to a string literal (${part.name()} -> " +
                    "'${part.name().lowercase()}').",
            ),
        )
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
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL `utcnow()` was replaced by Trino `at_timezone(current_timestamp, 'UTC')`",
            ),
        )
        val arg0 = unquotedStringExpr("current_timestamp")
        val arg1 = exprLit(Literal.string("UTC"))
        return exprCall(call, listOf(arg0, arg1))
    }

    // Trino gives names to ROW fields by a call to `CAST`. See docs: https://trino.io/docs/current/language/types.html?highlight=row#row.
    // Here, we model this ROW cast as a custom type cast with the row field names encoded in the custom type string.
    //
    // CAST(ROW(<values list>) AS <custom type with ROW field names>)
    private fun castrow(args: SqlArgs): Expr {
        val castValue = args.first().expr as ExprArray
        val rowCall =
            exprCall(
                Identifier.regular("ROW"),
                castValue.values,
            )
        val asType = ((args.last().expr as ExprLit).lit).stringValue()
        val customType = DataType.USER_DEFINED(Identifier.regular(asType))
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
        val fnName = Identifier.regular("transform")
        val arrayExpr = sqlArgs[0].expr
        val elementVar = sqlArgs[1].expr
        val elementExpr = sqlArgs[2].expr
        return exprCall(fnName, listOf(arrayExpr, elementVar, elementExpr))
    }

    /**
     * Returns true if and only if [type] is a day-time interval that contains any time fields.
     */
    private fun isIntervalTime(type: PType): Boolean {
        if (type.code() != PType.INTERVAL_DT) {
            return false
        }
        return type.intervalCode != IntervalCode.DAY
    }

    override fun plusFn(args: SqlArgs): Expr {
        val lhsType = args[0].type
        val rhsType = args[1].type
        if (lhsType.code() == PType.DATE && isIntervalTime(rhsType)) {
            listener.report(
                ScribeProblem.simpleInfo(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Trino does not support arithmetic between dates and intervals with time fields.",
                ),
            )
        } else if (isIntervalTime(lhsType) && rhsType.code() == PType.DATE) {
            listener.report(
                ScribeProblem.simpleInfo(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Trino does not support arithmetic between dates and intervals with time fields.",
                ),
            )
        }
        return super.plusFn(args)
    }

    override fun minusFn(args: SqlArgs): Expr {
        val lhsType = args[0].type
        val rhsType = args[1].type
        if (lhsType.code() == PType.DATE && isIntervalTime(rhsType)) {
            listener.report(
                ScribeProblem.simpleInfo(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Trino does not support arithmetic between dates and intervals with time fields.",
                ),
            )
        } else if (isIntervalTime(lhsType) && rhsType.code() == PType.DATE) {
            listener.report(
                ScribeProblem.simpleInfo(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Trino does not support arithmetic between dates and intervals with time fields.",
                ),
            )
        }
        return super.minusFn(args)
    }
}
