package org.partiql.scribe.integ.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.partiql.scribe.integ.config.Config
import org.partiql.scribe.integ.loader.ColumnDef
import org.partiql.scribe.integ.loader.ColumnType
import org.partiql.scribe.integ.loader.TableSchema
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.Column
import software.amazon.awssdk.services.glue.model.CreateTableRequest
import software.amazon.awssdk.services.glue.model.DeleteTableRequest
import software.amazon.awssdk.services.glue.model.SerDeInfo
import software.amazon.awssdk.services.glue.model.StorageDescriptor
import software.amazon.awssdk.services.glue.model.TableInput
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.security.MessageDigest

fun toGlueType(type: ColumnType): String {
    return when (type) {
        is ColumnType.Bool -> "boolean"
        is ColumnType.Int16 -> "smallint"
        is ColumnType.Int32 -> "int"
        is ColumnType.Int64 -> "bigint"
        is ColumnType.Float32 -> "float"
        is ColumnType.Float64 -> "double"
        is ColumnType.StringType -> "string"
        is ColumnType.Char -> "string"
        is ColumnType.Decimal -> "decimal(38,10)"
        is ColumnType.Timestamp -> "timestamp"
        is ColumnType.TimestampTz -> "timestamp"
        is ColumnType.Time -> "string"
        is ColumnType.TimeTz -> "string"
        is ColumnType.Date -> "date"
        is ColumnType.Interval -> "string"
        is ColumnType.Blob -> "binary"
        is ColumnType.Any -> "string"
        is ColumnType.Struct -> {
            val fields = type.fields.joinToString(",") { "${it.name}:${toGlueType(it.type)}" }
            "struct<$fields>"
        }
        is ColumnType.Array -> "array<${toGlueType(type.items)}>"
    }
}

class DataLoader(private val config: Config) {

    private val mapper = ObjectMapper().registerKotlinModule()

    private val s3: S3Client by lazy {
        S3Client.builder()
            .region(config.region)
            .credentialsProvider(config.credentialsProvider)
            .build()
    }

    private val glue: GlueClient by lazy {
        GlueClient.builder()
            .region(config.region)
            .credentialsProvider(config.credentialsProvider)
            .build()
    }

    fun isStale(catalogDir: File): Boolean {
        val currentHash = computeCatalogHash(catalogDir)
        val storedHash = getStoredHash()
        return currentHash != storedHash
    }

    fun load(tables: List<GeneratedTable>, catalogDir: File) {
        for (table in tables) {
            uploadData(table)
            createGlueTable(table.schema)
        }
        storeHash(computeCatalogHash(catalogDir))
    }

    private fun uploadData(table: GeneratedTable) {
        val jsonLines = table.rows.joinToString("\n") { row ->
            mapper.writeValueAsString(row)
        }

        val key = "${config.s3.tablePrefix}${table.schema.name}/data.json"
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(config.s3.bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(jsonLines),
        )
    }

    private fun createGlueTable(schema: TableSchema) {
        val columns = schema.columns.map { col ->
            Column.builder()
                .name(col.name)
                .type(glueType(col.type))
                .build()
        }

        val tableLocation = "s3://${config.s3.bucket}/${config.s3.tablePrefix}${schema.name}/"

        // Delete if exists (idempotent)
        try {
            glue.deleteTable(
                DeleteTableRequest.builder()
                    .databaseName(config.athena.database)
                    .name(schema.name)
                    .build()
            )
        } catch (_: Exception) {}

        glue.createTable(
            CreateTableRequest.builder()
                .databaseName(config.athena.database)
                .tableInput(
                    TableInput.builder()
                        .name(schema.name)
                        .storageDescriptor(
                            StorageDescriptor.builder()
                                .columns(columns)
                                .location(tableLocation)
                                .inputFormat("org.apache.hadoop.mapred.TextInputFormat")
                                .outputFormat("org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat")
                                .serdeInfo(
                                    SerDeInfo.builder()
                                        .serializationLibrary("org.openx.data.jsonserde.JsonSerDe")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun glueType(type: ColumnType): String = toGlueType(type)


    private fun computeCatalogHash(catalogDir: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        catalogDir.listFiles()
            ?.filter { it.extension == "ion" }
            ?.sortedBy { it.name }
            ?.forEach { file -> digest.update(file.readBytes()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getStoredHash(): String? {
        return try {
            val resp = s3.getObjectAsBytes(
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(config.s3.bucket)
                    .key("${config.s3.metadataPrefix}catalog-hash.txt")
                    .build()
            )
            resp.asUtf8String().trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun storeHash(hash: String) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(config.s3.bucket)
                .key("${config.s3.metadataPrefix}catalog-hash.txt")
                .contentType("text/plain")
                .build(),
            RequestBody.fromString(hash),
        )
    }
}
