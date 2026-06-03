package org.partiql.scribe.integ.engine

import org.partiql.scribe.integ.config.AthenaConfig
import org.partiql.scribe.integ.config.Config
import org.partiql.scribe.integ.loader.Target
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest

class AthenaSparkClient(
    private val athenaConfig: AthenaConfig,
    private val appConfig: Config,
    private val timeoutMs: Long = 120_000,
    private val maxRetries: Int = 3,
) : EngineClient {

    override val target = Target.SPARK

    private val client: AthenaClient by lazy {
        AthenaClient.builder()
            .region(appConfig.region)
            .credentialsProvider(appConfig.credentialsProvider)
            .build()
    }

    override fun execute(sql: String): ExecutionResult {
        val start = System.currentTimeMillis()
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            try {
                return doExecute(sql, start)
            } catch (e: Exception) {
                lastError = e.message
                if (isRetryable(e) && attempt < maxRetries - 1) {
                    Thread.sleep(backoffMs(attempt))
                } else {
                    return ExecutionResult(
                        status = Status.FAILED,
                        error = e.message,
                        durationMs = System.currentTimeMillis() - start,
                    )
                }
            }
        }

        return ExecutionResult(
            status = Status.FAILED,
            error = "Max retries exceeded: $lastError",
            durationMs = System.currentTimeMillis() - start,
        )
    }

    private fun doExecute(sql: String, startTime: Long): ExecutionResult {
        val startReq = StartQueryExecutionRequest.builder()
            .queryString(sql)
            .workGroup(athenaConfig.sparkWorkgroup)
            .queryExecutionContext { it.database(athenaConfig.database) }
            .resultConfiguration(
                ResultConfiguration.builder()
                    .outputLocation(athenaConfig.outputLocation)
                    .build()
            )
            .build()

        val executionId = client.startQueryExecution(startReq).queryExecutionId()
        val state = pollUntilComplete(executionId, startTime)

        return when (state) {
            QueryExecutionState.SUCCEEDED -> {
                val results = client.getQueryResults(
                    GetQueryResultsRequest.builder()
                        .queryExecutionId(executionId)
                        .build()
                )
                val columns = results.resultSet().resultSetMetadata().columnInfo().map {
                    Column(it.name(), it.type())
                }
                val rows = results.resultSet().rows().drop(1).map { row ->
                    row.data().map { it.varCharValue() }
                }
                ExecutionResult(
                    status = Status.SUCCESS,
                    rows = rows,
                    columns = columns,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
            QueryExecutionState.FAILED -> {
                val exec = client.getQueryExecution(
                    GetQueryExecutionRequest.builder()
                        .queryExecutionId(executionId)
                        .build()
                )
                ExecutionResult(
                    status = Status.FAILED,
                    error = exec.queryExecution().status().stateChangeReason(),
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
            else -> ExecutionResult(
                status = Status.TIMEOUT,
                error = "Query timed out after ${timeoutMs}ms",
                durationMs = System.currentTimeMillis() - startTime,
            )
        }
    }

    private fun pollUntilComplete(executionId: String, startTime: Long): QueryExecutionState {
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val resp = client.getQueryExecution(
                GetQueryExecutionRequest.builder()
                    .queryExecutionId(executionId)
                    .build()
            )
            when (val state = resp.queryExecution().status().state()) {
                QueryExecutionState.SUCCEEDED, QueryExecutionState.FAILED, QueryExecutionState.CANCELLED -> return state
                else -> Thread.sleep(1000)
            }
        }
        return QueryExecutionState.UNKNOWN_TO_SDK_VERSION
    }

    override fun healthCheck(): Boolean {
        return try {
            val result = execute("SELECT 1")
            result.status == Status.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    private fun isRetryable(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: return false
        return "throttl" in msg || "toomanyrequests" in msg || "rate exceeded" in msg
    }

    private fun backoffMs(attempt: Int): Long = (1L shl attempt) * 1000
}
