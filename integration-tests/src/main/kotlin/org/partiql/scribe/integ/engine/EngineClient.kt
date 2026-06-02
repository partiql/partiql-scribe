package org.partiql.scribe.integ.engine

import org.partiql.scribe.integ.loader.Target

enum class Status { SUCCESS, FAILED, TIMEOUT, SKIPPED, EXPECTED_FAIL }

data class Column(val name: String, val type: String)

data class ExecutionResult(
    val status: Status,
    val rows: List<List<Any?>>? = null,
    val columns: List<Column>? = null,
    val error: String? = null,
    val durationMs: Long = 0,
)

interface EngineClient {
    val target: Target
    fun execute(sql: String): ExecutionResult
    fun healthCheck(): Boolean
}
