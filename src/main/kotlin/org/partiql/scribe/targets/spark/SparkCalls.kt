package org.partiql.scribe.targets.spark

import org.partiql.ast.Expr
import org.partiql.ast.exprCall
import org.partiql.ast.exprLit
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlTransform.Companion.id
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

public open class SparkCalls(private val log: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        this["current_user"] = ::currentUser
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
}