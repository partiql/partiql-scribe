package org.partiql.scribe.integ.data

import org.junit.jupiter.api.Test
import org.partiql.scribe.integ.loader.ColumnDef
import org.partiql.scribe.integ.loader.ColumnType
import org.partiql.scribe.integ.loader.TableSchema
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DataGeneratorTest {

    @Test
    fun `generates correct number of rows`() {
        val schema = TableSchema(
            name = "TEST",
            columns = listOf(ColumnDef("a", ColumnType.Int32))
        )
        val generator = DataGenerator(rowCount = 5)
        val tables = generator.generate(listOf(schema))

        assertEquals(1, tables.size)
        assertEquals(5, tables[0].rows.size)
    }

    @Test
    fun `generates values for basic types`() {
        val schema = TableSchema(
            name = "TEST",
            columns = listOf(
                ColumnDef("bool_col", ColumnType.Bool),
                ColumnDef("int_col", ColumnType.Int32),
                ColumnDef("long_col", ColumnType.Int64),
                ColumnDef("double_col", ColumnType.Float64),
                ColumnDef("string_col", ColumnType.StringType),
                ColumnDef("decimal_col", ColumnType.Decimal),
                ColumnDef("ts_col", ColumnType.Timestamp),
                ColumnDef("date_col", ColumnType.Date),
            )
        )
        val generator = DataGenerator(rowCount = 1)
        val row = generator.generate(listOf(schema))[0].rows[0]

        assertIs<Boolean>(row["bool_col"])
        assertIs<Int>(row["int_col"])
        assertIs<Long>(row["long_col"])
        assertIs<Double>(row["double_col"])
        assertIs<String>(row["string_col"])
        assertIs<String>(row["decimal_col"])
        assertIs<String>(row["ts_col"])
        assertIs<String>(row["date_col"])
    }

    @Test
    fun `generates nested struct data`() {
        val schema = TableSchema(
            name = "TEST",
            columns = listOf(
                ColumnDef("d", ColumnType.Struct(
                    fields = listOf(
                        ColumnDef("e", ColumnType.StringType),
                        ColumnDef("f", ColumnType.Int32),
                    )
                ))
            )
        )
        val generator = DataGenerator(rowCount = 1)
        val row = generator.generate(listOf(schema))[0].rows[0]

        val structVal = row["d"]
        assertNotNull(structVal)
        assertIs<Map<*, *>>(structVal)
        val map = structVal as Map<String, Any?>
        assertEquals("hello", map["e"])
        assertEquals(1, map["f"])
    }

    @Test
    fun `generates array data`() {
        val schema = TableSchema(
            name = "TEST",
            columns = listOf(
                ColumnDef("nums", ColumnType.Array(ColumnType.Int32))
            )
        )
        val generator = DataGenerator(rowCount = 1)
        val row = generator.generate(listOf(schema))[0].rows[0]

        val arr = row["nums"]
        assertNotNull(arr)
        assertIs<List<*>>(arr)
        assertEquals(2, (arr as List<*>).size)
    }
}
