package org.partiql.scribe.targets.spark

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.exprBinary
import org.partiql.ast.exprCall
import org.partiql.ast.exprCast
import org.partiql.ast.exprLit
import org.partiql.ast.typeBigint
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlTransform.Companion.id
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
public open class SparkCalls(private val log: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        this["current_user"] = ::currentUser
        this["transform"] = ::transform
    }

    // https://spark.apache.org/docs/latest/api/sql/index.html#array_contains
    override fun inCollection(args: List<SqlArg>): Expr {
        val call = id("array_contains")
        log.info("PartiQL value IN collection was replaced by Spark `array_contains(collection, value)`")
        return exprCall(call, args.map { it.expr })
    }

    private fun currentUser(args: List<SqlArg>): Expr {
        val currentUser = id("current_user")
        log.info("PartiQL CURRENT_USER was replaced by Spark `current_user()`")
        return exprCall(currentUser, emptyList())
    }

    // convert_timezone('UTC', current_timestamp())
    // the default return of current_timestamp function is session time zone dependent.
    // current_timestamp() : https://spark.apache.org/docs/latest/api/sql/index.html#current_timestamp
    // convert_timezone() : https://spark.apache.org/docs/latest/api/sql/index.html#convert_timezone
    @OptIn(PartiQLValueExperimental::class)
    private fun utcnow(args: List<SqlArg>): Expr {
        val convertTimeZone = id("convert_timezone")
        log.info("PartiQL `utcnow()` was replaced by Spark `convert_timezone('UTC', current_timestamp())`")
        val currentTimestamp = id("current_timestamp")
        val targetTimezone = exprLit(stringValue("utc"))
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
        val fnName = id("transform")
        val arrayExpr = args[0].expr
        val elementVar = args[1].expr
        val elementExpr = args[2].expr
        return exprCall(fnName, listOf(arrayExpr, elementVar, elementExpr))
    }

    /**
     * PartiQL: date_add(part: datetime_part, quantity: int, date: date|timestamp) -> date|timestamp`
     * Spark:   date + interval
     *
     * We perform the conversion by a
     *
     * Notes:
     *  > https://spark.apache.org/docs/2.3.0/api/sql/index.html#date_add
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#date_add(org.apache.spark.sql.Column,int)
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#add_months(org.apache.spark.sql.Column,int)
     *  > The number of days to can be negative to subtract days
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        val quantity = args[0].expr
        val date = args[1].expr
        val parts = arrayOfNulls<Expr>(7)
        when (part) {
            DatetimeField.YEAR -> parts[0] = quantity
            DatetimeField.MONTH -> parts[1] = quantity
            // skip weeks
            DatetimeField.DAY -> parts[3] = quantity
            DatetimeField.HOUR -> parts[4] = quantity
            DatetimeField.MINUTE -> parts[5] = quantity
            DatetimeField.SECOND -> parts[6] = quantity
            else -> error("Unexpected datetime part `$part`")
        }
        val interval = exprCall(id("make_interval"), parts.map { it ?: exprLit(int32Value(0)) })

        // Add detailed warning about this translation.
        val intervalString = parts.joinToString(
            prefix = "make_interval(",
            postfix = ")",
            separator = ", "
        ) {
            if (it != null) "<quantity>" else "0"
        }
        log.info("PartiQL `date_add($part, <quantity>, <date>)` was replaced by Spark `<date> + $intervalString`.")

        return exprBinary(
            op = Expr.Binary.Op.PLUS,
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
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr = when (part) {
        DatetimeField.YEAR -> {
            val d1 = args[0].expr
            val d2 = args[1].expr
            val call = exprCall(
                function = id("months_between"),
                args = listOf(d2, d1)
            )
            log.info("PartiQL `date_diff(year, <date_1>, <date_2>)` was replaced by Spark `CAST(months_between(<date_2>, <date_1>) / 12 AS BIGINT)`.")
            truncate(div(call, 12))
        }
        DatetimeField.MONTH -> {
            val d1 = args[0].expr
            val d2 = args[1].expr
            val call = exprCall(
                function = id("months_between"),
                args = listOf(d2, d1)
            )
            log.info("PartiQL `date_diff(month, <date_1>, <date_2>)` was replaced by Spark `CAST(months_between(<date_2>, <date_1>) AS BIGINT)`.")
            truncate(call)
        }
        DatetimeField.DAY -> {
            val d1 = args[0].expr
            val d2 = args[1].expr
            log.info("PartiQL `date_diff(day, <date_1>, <date_2>)` was replaced by Spark `date_diff(<date_2>, <date_1>)`.")
            exprCall(
                function = id("date_diff"),
                args = listOf(d2, d1)
            )
        }
        DatetimeField.HOUR -> {
            val d1 = unixTimestamp(args[0].expr)
            val d2 = unixTimestamp(args[1].expr)
            val d = diff(d2, d1)
            log.info("PartiQL `date_diff(hour, <date_1>, <date_2>)` was replaced by Spark `CAST((unix_timestamp(<date_2>) - unix_timestamp(<date_1>)) / 3600 AS BIGINT)`.")
            truncate(div(d, 3600))
        }
        DatetimeField.MINUTE -> {
            val d1 = unixTimestamp(args[0].expr)
            val d2 = unixTimestamp(args[1].expr)
            val d = diff(d2, d1)
            log.info("PartiQL `date_diff(minute, <date_1>, <date_2>)` was replaced by Spark `CAST((unix_timestamp(<date_2>) - unix_timestamp(<date_1>)) / 60 AS BIGINT)`.")
            truncate(div(d, 60))
        }
        DatetimeField.SECOND -> {
            val d1 = unixTimestamp(args[0].expr)
            val d2 = unixTimestamp(args[1].expr)
            log.info("PartiQL `date_diff(second, <date_1>, <date_2>)` was replaced by Spark `unix_timestamp(<date_2>) - unix_timestamp(<date_1>)`.")
            diff(d2, d1)
        }
        else -> error("Unexpected datetime part `$part`")
    }

    private fun truncate(arg: Expr): Expr = exprCast(
        value = arg,
        asType = typeBigint(),
    )

    private fun diff(lhs: Expr, rhs: Expr) = exprBinary(
        op = Expr.Binary.Op.MINUS,
        lhs = lhs,
        rhs = rhs,
    )

    private fun div(arg: Expr, divisor: Int) = exprBinary(
        op = Expr.Binary.Op.DIVIDE,
        lhs = arg,
        rhs = exprLit(int32Value(divisor))
    )

    private fun unixTimestamp(arg: Expr) = exprCall(
        function = id("unix_timestamp"),
        args = listOf(arg),
    )
}
