package org.partiql.scribe.targets.spark

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprBinary
import org.partiql.ast.exprCall
import org.partiql.ast.exprLit
import org.partiql.ast.identifierSymbol
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlTransform.Companion.id
import org.partiql.scribe.warn
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue

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

    private fun currentUser(sqlArgs: List<SqlArg>): Expr {
        val currentUser = id("current_user")
        log.info("PartiQL CURRENT_USER was replaced by Spark `current_user()`")
        return exprCall(currentUser, emptyList())
    }

    // convert_timezone('UTC', current_timestamp())
    // the default return of current_timestamp function is session time zone dependent.
    // current_timestamp() : https://spark.apache.org/docs/latest/api/sql/index.html#current_timestamp
    // convert_timezone() : https://spark.apache.org/docs/latest/api/sql/index.html#convert_timezone
    @OptIn(PartiQLValueExperimental::class)
    private fun utcnow(sqlArgs: List<SqlArg>): Expr {
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
    private fun transform(sqlArgs: List<SqlArg>): Expr {
        val fnName = id("transform")
        val arrayExpr = sqlArgs[0].expr
        val elementVar = sqlArgs[1].expr
        val elementExpr = sqlArgs[2].expr
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
        var quantity = args[0].expr
        var date = args[1].expr
        var parts = arrayOfNulls<Expr>(7)
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
        val interval = exprCall(id("make_interval"), parts.map { it ?:  exprLit(int32Value(0)) })

        // Add detailed warning about this translation.
        val intervalString = parts.joinToString(
            prefix = "make_interval(",
            postfix = ")",
            separator = ", "
        ) {
           if (it != null) "<quantity>" else "0"
        }
        log.warn("PartiQL `date_add($part, <quantity>, <date>)` was replaced by Spark `<date> + $intervalString`.")

        return exprBinary(
            op = Expr.Binary.Op.PLUS,
            lhs = date,
            rhs = interval,
        )
    }

    /**
     * PartiQL's date_diff accepts a part whereas Spark's datediff returns the difference in days.
     *
     * We extract the datetime part and perform the difference.
     *
     * Notes:
     *  > https://github.com/partiql/partiql-lang-kotlin/wiki/Functions#date_diff----since-v010
     *  > https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/functions.html#datediff(org.apache.spark.sql.Column,org.apache.spark.sql.Column)
     *  > https://spark.apache.org/docs/2.3.0/api/sql/index.html#datediff
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        val extract = when (part) {
            DatetimeField.YEAR -> "year"
            DatetimeField.MONTH -> "month"
            DatetimeField.DAY -> "day"
            DatetimeField.HOUR -> "hour"
            DatetimeField.MINUTE -> "minute"
            DatetimeField.SECOND -> "second"
            else -> error("Unexpected datetime part `$part`")
        }
        val d1 = exprCall(id(extract), listOf(args[0].expr))
        val d2 = exprCall(id(extract), listOf(args[1].expr))
        return exprBinary(
            op = Expr.Binary.Op.MINUS,
            lhs = d2,
            rhs = d1,
        )
    }
}
