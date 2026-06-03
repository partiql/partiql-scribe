package org.partiql.scribe.integ.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.partiql.scribe.integ.config.Config
import org.partiql.scribe.integ.loader.ColumnDef
import org.partiql.scribe.integ.loader.ColumnType
import org.partiql.scribe.integ.loader.TableSchema
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.StatusString

class RedshiftDataLoader(private val config: Config) {

    private val mapper = ObjectMapper().registerKotlinModule()

    private val client: RedshiftDataClient by lazy {
        RedshiftDataClient.builder()
            .region(config.region)
            .credentialsProvider(config.credentialsProvider)
            .build()
    }

    fun load(tables: List<GeneratedTable>) {
        executeSql("CREATE SCHEMA IF NOT EXISTS \"default\"")
        executeSql("SET search_path TO \"default\"")

        for (table in tables) {
            createTable(table.schema)
            insertData(table)
        }
    }

    private fun createTable(schema: TableSchema) {
        executeSql("DROP TABLE IF EXISTS \"default\".\"${schema.name}\"")

        val columns = schema.columns.joinToString(",\n  ") { col ->
            "\"${col.name}\" ${toRedshiftType(col.type)}"
        }
        executeSql("CREATE TABLE \"default\".\"${schema.name}\" (\n  $columns\n)")
    }

    private fun insertData(table: GeneratedTable) {
        for (row in table.rows) {
            val values = table.schema.columns.joinToString(", ") { col ->
                formatValue(row[col.name], col.type)
            }
            executeSql("INSERT INTO \"default\".\"${table.schema.name}\" VALUES ($values)")
        }
    }

    private fun formatValue(value: Any?, type: ColumnType): String {
        if (value == null) return "NULL"
        return when (type) {
            is ColumnType.Bool -> if (value as Boolean) "TRUE" else "FALSE"
            is ColumnType.Int16, is ColumnType.Int32 -> "$value"
            is ColumnType.Int64 -> "$value"
            is ColumnType.Float32, is ColumnType.Float64 -> "$value"
            is ColumnType.Decimal -> "$value"
            is ColumnType.StringType, is ColumnType.Char -> "'${(value as String).replace("'", "''")}'"
            is ColumnType.Timestamp, is ColumnType.TimestampTz -> "'$value'"
            is ColumnType.Time, is ColumnType.TimeTz -> "'$value'"
            is ColumnType.Date -> "'$value'"
            is ColumnType.Interval -> "'$value'"
            is ColumnType.Blob -> "'$value'"
            is ColumnType.Any -> "JSON_PARSE('\"${(value as String).replace("'", "''")}\"')"
            is ColumnType.Struct -> "JSON_PARSE('${mapper.writeValueAsString(value).replace("'", "''")}')"
            is ColumnType.Array -> "JSON_PARSE('${mapper.writeValueAsString(value).replace("'", "''")}')"
        }
    }

    private fun executeSql(sql: String) {
        val req = ExecuteStatementRequest.builder()
            .clusterIdentifier(config.redshift.clusterIdentifier)
            .database(config.redshift.database)
            .dbUser(config.redshift.dbUser)
            .sql(sql)
            .build()

        val stmtId = client.executeStatement(req).id()
        pollUntilComplete(stmtId)
    }

    private fun pollUntilComplete(stmtId: String) {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val desc = client.describeStatement(
                DescribeStatementRequest.builder().id(stmtId).build()
            )
            when (desc.status()) {
                StatusString.FINISHED -> return
                StatusString.FAILED, StatusString.ABORTED ->
                    throw RuntimeException("Redshift SQL failed: ${desc.error()}")
                else -> Thread.sleep(500)
            }
        }
        throw RuntimeException("Redshift SQL timed out")
    }

    companion object {
        fun toRedshiftType(type: ColumnType): String {
            return when (type) {
                is ColumnType.Bool -> "BOOLEAN"
                is ColumnType.Int16 -> "SMALLINT"
                is ColumnType.Int32 -> "INTEGER"
                is ColumnType.Int64 -> "BIGINT"
                is ColumnType.Float32 -> "REAL"
                is ColumnType.Float64 -> "DOUBLE PRECISION"
                is ColumnType.StringType -> "VARCHAR(256)"
                is ColumnType.Char -> "VARCHAR(16)"
                is ColumnType.Decimal -> "DECIMAL(38,10)"
                is ColumnType.Timestamp -> "TIMESTAMP"
                is ColumnType.TimestampTz -> "TIMESTAMPTZ"
                is ColumnType.Time -> "TIME"
                is ColumnType.TimeTz -> "TIMETZ"
                is ColumnType.Date -> "DATE"
                is ColumnType.Interval -> "VARCHAR(64)"
                is ColumnType.Blob -> "VARCHAR(256)"
                is ColumnType.Any -> "SUPER"
                is ColumnType.Struct -> "SUPER"
                is ColumnType.Array -> "SUPER"
            }
        }
    }
}
