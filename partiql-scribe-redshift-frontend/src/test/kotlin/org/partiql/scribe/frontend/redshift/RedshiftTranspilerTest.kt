package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.partiql.scribe.utils.SessionProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedshiftTranspilerTest {
    private val session = SessionProvider().getSession()

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
    fun unsupportedConstructHasLocation() {
        val result = RedshiftTranspiler().transpile("SELECT b\nFROM T\nNATURAL JOIN SIMPLE_T", session)
        assertEquals(RedshiftTranspileResult.Status.UNSUPPORTED, result.status)
        val problem = result.problems.single()
        assertEquals(3, problem.line)
        assertEquals(1, problem.column)
        assertTrue(problem.message.contains("NATURAL JOIN"))
    }
}
