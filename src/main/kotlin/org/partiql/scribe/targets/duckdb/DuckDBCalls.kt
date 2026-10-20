package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Ast.exprRowValue
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.existsSubquery
import org.partiql.scribe.sql.inCollectionArrayOperands
import org.partiql.scribe.sql.integerOperands
import org.partiql.scribe.sql.utils.unquotedStringExpr
import org.partiql.spi.types.PType
import java.math.BigDecimal

public open class DuckDBCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            this["map_contains_key"] = ::mapContainsKey
            this["map_get"] = ::mapGet
            this["size"] = ::sizeFn
            this["cardinality"] = ::cardinalityFn
            this["exists"] = ::existsFn
        }

    /**
     * PartiQL `map_contains_key(map, key)` -> DuckDB `map_contains(map, key)`
     */
    private fun mapContainsKey(args: SqlArgs): Expr {
        val mapContainsId = Identifier.regular("map_contains")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_contains_key` was replaced by DuckDB `map_contains(map, key)`.",
            ),
        )
        val mapExpr = args[0].expr
        val keyExpr = args[1].expr
        return exprCall(mapContainsId, listOf(mapExpr, keyExpr))
    }

    /**
     * PartiQL `map_get(map, key)` -> DuckDB `map_extract_value(map, key)`
     *
     * DuckDB's `element_at` / `map_extract` return a LIST (`[value]`, or `[]` for a missing key); `map_extract_value`
     * returns the value itself (NULL for a missing key), matching PartiQL `map_get`.
     */
    private fun mapGet(args: SqlArgs): Expr {
        val id = Identifier.regular("map_extract_value")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_get` was replaced by DuckDB `map_extract_value`",
            ),
        )
        return exprCall(id, listOf(args[0].expr, args[1].expr))
    }

    /**
     * PartiQL `size(collection)` -> DuckDB `len(list)` / `cardinality(map)`
     */
    private fun sizeFn(args: SqlArgs): Expr {
        val call = collectionSize(args[0])
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `size` was replaced by DuckDB `${call.function.identifier.text}`",
            ),
        )
        return call
    }

    /**
     * PartiQL `cardinality(collection)` -> DuckDB `len(list)` / `cardinality(map)`
     */
    private fun cardinalityFn(args: SqlArgs): Expr = collectionSize(args[0])

    /**
     * PartiQL `exists(collection)` -> DuckDB `len(list) > 0` / `cardinality(map) > 0`
     */
    private fun existsFn(args: SqlArgs): Expr {
        existsSubquery(args)?.let { return it }
        val call = collectionSize(args[0])
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `exists` was replaced by DuckDB `${call.function.identifier.text}(...) > 0`",
            ),
        )
        return exprOperator(">", call, exprLit(Literal.intNum(0)))
    }

    /**
     * DuckDB's `cardinality` only accepts a MAP ("Cardinality can only operate on MAPs"), so a list (PartiQL
     * array/bag) uses `len` instead.
     */
    private fun collectionSize(arg: SqlArg): ExprCall {
        val name =
            when (arg.type.code()) {
                PType.ARRAY, PType.BAG -> "len"
                else -> "cardinality"
            }
        return exprCall(Identifier.regular(name), listOf(arg.expr)) as ExprCall
    }

    /**
     * DuckDB does not support the SQL `SUBSTRING(<value> FROM <start> FOR <length>)` special form. Instead it uses the
     * comma-argument function form `substring(<value>, <start>[, <length>])` (1-based).
     *
     * DuckDB diverges from PartiQL on start/length values. In particular, DuckDB accepts start values less than 1 with
     * semantics that differ from PartiQL's SQL-standard "start before the beginning" behavior. We reject literal
     * `start < 1` and literal `length < 0` during transpilation. For any non-literal expression, we preserve DuckDB's
     * native `substring` call and report the potential semantic mismatch instead of rewriting it.
     *
     * https://duckdb.org/docs/current/functions/string.html#substring
     */
    override fun substring(args: SqlArgs): Expr {
        val value = args[0].expr
        val start = args[1].expr
        val length = args.getOrNull(2)?.expr
        val id = Identifier.regular("SUBSTRING")
        checkSubstringArgStart(start)
        length?.let { lengthArg ->
            checkSubstringArgLength(lengthArg)
        }
        return when (args.size) {
            2 -> {
                listener.report(
                    ScribeProblem.simpleInfo(
                        code = ScribeProblem.TRANSLATION_INFO,
                        message =
                            "PartiQL `SUBSTRING(<value> FROM <start>)` was replaced by DuckDB " +
                                "`substring(<value>, <start>)` because DuckDB does not support the FROM ... FOR syntax. " +
                                "Scribe rejects literal starts less than 1 because DuckDB supports them with " +
                                "semantics that differ from PartiQL. Non-literal start expressions are passed " +
                                "through unchanged, so DuckDB may still diverge from PartiQL if `<start>` evaluates " +
                                "less than 1 at runtime.",
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
                            "PartiQL `SUBSTRING(<value> FROM <start> FOR <length>)` was replaced by DuckDB " +
                                "`substring(<value>, <start>, <length>)` because DuckDB does not support the FROM ... FOR syntax. " +
                                "Scribe rejects literal starts less than 1 because DuckDB supports them with " +
                                "semantics that differ from PartiQL, and it rejects negative literal lengths. " +
                                "Non-literal start/length expressions are passed through unchanged, so DuckDB may " +
                                "still diverge from PartiQL if `<start>` evaluates less than 1 or `<length>` " +
                                "evaluates negative at runtime.",
                    ),
                )
                exprCall(id, listOf(value, start, lengthArg))
            }
        }
    }

    private fun checkSubstringArgStart(start: Expr) {
        if (start is ExprLit && start.lit.code() == Literal.INT_NUM && start.lit.bigDecimalValue() < BigDecimal.ONE) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start " +
                        "values less than 1 with different semantics. Non-literal start/length expressions are " +
                        "passed through unchanged.",
                ),
            )
        }
    }

    private fun checkSubstringArgLength(length: Expr) {
        if (length is ExprLit && length.lit.code() == Literal.INT_NUM && length.lit.bigDecimalValue() < BigDecimal.ZERO) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "DuckDB substring with a negative literal length is unsupported. " +
                        "Scribe does not rewrite target-specific negative substring semantics.",
                ),
            )
        }
    }

    /**
     * PartiQL's `DATE_ADD(<part>, <amount>, <datetime>)` has no faithful DuckDB equivalent: DuckDB's
     * `date_add(date, interval)` takes two arguments, and the `<amount>` may be a dynamic expression that
     * cannot be rendered as an INTERVAL literal. Rather than emit SQL that fails (or behaves differently) at
     * runtime, reject the call at transpile time.
     *
     * https://duckdb.org/docs/current/functions/datetime.html#date_add
     */
    override fun dateAdd(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "DuckDB does not support PartiQL's `DATE_ADD(<part>, <amount>, <datetime>)`. DuckDB's " +
                    "`date_add` takes a (date, interval) pair, which cannot faithfully represent a dynamic " +
                    "amount expression.",
            ),
        )
    }

    /**
     * PartiQL's `DATE_DIFF(<part>, <start>, <end>)` counts complete units (e.g. 2020-12-31 -> 2021-01-01 is 0 years).
     * DuckDB's `date_diff` instead counts part boundaries crossed (1 year), so this translates to DuckDB's
     * `date_sub('<part>', <start>, <end>)`, which counts complete units. `date_sub` has no overload for TIME WITH TIME
     * ZONE operands, and only time parts on TIME, so those are rejected at transpile time instead of emitting SQL that
     * fails at runtime.
     *
     * https://duckdb.org/docs/current/functions/datetime.html#date_subpart-startdate-enddate
     */
    override fun dateDiff(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val lhsCode = args[0].type.code()
        val rhsCode = args[1].type.code()
        // DuckDB's `date_sub` has no overload for TIME WITH TIME ZONE operands.
        if (lhsCode == PType.TIMEZ || rhsCode == PType.TIMEZ) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "DuckDB's `date_sub` has no overload for TIME WITH TIME ZONE operands.",
                ),
            )
        }
        // For plain TIME, DuckDB's `date_sub` only supports time-granularity parts (HOUR/MINUTE/SECOND);
        // date parts such as DAY/MONTH/YEAR error with `"time" units "<part>" not recognized`.
        val timeParts = setOf(DatetimeField.HOUR, DatetimeField.MINUTE, DatetimeField.SECOND)
        if ((lhsCode == PType.TIME || rhsCode == PType.TIME) && part.code() !in timeParts) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "DuckDB's `date_sub` on TIME supports only HOUR, MINUTE, and SECOND parts; " +
                        "${part.name()} is not supported.",
                ),
            )
        }
        val call = Identifier.regular("date_sub")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL's `date_diff` was replaced by DuckDB `date_sub` (which counts complete units). Converted the " +
                    "first argument from an unquoted keyword to a string literal (${part.name()} -> " +
                    "'${part.name().lowercase()}').",
            ),
        )
        val arg0 = exprLit(Literal.string(part.name().lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    override fun minusFn(args: SqlArgs): Expr {
        val lhsType = args[0].type
        val rhsType = args[1].type
        // DuckDB has no `-` operator for the TIME type family (TIME, TIME WITH TIME ZONE); DATE/TIMESTAMP
        // subtraction is fine. Reject at transpile time instead of emitting SQL that fails at runtime.
        val timeFamily = setOf(PType.TIME, PType.TIMEZ)
        if (lhsType.code() in timeFamily && rhsType.code() in timeFamily) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "DuckDB has no `-` (subtraction) operator for TIME / TIME WITH TIME ZONE operands.",
                ),
            )
        }
        // Note: DuckDB supports `DATE - <interval>` (including intervals with time fields); it promotes the
        // result to TIMESTAMP, so no special handling is needed here.
        return super.minusFn(args)
    }

    /**
     * PartiQL integer division truncates toward zero and returns an integer. DuckDB's `/` is always floating point
     * (`3 / 2 = 1.5`), so integer operands use `//` (`3 // 2 = 1`, `-3 // 2 = -1`).
     */
    override fun divFn(args: SqlArgs): Expr = if (integerOperands(args)) binary("//", args) else super.divFn(args)

    /**
     * PartiQL `IN` follows SQL three-valued logic: no match plus a NULL element gives NULL, not false. DuckDB's
     * list-membership forms lose that NULL, so:
     *
     * - A literal bag `x IN << 1, 2 >>` is rendered as `x IN (1, 2)` rather than the list `x IN [1, 2]`.
     * - Membership against a runtime array (e.g. a column) uses `contains`, wrapped to restore the NULL:
     *   `CASE WHEN contains(arr, x) THEN true WHEN x IS NULL OR arr IS NULL OR list_count(arr) < len(arr) THEN NULL
     *   ELSE false END` (`list_count` skips NULL elements, `len` doesn't).
     *
     * https://duckdb.org/docs/current/functions/array.html#contains
     */
    override fun inCollection(args: SqlArgs): Expr {
        val rhs = args[1].expr
        if (rhs is ExprBag) {
            return exprInCollection(args[0].expr, exprRowValue(rhs.values), false)
        }
        val (value, collection) = inCollectionArrayOperands(args) ?: return super.inCollection(args)
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `<value> IN <array>` was replaced by a NULL-aware DuckDB `contains(<array>, <value>)`",
            ),
        )
        val contains = exprCall(Identifier.regular("contains"), listOf(collection, value))
        val hasNullElement =
            exprOperator(
                "<",
                exprCall(Identifier.regular("list_count"), listOf(collection)),
                exprCall(Identifier.regular("len"), listOf(collection)),
            )
        val unknown =
            exprOr(exprOr(exprNullPredicate(value, false), exprNullPredicate(collection, false)), hasNullElement)
        return exprCase(
            null,
            listOf(
                exprCaseBranch(contains, exprLit(Literal.bool(true))),
                exprCaseBranch(unknown, exprLit(Literal.nul())),
            ),
            exprLit(Literal.bool(false)),
        )
    }

    /**
     * PartiQL `utcnow()` -> DuckDB `timezone('UTC', current_timestamp)`, the current time as a UTC TIMESTAMP.
     *
     * https://duckdb.org/docs/current/functions/timestamptz.html#timezonetext-timestamptz
     */
    private fun utcnow(args: SqlArgs): Expr {
        val call = Identifier.regular("timezone")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL `utcnow()` was replaced by DuckDB `timezone('UTC', current_timestamp)`",
            ),
        )
        val arg0 = exprLit(Literal.string("UTC"))
        val arg1 = unquotedStringExpr("current_timestamp")
        return exprCall(call, listOf(arg0, arg1))
    }

    override fun overlaps(args: SqlArgs): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "DuckDB does not support OVERLAPS predicate",
            ),
        )
    }
}
