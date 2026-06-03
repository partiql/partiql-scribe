package org.partiql.scribe.integ.data

import org.partiql.scribe.integ.loader.ColumnDef
import org.partiql.scribe.integ.loader.ColumnType
import org.partiql.scribe.integ.loader.TableSchema

data class GeneratedTable(
    val schema: TableSchema,
    val rows: List<Map<String, Any?>>,
)

class DataGenerator(private val rowCount: Int = 3) {

    fun generate(schemas: List<TableSchema>): List<GeneratedTable> {
        return schemas.map { schema ->
            GeneratedTable(
                schema = schema,
                rows = (0 until rowCount).map { rowIndex -> generateRow(schema.columns, rowIndex) },
            )
        }
    }

    private fun generateRow(columns: List<ColumnDef>, rowIndex: Int): Map<String, Any?> {
        return columns.associate { col ->
            col.name to generateValue(col.type, rowIndex)
        }
    }

    private fun generateValue(type: ColumnType, rowIndex: Int): Any? {
        return when (type) {
            is ColumnType.Bool -> BOOLS[rowIndex % BOOLS.size]
            is ColumnType.Int16 -> INTS[rowIndex % INTS.size]
            is ColumnType.Int32 -> INTS[rowIndex % INTS.size]
            is ColumnType.Int64 -> LONGS[rowIndex % LONGS.size]
            is ColumnType.Float32 -> DOUBLES[rowIndex % DOUBLES.size]
            is ColumnType.Float64 -> DOUBLES[rowIndex % DOUBLES.size]
            is ColumnType.StringType -> STRINGS[rowIndex % STRINGS.size]
            is ColumnType.Char -> STRINGS[rowIndex % STRINGS.size]
            is ColumnType.Decimal -> DECIMALS[rowIndex % DECIMALS.size]
            is ColumnType.Timestamp -> TIMESTAMPS[rowIndex % TIMESTAMPS.size]
            is ColumnType.TimestampTz -> TIMESTAMPS[rowIndex % TIMESTAMPS.size]
            is ColumnType.Time -> TIMES[rowIndex % TIMES.size]
            is ColumnType.TimeTz -> TIMES_TZ[rowIndex % TIMES_TZ.size]
            is ColumnType.Date -> DATES[rowIndex % DATES.size]
            is ColumnType.Interval -> INTERVALS[rowIndex % INTERVALS.size]
            is ColumnType.Blob -> STRINGS[rowIndex % STRINGS.size]
            is ColumnType.Struct -> {
                type.fields.associate { field ->
                    field.name to generateValue(field.type, rowIndex)
                }
            }
            is ColumnType.Array -> {
                listOf(generateValue(type.items, rowIndex), generateValue(type.items, rowIndex + 1))
            }
            is ColumnType.Any -> STRINGS[rowIndex % STRINGS.size]
        }
    }

    companion object {
        private val BOOLS = listOf(true, false, true)
        private val INTS = listOf(1, 2, 3)
        private val LONGS = listOf(100L, 200L, 300L)
        private val DOUBLES = listOf(1.5, 2.5, 3.5)
        private val STRINGS = listOf("hello", "world", "test")
        private val DECIMALS = listOf("1.23", "4.56", "7.89")
        private val TIMESTAMPS = listOf("2023-01-01 00:00:00", "2023-06-15 12:30:00", "2023-12-31 23:59:59")
        private val TIMES = listOf("10:30:00", "14:45:30", "23:59:59")
        private val TIMES_TZ = listOf("10:30:00+00:00", "14:45:30-07:00", "23:59:59+05:30")
        private val DATES = listOf("2023-01-01", "2023-06-15", "2023-12-31")
        private val INTERVALS = listOf("1", "2", "3")
    }
}
