package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.scribe.frontend.redshift.internal.RedshiftSyntaxException
import kotlin.test.assertEquals

class RedshiftTranspilerTest {
    @Test
    fun tablesListsTheTablesAQueryReads() {
        val sql =
            """
            WITH recent AS (SELECT * FROM andes.Orders.o_items WHERE d > '2024-01-01'),
                 totals AS (SELECT customer_id, SUM(qty) AS q FROM recent GROUP BY 1)
            SELECT t.customer_id, c.name, e
            FROM totals t
              JOIN "Customers"."Dim_Customer" c ON c.id = t.customer_id
              LEFT JOIN andes.orders.o_items AS again ON again.customer_id = t.customer_id,
              c.tags AS e
            WHERE t.q > (SELECT AVG(q) FROM totals) AND EXISTS (SELECT 1 FROM public.flags f WHERE f.id = c.id)
            """.trimIndent()
        val tables = RedshiftTranspiler().tables(sql)
        assertEquals(
            listOf(listOf("andes", "orders", "o_items"), listOf("customers", "dim_customer"), listOf("public", "flags")),
            tables,
        )
    }

    @Test
    fun tablesKeepsCaseWithCaseSensitiveIdentifiers() {
        val tables = RedshiftTranspiler(caseSensitiveIdentifiers = true).tables("SELECT 1 FROM \"Provider\".\"Table\" p, provider.Other")
        assertEquals(listOf(listOf("Provider", "Table"), listOf("provider", "other")), tables)
    }

    @Test
    fun syntaxErrorsHaveLocations() {
        val ex = assertThrows<RedshiftSyntaxException> { RedshiftTranspiler().tables("SELECT a FROM t WHERE") }
        assertEquals(1, ex.line)
        assertEquals(22, ex.column)
    }
}
