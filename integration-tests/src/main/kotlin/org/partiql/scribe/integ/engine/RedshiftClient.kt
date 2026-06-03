package org.partiql.scribe.integ.engine

import org.partiql.scribe.integ.config.Config
import org.partiql.scribe.integ.config.RedshiftConfig
import org.partiql.scribe.integ.loader.Target
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultRequest
import software.amazon.awssdk.services.redshiftdata.model.StatusString

class RedshiftClient(
    private val redshiftConfig: RedshiftConfig,
    private val appConfig: Config,
    private val timeoutMs: Long = 30_000,
    private val maxRetries: Int = 3,
) : EngineClient {

    override val target = Target.REDSHIFT

    private val client: RedshiftDataClient by lazy {
        RedshiftDataClient.builder()
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
        val req = ExecuteStatementRequest.builder()
            .clusterIdentifier(redshiftConfig.clusterIdentifier)
            .database(redshiftConfig.database)
            .dbUser(redshiftConfig.dbUser)
            .sql(sql)
            .build()

        val stmtId = client.executeStatement(req).id()
        val state = pollUntilComplete(stmtId, startTime)

        return when (state) {
            StatusString.FINISHED -> {
                val resultResp = client.getStatementResult(
                    GetStatementResultRequest.builder().id(stmtId).build()
                )
                val columns = resultResp.columnMetadata().map {
                    Column(it.name(), it.typeName())
                }
                val rows = resultResp.records().map { row ->
                    row.map { field ->
                        when {
                            field.isNull == true -> null
                            field.stringValue() != null -> field.stringValue()
                            field.longValue() != null -> field.longValue()
                            field.doubleValue() != null -> field.doubleValue()
                            field.booleanValue() != null -> field.booleanValue()
                            else -> field.stringValue()
                        }
                    }
                }
                ExecutionResult(
                    status = Status.SUCCESS,
                    rows = rows,
                    columns = columns,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            }
            StatusString.FAILED, StatusString.ABORTED -> {
                val desc = client.describeStatement(
                    DescribeStatementRequest.builder().id(stmtId).build()
                )
                ExecutionResult(
                    status = Status.FAILED,
                    error = desc.error(),
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

    private fun pollUntilComplete(stmtId: String, startTime: Long): StatusString {
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val desc = client.describeStatement(
                DescribeStatementRequest.builder().id(stmtId).build()
            )
            when (desc.status()) {
                StatusString.FINISHED, StatusString.FAILED, StatusString.ABORTED -> return desc.status()
                else -> Thread.sleep(500)
            }
        }
        return StatusString.UNKNOWN_TO_SDK_VERSION
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
        return "throttl" in msg || "rate" in msg || "busy" in msg
    }

    private fun backoffMs(attempt: Int): Long = (1L shl attempt) * 1000
}
