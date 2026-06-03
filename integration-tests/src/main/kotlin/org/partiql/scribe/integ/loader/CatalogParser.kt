package org.partiql.scribe.integ.loader

import com.amazon.ion.IonStruct
import com.amazon.ion.IonText
import com.amazon.ion.system.IonSystemBuilder
import java.io.File

data class TableSchema(
    val name: String,
    val columns: List<ColumnDef>,
)

data class ColumnDef(
    val name: String,
    val type: ColumnType,
    val nullable: Boolean = true,
)

sealed class ColumnType {
    object Bool : ColumnType()
    object Int32 : ColumnType()
    object Int64 : ColumnType()
    object Float32 : ColumnType()
    object Float64 : ColumnType()
    object StringType : ColumnType()
    object Decimal : ColumnType()
    object Timestamp : ColumnType()
    object TimestampTz : ColumnType()
    object Time : ColumnType()
    object TimeTz : ColumnType()
    object Date : ColumnType()
    object Interval : ColumnType()
    object Blob : ColumnType()
    object Int16 : ColumnType()
    object Char : ColumnType()
    object Any : ColumnType()
    data class Struct(val fields: List<ColumnDef>) : ColumnType()
    data class Array(val items: ColumnType) : ColumnType()
}

class CatalogParser(private val scribePath: File) {

    private val ion = IonSystemBuilder.standard().build()
    private val catalogDir = scribePath.resolve("src/test/resources/catalogs/default")

    fun parse(): List<TableSchema> {
        if (!catalogDir.exists()) return emptyList()

        return catalogDir.listFiles()
            ?.filter { it.extension == "ion" }
            ?.mapNotNull { parseFile(it) }
            ?: emptyList()
    }

    private fun parseFile(file: File): TableSchema? {
        val tableName = file.nameWithoutExtension
        val datagram = ion.newLoader().load(file)
        val root = datagram.firstOrNull() as? IonStruct ?: return null

        val typeName = (root["type"] as? IonText)?.stringValue()
        if (typeName != "bag") return null

        val columns = extractFields(root)
        if (columns.isEmpty()) return null
        return TableSchema(name = tableName, columns = columns)
    }

    private fun extractFields(struct: IonStruct): List<ColumnDef> {
        // For top-level bag types: fields are under items.fields
        // For nested struct types: fields are directly on the struct
        val fields = (struct["items"] as? IonStruct)?.let { it["fields"] as? com.amazon.ion.IonList }
            ?: (struct["fields"] as? com.amazon.ion.IonList)
            ?: return emptyList()

        return fields.mapNotNull { field ->
            val fieldStruct = field as? IonStruct ?: return@mapNotNull null
            val name = (fieldStruct["name"] as? IonText)?.stringValue() ?: return@mapNotNull null
            val type = parseType(fieldStruct["type"])
            ColumnDef(name = name, type = type)
        }
    }

    private fun parseType(value: com.amazon.ion.IonValue?): ColumnType {
        if (value == null) return ColumnType.Any
        return when {
            value is IonText -> when (value.stringValue()) {
                "bool" -> ColumnType.Bool
                "int16" -> ColumnType.Int16
                "int32" -> ColumnType.Int32
                "int64" -> ColumnType.Int64
                "float32" -> ColumnType.Float32
                "float64" -> ColumnType.Float64
                "string" -> ColumnType.StringType
                "char" -> ColumnType.Char
                "decimal" -> ColumnType.Decimal
                "timestamp" -> ColumnType.Timestamp
                "timestampz" -> ColumnType.TimestampTz
                "time" -> ColumnType.Time
                "timez" -> ColumnType.TimeTz
                "date" -> ColumnType.Date
                "blob" -> ColumnType.Blob
                "intervalY", "intervalMon", "intervalD", "intervalH", "intervalMin", "intervalS",
                "intervalY2Mon", "intervalD2H", "intervalD2Min", "intervalD2S",
                "intervalH2Min", "intervalH2S", "intervalMin2S" -> ColumnType.Interval
                "any" -> ColumnType.Any
                else -> ColumnType.Any
            }
            value is IonStruct -> {
                val typeName = (value["type"] as? IonText)?.stringValue()
                when (typeName) {
                    "struct" -> ColumnType.Struct(extractFields(value))
                    "list", "bag" -> {
                        val items = parseType(value["items"])
                        ColumnType.Array(items)
                    }
                    "timez" -> ColumnType.TimeTz
                    "timestampz" -> ColumnType.TimestampTz
                    "decimal" -> ColumnType.Decimal
                    else -> ColumnType.Any
                }
            }
            else -> ColumnType.Any
        }
    }
}
