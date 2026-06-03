package org.partiql.scribe.integ.data

import org.junit.jupiter.api.Test
import org.partiql.scribe.integ.loader.ColumnDef
import org.partiql.scribe.integ.loader.ColumnType
import kotlin.test.assertEquals

class GlueTypeTest {

    @Test
    fun `basic types map correctly`() {
        assertEquals("boolean", toGlueType(ColumnType.Bool))
        assertEquals("int", toGlueType(ColumnType.Int32))
        assertEquals("bigint", toGlueType(ColumnType.Int64))
        assertEquals("double", toGlueType(ColumnType.Float64))
        assertEquals("string", toGlueType(ColumnType.StringType))
        assertEquals("decimal(38,10)", toGlueType(ColumnType.Decimal))
        assertEquals("timestamp", toGlueType(ColumnType.Timestamp))
        assertEquals("date", toGlueType(ColumnType.Date))
        assertEquals("string", toGlueType(ColumnType.Any))
    }

    @Test
    fun `struct type with fields`() {
        val structType = ColumnType.Struct(
            fields = listOf(
                ColumnDef("e", ColumnType.StringType),
                ColumnDef("f", ColumnType.Int32),
            )
        )
        assertEquals("struct<e:string,f:int>", toGlueType(structType))
    }

    @Test
    fun `nested struct type`() {
        val nested = ColumnType.Struct(
            fields = listOf(
                ColumnDef("inner", ColumnType.Struct(
                    fields = listOf(ColumnDef("val", ColumnType.Int32))
                ))
            )
        )
        assertEquals("struct<inner:struct<val:int>>", toGlueType(nested))
    }

    @Test
    fun `array type`() {
        assertEquals("array<int>", toGlueType(ColumnType.Array(ColumnType.Int32)))
        assertEquals("array<string>", toGlueType(ColumnType.Array(ColumnType.StringType)))
    }

    @Test
    fun `array of struct`() {
        val type = ColumnType.Array(
            ColumnType.Struct(listOf(ColumnDef("x", ColumnType.Int32)))
        )
        assertEquals("array<struct<x:int>>", toGlueType(type))
    }
}
