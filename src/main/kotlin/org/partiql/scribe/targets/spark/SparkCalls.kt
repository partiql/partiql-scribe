package org.partiql.scribe.targets.spark

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls

public open class SparkCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    private companion object {
        /**
         * Java regex metacharacters, which must be escaped for a delimiter to match literally. Spark compiles the
         * `split` regex with `java.util.regex.Pattern`.
         */
        private val REGEX_METACHARACTERS = setOf('\\', '.', '[', ']', '{', '}', '(', ')', '*', '+', '-', '?', '^', '$', '|')
    }

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            this["current_user"] = ::currentUser
            this["transform"] = ::transform
            this["map_get"] = ::mapGet
            this["cardinality"] = ::cardinalityFn
            this["exists"] = ::existsFn
        }

    private fun currentUser(args: List<SqlArg>): Expr {
        val currentUser = Identifier.regular("current_user")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL CURRENT_USER was replaced by Spark `current_user()`",
            ),
        )
        return exprCall(currentUser, emptyList())
    }

    // convert_timezone('UTC', current_timestamp())
    // the default return of current_timestamp function is session time zone dependent.
    // current_timestamp() : https://spark.apache.org/docs/latest/api/sql/index.html#current_timestamp
    // convert_timezone() : https://spark.apache.org/docs/latest/api/sql/index.html#convert_timezone
    private fun utcnow(args: List<SqlArg>): Expr {
        val convertTimeZone = Identifier.regular("convert_timezone")
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL `utcnow()` was replaced by Spark `convert_timezone('UTC', current_timestamp())`",
            ),
        )
        val currentTimestamp = Identifier.regular("current_timestamp")
        val targetTimezone = exprLit(Literal.string("UTC"))
        return exprCall(convertTimeZone, listOf(targetTimezone, exprCall(currentTimestamp, emptyList())))
    }

    // transform(<array>, <func>) where func transforms each array element w/ syntax elem -> <result value>
    // docs: https://spark.apache.org/docs/latest/api/sql/index.html#transform
    // This function is used for transpilation of `EXCLUDE` collection wildcards. It is similar to a functional map but
    // uses some special syntax (same as Trino's `transform` function).
    // e.g. SELECT transform(array(1, 2, 3), x -> x + 1) outputs [2, 3, 4]
    // encode as `transform(<arrayExpr>, <elementVar>, <elementExpr>)`
    // which gets translated to `transform(<arrayExpr>, <elementVar> -> <elementExpr>)` in RexConverter
    private fun transform(args: List<SqlArg>): Expr {
        val fnName = Identifier.regular("transform")
        val arrayExpr = args[0].expr
        val elementVar = args[1].expr
        val elementExpr = args[2].expr
        return exprCall(fnName, listOf(arrayExpr, elementVar, elementExpr))
    }

    /**
     * PartiQL: date_add(part: datetime_part, quantity: int, date: date|timestamp) -> date|timestamp`
     * Spark:   date + interval
     *
     * We perform the conversion by converting the datetime part and quantity into an interval value, which we add to
     * the date value.
     *
     * Notes:
     *  > https://spark.apache.org/docs/2.3.0/api/sql/index.html#date_add
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#date_add(org.apache.spark.sql.Column,int)
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#add_months(org.apache.spark.sql.Column,int)
     *  > The number of days to can be negative to subtract days
     */
    override fun dateAdd(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val quantity = args[0].expr
        val date = args[1].expr
        val parts = arrayOfNulls<Expr>(7)
        when (part.code()) {
            DatetimeField.YEAR -> parts[0] = quantity
            DatetimeField.MONTH -> parts[1] = quantity
            // skip weeks
            DatetimeField.DAY -> parts[3] = quantity
            DatetimeField.HOUR -> parts[4] = quantity
            DatetimeField.MINUTE -> parts[5] = quantity
            DatetimeField.SECOND -> parts[6] = quantity
            else ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.INVALID_PLAN,
                        "Unexpected datetime part `$part`",
                    ),
                )
        }
        val interval = exprCall(Identifier.regular("make_interval"), parts.map { it ?: exprLit(Literal.intNum(0)) })

        // Add detailed warning about this translation.
        val intervalString =
            parts.joinToString(
                prefix = "make_interval(",
                postfix = ")",
                separator = ", ",
            ) {
                if (it != null) "<quantity>" else "0"
            }
        listener.report(
            ScribeProblem.simpleInfo(
                ScribeProblem.TRANSLATION_INFO,
                "PartiQL `date_add($part, <quantity>, <date>)` was replaced by Spark `<date> + $intervalString`.",
            ),
        )
        return exprOperator(
            symbol = "+",
            lhs = date,
            rhs = interval,
        )
    }

    /**
     * PartiQL's date_diff accepts a part whereas Spark's date_diff returns the difference in days between two dates.
     *
     * For date parts, we use the builtins (or calculate years)
     * For time parts, we convert to unix seconds and convert using 60 seconds per minute and 60 minutes per hour.
     *
     * Notes:
     *  > https://github.com/partiql/partiql-lang-kotlin/wiki/Functions#date_diff----since-v010
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#datediff(org.apache.spark.sql.Column,org.apache.spark.sql.Column)
     *  > https://spark.apache.org/docs/2.3.0/api/sql/index.html#datediff
     */
    override fun dateDiff(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr =
        when (part.code()) {
            DatetimeField.YEAR -> {
                val d1 = args[0].expr
                val d2 = args[1].expr
                val call =
                    exprCall(
                        function = Identifier.regular("months_between"),
                        args = listOf(d2, d1),
                    )
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(year, <date_1>, <date_2>)` was replaced by " +
                            "Spark `CAST(months_between(<date_2>, <date_1>) / 12 AS BIGINT)`.",
                    ),
                )
                truncate(div(call, 12))
            }
            DatetimeField.MONTH -> {
                val d1 = args[0].expr
                val d2 = args[1].expr
                val call =
                    exprCall(
                        function = Identifier.regular("months_between"),
                        args = listOf(d2, d1),
                    )
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(month, <date_1>, <date_2>)` was replaced by " +
                            "Spark `CAST(months_between(<date_2>, <date_1>) AS BIGINT)`.",
                    ),
                )
                truncate(call)
            }
            DatetimeField.DAY -> {
                val d1 = args[0].expr
                val d2 = args[1].expr
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(day, <date_1>, <date_2>)` was replaced by Spark `date_diff(<date_2>, <date_1>)`.",
                    ),
                )
                exprCall(
                    function = Identifier.regular("date_diff"),
                    args = listOf(d2, d1),
                )
            }
            DatetimeField.HOUR -> {
                val d1 = unixTimestamp(args[0].expr)
                val d2 = unixTimestamp(args[1].expr)
                val d = diff(d2, d1)
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(hour, <date_1>, <date_2>)` was replaced by " +
                            "Spark `CAST((unix_timestamp(<date_2>) - unix_timestamp(<date_1>)) / 3600 AS BIGINT)`.",
                    ),
                )
                truncate(div(d, 3600))
            }
            DatetimeField.MINUTE -> {
                val d1 = unixTimestamp(args[0].expr)
                val d2 = unixTimestamp(args[1].expr)
                val d = diff(d2, d1)
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(minute, <date_1>, <date_2>)` was replaced by " +
                            "Spark `CAST((unix_timestamp(<date_2>) - unix_timestamp(<date_1>)) / 60 AS BIGINT)`.",
                    ),
                )
                truncate(div(d, 60))
            }
            DatetimeField.SECOND -> {
                val d1 = unixTimestamp(args[0].expr)
                val d2 = unixTimestamp(args[1].expr)
                listener.report(
                    ScribeProblem.simpleInfo(
                        ScribeProblem.TRANSLATION_INFO,
                        "PartiQL `date_diff(second, <date_1>, <date_2>)` was replaced by " +
                            "Spark `unix_timestamp(<date_2>) - unix_timestamp(<date_1>)`.",
                    ),
                )
                diff(d2, d1)
            }
            else ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.INVALID_PLAN,
                        "Unexpected datetime part `$part`",
                    ),
                )
        }

    /**
     * PartiQL `split(<string>, <delimiter>) -> list<string>` matches the delimiter **literally**, but Spark's
     * `split(str, regex)` interprets its second argument as a Java regular expression. Passing the delimiter through
     * unchanged silently mis-splits whenever it holds a regex metacharacter, e.g. PartiQL `split(v, '.')` splits on a
     * literal dot while Spark `split(v, '.')` splits on *every* character.
     *
     * We restore literal semantics by quoting the delimiter:
     *  - String literals are escaped at transpile time, which keeps the emitted SQL readable, e.g. `split(v, '\\.')`.
     *  - Any other expression (a column, a call, ...) is unknown until runtime, so we wrap it with Spark `concat` into
     *    a `\Q...\E` quoted region, mirroring Java's `Pattern.quote`.
     *
     * Notes:
     *  > https://spark.apache.org/docs/latest/api/sql/index.html#split
     *  > https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#quote-java.lang.String-
     */
    override fun split(args: SqlArgs): Expr {
        val id = Identifier.regular("SPLIT")
        val string = args[0].expr
        val delimiter = args[1].expr
        val quotedDelimiter =
            when {
                delimiter is ExprLit && delimiter.lit.code() == Literal.STRING -> {
                    listener.report(
                        ScribeProblem.simpleInfo(
                            code = ScribeProblem.TRANSLATION_INFO,
                            message =
                                "PartiQL `split(<string>, <delimiter>)` matches the delimiter literally whereas Spark " +
                                    "`split(str, regex)` takes a regex; the delimiter literal was regex-escaped.",
                        ),
                    )
                    exprLit(Literal.string(escapeRegex(delimiter.lit.stringValue())))
                }
                else -> {
                    listener.report(
                        ScribeProblem.simpleInfo(
                            code = ScribeProblem.TRANSLATION_INFO,
                            message =
                                "PartiQL `split(<string>, <delimiter>)` matches the delimiter literally whereas Spark " +
                                    "`split(str, regex)` takes a regex; the non-literal delimiter was wrapped in a " +
                                    "`\\Q...\\E` quoted region.",
                        ),
                    )
                    patternQuote(delimiter)
                }
            }
        return exprCall(id, listOf(string, quotedDelimiter))
    }

    /**
     * Escape the Java regex metacharacters in [delimiter] so that it matches literally.
     *
     * There are two layers of unescaping between this string and the regex engine, so escaping happens in two stages:
     *  1. Regex: prefix each metacharacter with a backslash, so `.` becomes `\.`.
     *  2. String literal: Spark's parser also processes backslash escapes inside string literals, so every backslash is
     *     doubled to survive that pass, turning `\.` into `\\.`.
     *
     * This yields the standard Spark idiom -- splitting on a literal `.` is written `split(v, '\\.')`. Note a literal
     * backslash needs all four: regex `\\` written as `'\\\\'`.
     */
    private fun escapeRegex(delimiter: String): String {
        val regexEscaped =
            delimiter.flatMap { char ->
                when (char) {
                    in REGEX_METACHARACTERS -> listOf('\\', char)
                    else -> listOf(char)
                }
            }.joinToString("")
        // Double every backslash so it survives Spark's string-literal unescaping.
        return regexEscaped.replace("\\", "\\\\")
    }

    /**
     * Wrap [delimiter] in a `\Q...\E` quoted region, equivalent to Java's `Pattern.quote`.
     *
     * Any `\E` already inside the delimiter would terminate the region early and expose the remainder to the regex
     * engine, so occurrences are closed and reopened (`\E\\E\Q`) exactly as `Pattern.quote` does. The
     * [replace][exprCall] runs in Spark, since the delimiter's value is not known at transpile time.
     */
    private fun patternQuote(delimiter: Expr): Expr {
        val sanitized =
            exprCall(
                function = Identifier.regular("REPLACE"),
                args =
                    listOf(
                        delimiter,
                        exprLit(Literal.string("\\\\E")),
                        exprLit(Literal.string("\\\\E\\\\\\\\E\\\\Q")),
                    ),
            )
        return exprCall(
            function = Identifier.regular("CONCAT"),
            args =
                listOf(
                    exprLit(Literal.string("\\\\Q")),
                    sanitized,
                    exprLit(Literal.string("\\\\E")),
                ),
        )
    }

    /**
     * PartiQL `map_get(map, key)` -> Spark `element_at(map, key)`
     */
    private fun mapGet(args: SqlArgs): Expr {
        val id = Identifier.regular("element_at")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_get` was replaced by Spark `element_at`",
            ),
        )
        return exprCall(id, listOf(args[0].expr, args[1].expr))
    }

    /**
     * PartiQL `cardinality(collection)` -> Spark `size(collection)`
     */
    private fun cardinalityFn(args: SqlArgs): Expr {
        val id = Identifier.regular("size")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `cardinality` was replaced by Spark `size`",
            ),
        )
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * PartiQL `exists(collection)` -> Spark `size(collection) > 0`
     */
    private fun existsFn(args: SqlArgs): Expr {
        val sizeId = Identifier.regular("size")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `exists` was replaced by Spark `size(...) > 0`",
            ),
        )
        val sizeCall = exprCall(sizeId, listOf(args[0].expr))
        return exprOperator(">", sizeCall, exprLit(Literal.intNum(0)))
    }

    override fun overlaps(args: SqlArgs): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Spark does not support OVERLAPS predicate.",
            ),
        )
    }

    private fun truncate(arg: Expr): Expr =
        exprCast(
            value = arg,
            asType = DataType.BIGINT(),
        )

    private fun diff(
        lhs: Expr,
        rhs: Expr,
    ) = exprOperator(
        symbol = "-",
        lhs = lhs,
        rhs = rhs,
    )

    private fun div(
        arg: Expr,
        divisor: Int,
    ) = exprOperator(
        symbol = "/",
        lhs = arg,
        rhs = exprLit(Literal.intNum(divisor)),
    )

    private fun unixTimestamp(arg: Expr) =
        exprCall(
            function = Identifier.regular("unix_timestamp"),
            args = listOf(arg),
        )
}
