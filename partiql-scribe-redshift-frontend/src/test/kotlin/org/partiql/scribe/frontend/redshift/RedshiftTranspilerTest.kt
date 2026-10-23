package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.partiql.scribe.utils.SessionProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedshiftTranspilerTest {
    private val session = SessionProvider().getSession()

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
    fun readmeExample() {
        val result = RedshiftTranspiler().transpile("SELECT TOP 5 NVL(c, 'none') AS v FROM t", session)
        assertEquals(RedshiftTranspileResult.Status.VALID, result.status)
        assertEquals("SELECT COALESCE(\"t\"['c'], 'none') AS \"v\" FROM \"default\".\"T\" AS \"t\" LIMIT 5", result.partiql)
        assertEquals(2, result.problems.count { it.code == RedshiftProblem.Code.TRANSLATION_INFO })
    }

    @Test
    fun translationNotesHaveLocations() {
        val result = RedshiftTranspiler().transpile("SELECT\n  NVL(c, 'x') AS v FROM T", session)
        assertEquals(RedshiftTranspileResult.Status.VALID, result.status)
        val note = result.problems.single()
        assertEquals(RedshiftProblem.Code.TRANSLATION_INFO, note.code)
        assertEquals(2, note.line)
        assertEquals(3, note.column)
    }

    @Test
    fun sameInputGivesSameOutput() {
        val transpiler = RedshiftTranspiler()
        val sql = "SELECT TOP 3 c, DATEADD(day, 1, timestamp_1) AS d FROM T WHERE b::bigint > 1 ORDER BY c"
        val outputs = (1..5).map { transpiler.transpile(sql, session).partiql }
        assertEquals(1, outputs.toSet().size)
    }

    @Test
    fun quotedIdentifiersFoldToLowerCaseByDefault() {
        val result = RedshiftTranspiler().transpile("SELECT \"C\" FROM T", session)
        assertEquals(RedshiftTranspileResult.Status.VALID, result.status, result.toString())
    }

    @Test
    fun quotedIdentifiersAreCaseSensitiveWhenEnabled() {
        val transpiler = RedshiftTranspiler(caseSensitiveIdentifiers = true)
        val exact = transpiler.transpile("SELECT \"c\" FROM \"T\"", session)
        assertEquals(RedshiftTranspileResult.Status.VALID, exact.status, exact.toString())
        val wrongCase = transpiler.transpile("SELECT \"C\" FROM \"T\"", session)
        assertEquals(RedshiftTranspileResult.Status.INVALID_INPUT, wrongCase.status, wrongCase.toString())
    }

    @Test
    fun unquotedIdentifiersMatchExactlyWhenCaseSensitive() {
        val transpiler = RedshiftTranspiler(caseSensitiveIdentifiers = true)
        val result = transpiler.transpile("SELECT c FROM (SELECT 2 AS \"C\", 1 AS \"c\") AS t", session)
        assertEquals(RedshiftTranspileResult.Status.VALID, result.status, result.toString())
        assertTrue(result.partiql!!.startsWith("SELECT \"t\"['c'] AS \"c\""), result.toString())
        val star = transpiler.transpile("SELECT * FROM (SELECT 2 AS \"C\", 1 AS \"c\") AS t", session)
        assertTrue(star.partiql!!.startsWith("SELECT \"t\"['C'] AS \"C\", \"t\"['c'] AS \"c\""), star.toString())
    }

    @Test
    fun unsupportedConstructHasLocation() {
        val result = RedshiftTranspiler().transpile("SELECT b\nFROM T\nNATURAL JOIN SIMPLE_T", session)
        assertEquals(RedshiftTranspileResult.Status.UNSUPPORTED, result.status)
        val problem = result.problems.single()
        assertEquals(3, problem.line)
        assertEquals(1, problem.column)
        assertTrue(problem.message.contains("NATURAL JOIN"))
    }
}
