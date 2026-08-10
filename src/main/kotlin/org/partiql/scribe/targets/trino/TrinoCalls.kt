package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
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
import java.math.BigDecimal

public open class TrinoCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            this.remove("bitwise_and")
            this["cast_row"] = ::castrow
            this["transform"] = ::transform
            this["map_contains_key"] = ::mapContainsKey
            this["map_get"] = ::mapGet
            this["size"] = ::sizeFn
            this["cardinality"] = ::cardinalityFn
            this["exists"] = ::existsFn
        }

    /**
     * Trino does not support the SQL `SUBSTRING(<value> FROM <start> FOR <length>)` special form. Instead it uses the
     * comma-argument function form `substring(<value>, <start>[, <length>])` (1-based).
     *
     * Trino diverges from PartiQL on start/length values. We reject literal `start < 1` and literal `length < 0`
     * during transpilation. For any non-literal expression, we preserve Trino's native `substring` call and report
     * the potential semantic mismatch instead of rewriting it.
     *
     * https://trino.io/docs/current/functions/string.html#substring
     */
    override fun substring(args: SqlArgs): Expr {
        val value = args[0].expr
        val start = args[1].expr
        val length = args.getOrNull(2)?.expr
        val id = Identifier.regular("substring")
        rejectLiteralStartLessThanOne(
            target = "Trino",
            start = start,
        )
        length?.let { lengthArg ->
            rejectNegativeLiteralLength(
                target = "Trino",
                length = lengthArg,
            )
        }
        return when (args.size) {
            2 -> {
                listener.report(
                    ScribeProblem.simpleInfo(
                        code = ScribeProblem.TRANSLATION_INFO,
                        message =
                            "PartiQL `SUBSTRING(<value> FROM <start>)` was replaced by Trino " +
                                "`substring(<value>, <start>)` because Trino does not support the FROM ... FOR syntax. " +
                                "Scribe rejects literal starts less than 1, but non-literal start expressions are " +
                                "passed through unchanged, so Trino may still diverge from PartiQL if `<start>` " +
                                "evaluates less than 1 at runtime.",
                    ),
                )
                exprCall(id, listOf(value, start))
            }
            else -> {
                val lengthArg = checkNotNull(length)
                listener.report(
                    ScribeProblem.simpleInfo(
                        code = ScribeProblem.TRANSLATION_INFO,
                        message =
                            "PartiQL `SUBSTRING(<value> FROM <start> FOR <length>)` was replaced by Trino " +
                                "`substring(<value>, <start>, <length>)` because Trino does not support the FROM ... FOR syntax. " +
                                "Scribe rejects literal starts less than 1 and negative literal lengths, but " +
                                "non-literal start/length expressions are passed through unchanged, so Trino may " +
                                "still diverge from PartiQL if `<start>` evaluates less than 1 or `<length>` " +
                                "evaluates negative at runtime.",
                    ),
                )
                exprCall(id, listOf(value, start, lengthArg))
            }
        }
    }

    private fun rejectLiteralStartLessThanOne(
        target: String,
        start: Expr,
    ) {
        if (start is ExprLit && start.lit.code() == Literal.INT_NUM && start.lit.bigDecimalValue() < BigDecimal.ONE) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "$target substring with a literal start less than 1 is unsupported. " +
                        "Scribe does not rewrite target-specific substring semantics for start < 1.",
                ),
            )
        }
    }

    private fun rejectNegativeLiteralLength(
        target: String,
        length: Expr,
    ) {
        if (length is ExprLit && length.lit.code() == Literal.INT_NUM && length.lit.bigDecimalValue() < BigDecimal.ZERO) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "$target substring with a negative literal length is unsupported. " +
                        "Scribe does not rewrite target-specific negative substring semantics.",
                ),
            )
        }
    }

    /**
     * Trino does not have `char_length`; use `length` instead.
     *
     * https://trino.io/docs/current/functions/string.html#length
     */
    override fun charLength(args: SqlArgs): Expr {
        val id = Identifier.regular("length")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `char_length` was replaced by Trino `length`",
            ),
        )
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * Trino rejects an empty string delimiter, while PartiQL returns the original string as a single-element list.
     * Scribe cannot know whether a non-literal delimiter expression evaluates to `''` at runtime, so it always
     * reports this edge case.
     */
    override fun split(args: SqlArgs): Expr {
        val string = args[0].expr
        val delimiter = args[1].expr
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message =
                    "PartiQL `split(<string>, <delimiter>)` was kept as Trino `split(string, delimiter)`. " +
                        "If the delimiter is `''` or evaluates to `''` at runtime, PartiQL returns the original " +
                        "string as a single-element list, but Trino fails with `INVALID_FUNCTION_ARGUMENT`.",
            ),
        )
        return exprCall(Identifier.regular("SPLIT"), listOf(string, delimiter))
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

    /**
     * PartiQL `map_contains_key(map, key)` -> Trino `contains(map_keys(map), key)`
     */
    private fun mapContainsKey(args: SqlArgs): Expr {
        val containsId = Identifier.regular("contains")
        val mapKeysId = Identifier.regular("map_keys")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_contains_key` was replaced by Trino `contains(map_keys(...), ...)`",
            ),
        )
        val mapExpr = args[0].expr
        val keyExpr = args[1].expr
        val mapKeysCall = exprCall(mapKeysId, listOf(mapExpr))
        return exprCall(containsId, listOf(mapKeysCall, keyExpr))
    }

    /**
     * PartiQL `map_get(map, key)` -> Trino `element_at(map, key)`
     */
    private fun mapGet(args: SqlArgs): Expr {
        val id = Identifier.regular("element_at")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_get` was replaced by Trino `element_at`",
            ),
        )
        return exprCall(id, listOf(args[0].expr, args[1].expr))
    }

    /**
     * PartiQL `size(collection)` -> Trino `cardinality(collection)`
     */
    private fun sizeFn(args: SqlArgs): Expr {
        val id = Identifier.regular("cardinality")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `size` was replaced by Trino `cardinality`",
            ),
        )
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * PartiQL `cardinality(collection)` -> Trino `cardinality(collection)` (native)
     */
    private fun cardinalityFn(args: SqlArgs): Expr {
        val id = Identifier.regular("cardinality")
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * PartiQL `exists(collection)` -> Trino `cardinality(collection) > 0`
     */
    private fun existsFn(args: SqlArgs): Expr {
        val cardId = Identifier.regular("cardinality")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `exists` was replaced by Trino `cardinality(...) > 0`",
            ),
        )
        val cardCall = exprCall(cardId, listOf(args[0].expr))
        return exprOperator(">", cardCall, exprLit(Literal.intNum(0)))
    }

    override fun overlaps(args: SqlArgs): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Trino does not support OVERLAPS predicate",
            ),
        )
    }
}
