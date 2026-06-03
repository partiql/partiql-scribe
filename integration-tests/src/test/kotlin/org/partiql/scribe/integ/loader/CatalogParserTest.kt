package org.partiql.scribe.integ.loader

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CatalogParserTest {

    @TempDir
    lateinit var tempDir: File

    private fun createCatalogDir(vararg files: Pair<String, String>): File {
        val catalogDir = File(tempDir, "src/test/resources/catalogs/default")
        catalogDir.mkdirs()
        for ((name, content) in files) {
            File(catalogDir, name).writeText(content)
        }
        return tempDir
    }

    @Test
    fun `parses simple table with basic types`() {
        val scribePath = createCatalogDir(
            "SIMPLE_T.ion" to """
                {
                  type: "bag",
                  items: {
                    type: "struct",
                    constraints: [ closed, ordered, unique ],
                    fields: [
                      { name: "a", type: "bool" },
                      { name: "b", type: "int32" },
                      { name: "c", type: "string" }
                    ]
                  }
                }
            """.trimIndent()
        )

        val parser = CatalogParser(scribePath)
        val schemas = parser.parse()

        assertEquals(1, schemas.size)
        val table = schemas[0]
        assertEquals("SIMPLE_T", table.name)
        assertEquals(3, table.columns.size)
        assertEquals("a", table.columns[0].name)
        assertIs<ColumnType.Bool>(table.columns[0].type)
        assertEquals("b", table.columns[1].name)
        assertIs<ColumnType.Int32>(table.columns[1].type)
        assertEquals("c", table.columns[2].name)
        assertIs<ColumnType.StringType>(table.columns[2].type)
    }

    @Test
    fun `parses nested struct fields`() {
        val scribePath = createCatalogDir(
            "T.ion" to """
                {
                  type: "bag",
                  items: {
                    type: "struct",
                    constraints: [ closed, ordered, unique ],
                    fields: [
                      { name: "a", type: "bool" },
                      {
                        name: "d",
                        type: {
                          type: "struct",
                          constraints: [ closed, ordered, unique ],
                          fields: [
                            { name: "e", type: "string" }
                          ]
                        }
                      }
                    ]
                  }
                }
            """.trimIndent()
        )

        val parser = CatalogParser(scribePath)
        val schemas = parser.parse()

        assertEquals(1, schemas.size)
        val table = schemas[0]
        assertEquals(2, table.columns.size)

        val structCol = table.columns[1]
        assertEquals("d", structCol.name)
        val structType = assertIs<ColumnType.Struct>(structCol.type)
        assertEquals(1, structType.fields.size)
        assertEquals("e", structType.fields[0].name)
        assertIs<ColumnType.StringType>(structType.fields[0].type)
    }

    @Test
    fun `parses array type`() {
        val scribePath = createCatalogDir(
            "ARR.ion" to """
                {
                  type: "bag",
                  items: {
                    type: "struct",
                    constraints: [ closed, ordered, unique ],
                    fields: [
                      {
                        name: "nums",
                        type: { type: "list", items: "int32" }
                      }
                    ]
                  }
                }
            """.trimIndent()
        )

        val parser = CatalogParser(scribePath)
        val schemas = parser.parse()

        assertEquals(1, schemas.size)
        val col = schemas[0].columns[0]
        assertEquals("nums", col.name)
        val arrayType = assertIs<ColumnType.Array>(col.type)
        assertIs<ColumnType.Int32>(arrayType.items)
    }

    @Test
    fun `skips non-bag files`() {
        val scribePath = createCatalogDir(
            "STR.ion" to "\"string\"",
            "VALID.ion" to """
                {
                  type: "bag",
                  items: {
                    type: "struct",
                    fields: [ { name: "x", type: "int32" } ]
                  }
                }
            """.trimIndent()
        )

        val parser = CatalogParser(scribePath)
        val schemas = parser.parse()

        assertEquals(1, schemas.size)
        assertEquals("VALID", schemas[0].name)
    }
}
