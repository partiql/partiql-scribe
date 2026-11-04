package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.problems.Severity
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.utils.SessionProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The public contract of [RedshiftFrontend]: the table listing, the problem codes and their positions, the notes, and
 * determinism, and that the AST plans and compiles with the standard PartiQL planner and the Scribe targets. The
 * translation itself is covered by [RedshiftTranspilerSuite].
 */
class RedshiftTranspilerTest {
    private val session = SessionProvider().getSession()
    private val planner = PartiQLPlanner.standard()

    /** A context that records every problem and throws on an error, as [ScribeContext.standard] does. */
    private class Recording : ScribeContext {
        val problems = mutableListOf<ScribeProblem>()

        override fun getProblemListener(): ScribeProblemListener =
            object : ScribeProblemListener {
                override fun report(problem: ScribeProblem) {
                    problems.add(problem)
                    if (problem.severity.code() == Severity.ERROR) throw ScribeException(problem)
                }
            }
    }

    private fun frontend(context: ScribeContext = ScribeContext.standard()) = RedshiftFrontend(context)

    private fun transpile(
        sql: String,
        context: Recording = Recording(),
    ): String {
        val plan = planner.plan(frontend(context).toAst(sql), session).plan
        return Scribe(context).compile(plan, session, PartiQLTarget).output.value
    }

    private fun failure(sql: String): ScribeProblem = assertThrows<ScribeException> { frontend().toAst(sql) }.error

    private fun ScribeProblem.message() = getOrNull("MESSAGE", String::class.java)!!

    private fun ScribeProblem.kind() = getOrNull(RedshiftFrontend.KIND, String::class.java)

    private fun ScribeProblem.position() = getOrNull("LINE", Int::class.javaObjectType) to getOrNull("COLUMN", Int::class.javaObjectType)

    @Test
    fun tablesListsEveryTableOnce() {
        val sql = "SELECT t.a FROM db.schema.T AS t JOIN simple_t ON t.b = simple_t.b, SIMPLE_T WHERE t.b IN (SELECT b FROM T)"
        assertEquals(listOf(listOf("db", "schema", "t"), listOf("simple_t"), listOf("t")), frontend().tables(sql))
    }

    @Test
    fun tablesSkipsCommonTableExpressions() {
        assertEquals(listOf(listOf("t"), listOf("simple_t")), frontend().tables("WITH c AS (SELECT a FROM T) SELECT a FROM c, SIMPLE_T"))
    }

    @Test
    fun tablesFoldsIdentifiers() {
        val sql = "SELECT a FROM Mixed.\"Case\""
        assertEquals(listOf(listOf("mixed", "case")), frontend().tables(sql))
        val caseSensitive = RedshiftFrontend(ScribeContext.standard(), caseSensitiveIdentifiers = true)
        assertEquals(listOf(listOf("mixed", "Case")), caseSensitive.tables(sql))
    }

    @Test
    fun tablesReportsTheFirstSyntaxError() {
        val ex = assertThrows<ScribeException> { frontend().tables("SELECT a FROM T WHERE") }
        assertEquals("UNSUPPORTED_OPERATION", ex.error.name())
        assertEquals(1 to 22, ex.error.position())
    }

    @Test
    fun tablesSeesTheCommonTableExpressionsThatTheAstSees() {
        // A common table expression sees the ones before it, so SIMPLE_T in the first body is the table.
        val sql = "WITH x AS (SELECT * FROM simple_t), simple_t AS (SELECT a FROM T) SELECT * FROM x"
        assertEquals(listOf(listOf("simple_t"), listOf("t")), frontend().tables(sql))
        // The query that the common table expression shadows reads the table of the same name.
        assertEquals(listOf(listOf("t")), frontend().tables("WITH t AS (SELECT a FROM t) SELECT a FROM t"))
    }

    @Test
    fun tablesRefusesWhatToAstRefusesAndReportsNoNotes() {
        val context = Recording()
        val ex = assertThrows<ScribeException> { RedshiftFrontend(context).tables("SELECT NVL(a, 0) FROM T") }
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, ex.error.kind())
        val notes = Recording()
        RedshiftFrontend(notes).tables("SELECT DISTINCT c FROM SIMPLE_T ORDER BY c LIMIT 5")
        assertTrue(notes.problems.isEmpty(), notes.problems.toString())
    }

    @Test
    fun caseSensitiveIdentifiersKeepTheirCaseInTheAst() {
        val sql = "SELECT c AS \"Mixed\" FROM T"
        val folded = transpile(sql)
        assertTrue(folded.contains("AS \"mixed\""), folded)
        val context = Recording()
        val caseSensitive = RedshiftFrontend(context, caseSensitiveIdentifiers = true)
        val plan = planner.plan(caseSensitive.toAst(sql), session).plan
        val kept = Scribe(context).compile(plan, session, PartiQLTarget).output.value
        assertTrue(kept.contains("AS \"Mixed\""), kept)
    }

    @Test
    fun problemsCarryTheirKind() {
        assertEquals(RedshiftFrontend.KIND_SYNTAX, failure("SELECT a FROM SIMPLE_T WHERE b >").kind())
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, failure("SELECT a FROM SIMPLE_T WHERE UPPER(c) = 'X'").kind())
        assertEquals(RedshiftFrontend.KIND_INVALID, failure("SELECT a FROM SIMPLE_T ORDER BY 2").kind())
    }

    @Test
    fun deepParenthesesAreRefused() {
        val depth = RedshiftFrontend.MAX_NESTING + 1
        val sql = "SELECT " + "(".repeat(depth) + "1" + ")".repeat(depth) + " FROM SIMPLE_T"
        val problem = failure(sql)
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, problem.kind())
        assertTrue(problem.message().contains("nested"), problem.message())
        assertEquals(1 to 8 + RedshiftFrontend.MAX_NESTING, problem.position())
        assertThrows<ScribeException> { frontend().tables(sql) }
        // The limit is high enough for a query of ordinary depth, and that depth converts, plans, and compiles.
        val fine = "SELECT " + "(".repeat(RedshiftFrontend.MAX_NESTING) + "1" + ")".repeat(RedshiftFrontend.MAX_NESTING) + " FROM SIMPLE_T"
        assertTrue(transpile(fine).startsWith("SELECT 1 AS"), fine)
    }

    @Test
    fun deepCaseExpressionsAreRefused() {
        fun nested(depth: Int) = "SELECT " + "CASE WHEN a THEN 1 ELSE ".repeat(depth) + "1" + " END".repeat(depth) + " AS v FROM T"
        val problem = failure(nested(RedshiftFrontend.MAX_NESTING + 1))
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, problem.kind())
        assertTrue(problem.message().contains("CASE"), problem.message())
        assertTrue(transpile(nested(RedshiftFrontend.MAX_NESTING)).contains("CASE WHEN"))
        // Far deeper than the parser's stack would take; refused before parsing.
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, failure(nested(5000)).kind())
    }

    @Test
    fun longPrefixRunsAreRefused() {
        val nots = failure("SELECT a FROM T WHERE " + "NOT ".repeat(RedshiftFrontend.MAX_NESTING + 1) + "a")
        assertTrue(nots.message().contains("prefix operators"), nots.message())
        val signs = failure("SELECT " + "- ".repeat(5000) + "b FROM T")
        assertTrue(signs.message().contains("prefix operators"), signs.message())
        assertTrue(transpile("SELECT a FROM T WHERE " + "NOT ".repeat(RedshiftFrontend.MAX_NESTING) + "a").contains("NOT"))
    }

    @Test
    fun deepOperatorChainsAreRefusedAtTheirToken() {
        // No parentheses: each AND is a level of the AST, and the AST is bounded so the planner's recursion is too.
        val terms = RedshiftFrontend.MAX_NESTING + 2
        val sql = "SELECT a FROM SIMPLE_T WHERE " + (1..terms).joinToString(" AND ") { "b = $it" }
        val problem = failure(sql)
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, problem.kind())
        assertTrue(problem.message().contains("levels deep"), problem.message())
        val (line, column) = problem.position()
        assertEquals(1, line)
        assertEquals("AND", sql.substring(column!! - 1, column + 2))
        val fine = "SELECT a FROM SIMPLE_T WHERE " + (1..RedshiftFrontend.MAX_NESTING).joinToString(" AND ") { "b = $it" }
        assertTrue(transpile(fine).contains(" AND "))
        for (chain in listOf(" || ", " + ", " UNION ALL ")) {
            val deep =
                if (chain == " UNION ALL ") {
                    (1..5000).joinToString(chain) { "SELECT a FROM T" }
                } else {
                    "SELECT " + (1..5000).joinToString(chain) { "b" } + " FROM T"
                }
            assertTrue(failure(deep).message().contains("levels deep"), chain)
        }
        val joins = "SELECT t0.a FROM T AS t0 " + (1..5000).joinToString(" ") { "JOIN T AS t$it ON t0.a = t$it.a" }
        assertTrue(failure(joins).message().contains("levels deep"))
        val commas = "SELECT t0.a FROM " + (0..5000).joinToString(", ") { "T AS t$it" }
        assertTrue(failure(commas).message().contains("levels deep"))
    }

    @Test
    fun caseSensitiveNamesAreKeptApartInTheDistinctWrap() {
        val context = Recording()
        val frontend = RedshiftFrontend(context, caseSensitiveIdentifiers = true)
        val plan = planner.plan(frontend.toAst("SELECT DISTINCT b AS \"X\", c AS x FROM T ORDER BY x LIMIT 5"), session).plan
        val output = Scribe(context).compile(plan, session, PartiQLTarget).output.value
        assertTrue(output.contains("AS \"X\"") && output.contains("AS \"x\""), output)
        assertTrue(output.contains("ORDER BY \"\$distinct\"['x']"), output)
    }

    @Test
    fun astPlansAndCompilesWithoutErrors() {
        val context = Recording()
        val partiql = transpile("SELECT a FROM SIMPLE_T WHERE b > 0", context)
        assertTrue(partiql.startsWith("SELECT"), partiql)
        assertTrue(context.problems.none { it.severity.code() == Severity.ERROR }, context.problems.toString())
    }

    @Test
    fun syntaxErrorHasPosition() {
        val problem = failure("SELECT a FROM SIMPLE_T WHERE b >")
        assertEquals("UNSUPPORTED_OPERATION", problem.name())
        assertTrue(problem.message().startsWith("Syntax error"), problem.message())
        assertEquals(1 to 33, problem.position())
    }

    @Test
    fun unconvertedFunctionIsUnsupportedWithPosition() {
        val problem = failure("SELECT a FROM SIMPLE_T WHERE UPPER(c) = 'X'")
        assertEquals("UNSUPPORTED_OPERATION", problem.name())
        assertEquals(1 to 30, problem.position())
        assertTrue(problem.message().contains("UPPER"), problem.message())
    }

    @Test
    fun queryRedshiftRejectsIsUnsupportedWithPosition() {
        val problem = failure("SELECT a FROM SIMPLE_T ORDER BY 2")
        assertEquals("UNSUPPORTED_OPERATION", problem.name())
        assertTrue(problem.message().startsWith("Invalid Redshift query"), problem.message())
        assertEquals(1 to 33, problem.position())
    }

    @Test
    fun rewritesAreReportedAsNotes() {
        val context = Recording()
        transpile("SELECT DISTINCT c FROM SIMPLE_T ORDER BY c LIMIT 5", context)
        val note = context.problems.single()
        assertEquals("TRANSLATION_INFO", note.name())
        assertEquals(Severity.INFO, note.severity.code())
        assertTrue(note.message().contains("DISTINCT"), note.message())
    }

    @Test
    fun conversionIsDeterministic() {
        val sql = "SELECT DISTINCT c, COUNT(*) AS n FROM SIMPLE_T GROUP BY c ORDER BY n DESC LIMIT 3"
        val first = Recording()
        val second = Recording()
        assertEquals(transpile(sql, first), transpile(sql, second))
        assertEquals(first.problems.map { it.toString() }, second.problems.map { it.toString() })
    }

    @Test
    fun otherTargetsTakeTheSamePlan() {
        val plan = planner.plan(frontend().toAst("SELECT a, b FROM SIMPLE_T WHERE c ILIKE 'x%'"), session).plan
        val redshift = Scribe(ScribeContext.standard()).compile(plan, session, RedshiftTarget.STANDARD).output.value
        assertTrue(redshift.contains("lower"), redshift)
    }
}
