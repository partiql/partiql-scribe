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
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls

public open class SparkCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            this["current_user"] = ::currentUser
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
