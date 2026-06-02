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
    object Float64 : ColumnType()
    object StringType : ColumnType()
    object Decimal : ColumnType()
    object Timestamp : ColumnType()
    object Date : ColumnType()
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
        val itemsValue = struct["items"] as? IonStruct ?: return emptyList()
        val fields = itemsValue["fields"] as? com.amazon.ion.IonList ?: return emptyList()

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
                "int32" -> ColumnType.Int32
                "int64" -> ColumnType.Int64
                "float64" -> ColumnType.Float64
                "string" -> ColumnType.StringType
                "decimal" -> ColumnType.Decimal
                "timestamp" -> ColumnType.Timestamp
                "date" -> ColumnType.Date
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
                    else -> ColumnType.Any
                }
            }
            else -> ColumnType.Any
        }
    }
}
