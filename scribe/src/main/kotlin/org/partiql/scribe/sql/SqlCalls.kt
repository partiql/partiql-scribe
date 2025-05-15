package org.partiql.scribe.sql

import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprIsType
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprMissingPredicate
import org.partiql.ast.Ast.exprNot
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprRowValue
import org.partiql.ast.Ast.exprSessionAttribute
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprMissingPredicate
import org.partiql.ast.expr.ExprNullPredicate
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.SessionAttribute
import org.partiql.ast.expr.TrimSpec
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.spi.types.PType

/**
 * Transform the call args to the special form.
 */
public typealias SqlCallFn = (SqlArgs) -> Expr

/**
 * List of arguments.
 */
public typealias SqlArgs = List<SqlArg>

/**
 * Pair an [Expr] with its resolved type.
 */
public class SqlArg(
    public val expr: Expr,
    public val type: PType,
)

/**
 * Maps a function name to basic rewrite logic.
 *
 * For target implementors, extend this and leverage the type-annotated function arguments to perform desired rewrite.
 */
public abstract class SqlCalls(context: ScribeContext) {
    private val listener = context.getErrorListener()

    public companion object {
        public fun standard(context: ScribeContext): SqlCalls = object : SqlCalls(context) {}
    }

    /**
     * List of special form rules. See [org.partiql.planner.Header] for the derivations.
     */
    public open val rules: Map<String, SqlCallFn> =
        mapOf(
            "not" to ::notFn,
            "pos" to ::posFn,
            "neg" to ::negFn,
            "eq" to ::eqFn,
            "ne" to ::neFn,
            "and" to ::andFn,
            "or" to ::orFn,
            "lt" to ::ltFn,
            "lte" to ::lteFn,
            "gt" to ::gtFn,
            "gte" to ::gteFn,
            "plus" to ::plusFn,
            "minus" to ::minusFn,
            "times" to ::timesFn,
            "divide" to ::divFn,
            "mod" to ::modFn,
            "modulo" to ::modFn,
            "concat" to ::concatFn,
            "bitwise_and" to ::bitwiseAnd,
            // DATE_ADD
            "date_add_year" to { args -> dateAdd(DatetimeField.YEAR(), args) },
            "date_add_month" to { args -> dateAdd(DatetimeField.MONTH(), args) },
            "date_add_day" to { args -> dateAdd(DatetimeField.DAY(), args) },
            "date_add_hour" to { args -> dateAdd(DatetimeField.HOUR(), args) },
            "date_add_minute" to { args -> dateAdd(DatetimeField.MINUTE(), args) },
            "date_add_second" to { args -> dateAdd(DatetimeField.SECOND(), args) },
            // DATE_DIFF
            "date_diff_year" to { args -> dateDiff(DatetimeField.YEAR(), args) },
            "date_diff_month" to { args -> dateDiff(DatetimeField.MONTH(), args) },
            "date_diff_day" to { args -> dateDiff(DatetimeField.DAY(), args) },
            "date_diff_hour" to { args -> dateDiff(DatetimeField.HOUR(), args) },
            "date_diff_minute" to { args -> dateDiff(DatetimeField.MINUTE(), args) },
            "date_diff_second" to { args -> dateDiff(DatetimeField.SECOND(), args) },
            // LIKE
            "like" to { args -> like(args, escape = false) },
            "like_escape" to { args -> like(args, escape = true) },
            // IS NULL/MISSING
            "is_null" to { args -> exprNullPredicate(args[0].expr, not = false) },
            "is_missing" to { args -> exprMissingPredicate(args[0].expr, not = false) },
            // TODO look at types supported here. we may need to add more or modify
            // IS <type>
            "is_bool" to { args -> exprIsType(args[0].expr, DataType.BOOL(), not = false) },
            "is_int8" to { args -> exprIsType(args[0].expr, DataType.TINYINT(), not = false) },
            "is_int16" to { args -> exprIsType(args[0].expr, DataType.SMALLINT(), not = false) },
            "is_int32" to { args -> exprIsType(args[0].expr, DataType.INT(), not = false) },
            "is_int64" to { args -> exprIsType(args[0].expr, DataType.BIGINT(), not = false) },
            "is_int" to { args -> exprIsType(args[0].expr, DataType.INT(), not = false) },
            "is_decimal" to { args -> exprIsType(args[0].expr, DataType.DECIMAL(), not = false) },
            "is_float32" to { args -> exprIsType(args[0].expr, DataType.FLOAT(), not = false) },
            "is_float64" to { args -> exprIsType(args[0].expr, DataType.DOUBLE_PRECISION(), not = false) },
            "is_char" to { args -> exprIsType(args[0].expr, DataType.CHAR(), not = false) },
            "is_string" to { args -> exprIsType(args[0].expr, DataType.STRING(), not = false) },
            "is_blob" to { args -> exprIsType(args[0].expr, DataType.BLOB(), not = false) },
            "is_clob" to { args -> exprIsType(args[0].expr, DataType.CLOB(), not = false) },
            "is_date" to { args -> exprIsType(args[0].expr, DataType.DATE(), not = false) },
            "is_time" to { args -> exprIsType(args[0].expr, DataType.TIME(), not = false) },
            "is_timeWithTz" to { args -> exprIsType(args[0].expr, DataType.TIME_WITH_TIME_ZONE(), not = false) },
            "is_timestamp" to { args -> exprIsType(args[0].expr, DataType.TIMESTAMP(), not = false) },
            "is_timestampWithTz" to { args -> exprIsType(args[0].expr, DataType.TIMESTAMP_WITH_TIME_ZONE(), not = false) },
            "is_interval" to { args -> exprIsType(args[0].expr, DataType.INTERVAL(), not = false) },
            "is_bag" to { args -> exprIsType(args[0].expr, DataType.BAG(), not = false) },
            "is_list" to { args -> exprIsType(args[0].expr, DataType.LIST(), not = false) },
            "is_struct" to { args -> exprIsType(args[0].expr, DataType.STRUCT(), not = false) },
            // Trim
            "trim" to { args -> trim(args, TrimSpec.BOTH()) },
            "trim_chars" to { args -> trim(args, TrimSpec.BOTH()) },
            "trim_leading" to { args -> trim(args, TrimSpec.LEADING()) },
            "trim_leading_chars" to { args -> trim(args, TrimSpec.LEADING()) },
            "trim_trailing" to { args -> trim(args, TrimSpec.TRAILING()) },
            "trim_trailing_chars" to { args -> trim(args, TrimSpec.TRAILING()) },
            // Session Attributes
            "current_user" to sessionAttribute(SessionAttribute.CURRENT_USER()),
            "current_date" to sessionAttribute(SessionAttribute.CURRENT_DATE()),
            // in collection
            "in_collection" to { args -> inCollection(args) },
            // between
            "between" to { args -> between(args) },
        )

    private fun removeSystemPrefix(name: String): String {
        return name.removePrefix("\uFDEF")
    }

    public fun retarget(
        name: String,
        args: SqlArgs,
    ): Expr {
        val normalizedName = removeSystemPrefix(name)
        val rule = rules[normalizedName]
        return if (rule == null) {
            // use default translations
            default(normalizedName, args)
        } else {
            // use special rule
            rule(args)
        }
    }

    public open fun default(
        name: String,
        args: SqlArgs,
    ): Expr {
        return exprCall(
            function = Identifier.delimited(name),
            args = args.map { it.expr },
        )
    }

    private fun sessionAttribute(attribute: SessionAttribute): SqlCallFn {
        return { _ -> exprSessionAttribute(attribute) }
    }

    public open fun unary(
        op: String,
        args: SqlArgs,
    ): Expr {
        if (args.size != 1) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(ScribeProblem.INVALID_PLAN, "Unary operator '$op' requires exactly 1 argument"),
            )
        }
        return exprOperator(symbol = op, lhs = null, rhs = args[0].expr)
    }

    public open fun binary(
        op: String,
        args: SqlArgs,
    ): Expr {
        if (args.size != 2) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(ScribeProblem.INVALID_PLAN, "Binary operator '$op' requires exactly 2 arguments"),
            )
        }
        return exprOperator(symbol = op, args[0].expr, args[1].expr)
    }

    private fun Boolean?.flip(): Boolean =
        when (this) {
            null -> true
            else -> !this
        }

    /**
     * Push the negation down if possible.
     * For example : NOT 1 is NULL -> 1 is NOT NULL.
     *
     * TODO: could consider removing redundant boolean expressions here. E.g.
     *  - NOT NOT <expr> -> <expr>
     *  - NOT false -> true
     *  - NOT true -> false
     * (this constant-folding step really should have been done at an earlier stage)
     */
    public open fun notFn(args: SqlArgs): Expr {
        // Check to replace NOT (x = y) with x <> y
        val arg = args[0].expr
        return when (arg) {
            is ExprOperator -> {
                if (arg.symbol == "=" && arg.lhs != null) {
                    exprOperator("<>", arg.lhs, arg.rhs)
                } else {
                    unary("NOT", args)
                }
            }
            is ExprBetween -> exprBetween(arg.value, arg.from, arg.to, arg.isNot.flip())
            is ExprInCollection -> {
                val collection =
                    when (val coll = arg.rhs) {
                        is ExprArray -> exprRowValue(coll.values)
                        else -> coll
                    }
                exprInCollection(arg.lhs, collection, arg.isNot.flip())
            }
            is ExprIsType -> exprIsType(arg.value, arg.type, arg.isNot.flip())
            is ExprLike -> exprLike(arg.value, arg.pattern, arg.escape, arg.isNot.flip())
            is ExprNullPredicate -> exprNullPredicate(arg.value, arg.isNot.flip())
            is ExprMissingPredicate -> exprMissingPredicate(arg.value, arg.isNot.flip())
            else -> exprNot(args[0].expr)
        }
    }

    public open fun posFn(args: SqlArgs): Expr = unary("+", args)

    public open fun negFn(args: SqlArgs): Expr = unary("-", args)

    public open fun eqFn(args: SqlArgs): Expr = binary("=", args)

    public open fun neFn(args: SqlArgs): Expr = binary("!=", args)

    public open fun andFn(args: SqlArgs): Expr = binary("AND", args)

    public open fun orFn(args: SqlArgs): Expr = binary("OR", args)

    public open fun ltFn(args: SqlArgs): Expr = binary("<", args)

    public open fun lteFn(args: SqlArgs): Expr = binary("<=", args)

    public open fun gtFn(args: SqlArgs): Expr = binary(">", args)

    public open fun gteFn(args: SqlArgs): Expr = binary(">=", args)

    public open fun plusFn(args: SqlArgs): Expr = binary("+", args)

    public open fun minusFn(args: SqlArgs): Expr = binary("-", args)

    public open fun timesFn(args: SqlArgs): Expr = binary("*", args)

    public open fun divFn(args: SqlArgs): Expr = binary("/", args)

    public open fun modFn(args: SqlArgs): Expr = binary("%", args)

    public open fun concatFn(args: SqlArgs): Expr = binary("||", args)

    public open fun bitwiseAnd(args: SqlArgs): Expr = binary("&", args)

    public open fun dateAdd(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val call = Identifier.regular("DATE_ADD")
        val arg0 = exprLit(Literal.string(part.name().uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    public open fun dateDiff(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val call = Identifier.regular("DATE_DIFF")
        val arg0 = exprLit(Literal.string(part.name().uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    // functions to operator
    public open fun inCollection(args: SqlArgs): Expr {
        val collection =
            when (val arg1 = args[1].expr) {
                is ExprArray -> exprRowValue(arg1.values)
                else -> arg1
            }
        return exprInCollection(args[0].expr, collection, false)
    }

    public open fun between(args: SqlArgs): Expr = exprBetween(args[0].expr, args[1].expr, args[2].expr, false)

    public open fun like(
        args: SqlArgs,
        escape: Boolean,
    ): Expr {
        val arg0 = args[0].expr
        val arg1 = args[1].expr
        val arg2 = if (escape) args[2].expr else null
        return exprLike(arg0, arg1, arg2, false)
    }

    /**
     * SQL TRIM — TRIM( [ BOTH | LEADING | TRAILING ] [<chars> FROM ] <value> )
     */
    public open fun trim(
        args: SqlArgs,
        spec: TrimSpec,
    ): Expr {
        return when (args.size) {
            1 -> {
                val value = args[0].expr
                exprTrim(value, null, spec)
            }
            2 -> {
                val value = args[0].expr
                val chars = args[1].expr
                exprTrim(value, chars, spec)
            }
            else ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(ScribeProblem.INVALID_PLAN, "Unsupported trim(...) with arity ${args.size}"),
                )
        }
    }
}
