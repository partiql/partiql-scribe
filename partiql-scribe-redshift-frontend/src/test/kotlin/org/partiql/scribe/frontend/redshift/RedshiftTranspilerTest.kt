package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.problems.Severity
import org.partiql.scribe.targets.duckdb.DuckDBTarget
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.targets.spark.SparkTarget
import org.partiql.scribe.targets.trino.TrinoTarget
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.errors.PError
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The public contract of [RedshiftFrontend]: the table listing, the problem codes and their positions, the notes, and
 * determinism, and that the AST plans and compiles with the standard PartiQL planner and the Scribe targets. The
 * translation itself is covered by [RedshiftTranspilerSuite].
 */
class RedshiftTranspilerTest {
    private val session = RedshiftTestSession.withPow()
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
        val ex = assertThrows<ScribeException> { RedshiftFrontend(context).tables("SELECT GETDATE() FROM T") }
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
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, failure("SELECT a FROM SIMPLE_T WHERE GETDATE() IS NULL").kind())
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

    /** The size refusal of a call of [name], or of the statement when [name] is null, as [KIND_UNSUPPORTED]. */
    private fun tooLarge(
        sql: String,
        name: String?,
    ) {
        val problem = failure(sql)
        assertEquals(RedshiftFrontend.KIND_UNSUPPORTED, problem.kind(), problem.message())
        val expected =
            if (name == null) {
                "the query is too large to translate (more than 100,000 nodes once its functions and common table expressions are repeated)"
            } else {
                "$name makes the query too large to translate (more than 100,000 nodes once its arguments are repeated)"
            }
        assertTrue(problem.message().endsWith(expected), problem.message())
    }

    private fun bs(n: Int) = (1..n).joinToString(", ") { "b" }

    @Test
    fun repeatingRewritesAreBoundedBySize() {
        // GREATEST compares every pair of arguments, and a nested DECODE or GREATEST repeats its expression at each
        // level. The numbers are the measured edges of the 100,000-node bound, so a change to a rewrite or the bound shows.
        fun decode(depth: Int) = "SELECT " + "DECODE(".repeat(depth) + "b" + ", b, 1, 0)".repeat(depth) + " FROM SIMPLE_T"

        fun greatest(depth: Int) = "SELECT " + "GREATEST(".repeat(depth) + "b" + ", b, 1)".repeat(depth) + " FROM SIMPLE_T"
        assertTrue(transpile("SELECT GREATEST(${bs(129)}) FROM SIMPLE_T").contains("CASE WHEN"))
        tooLarge("SELECT GREATEST(${bs(130)}) FROM SIMPLE_T", "GREATEST")
        assertTrue(transpile(decode(12)).contains("CASE WHEN"))
        tooLarge(decode(13), "DECODE")
        assertTrue(transpile(greatest(6)).contains("CASE WHEN"))
        tooLarge(greatest(7), "GREATEST")
        // A rewrite nested in another counts once: the inner GREATEST is part of the outer one's result.
        assertTrue(transpile("SELECT GREATEST(GREATEST(${bs(80)}), 1) FROM SIMPLE_T").contains("CASE WHEN"))
        // A long literal weighs its length: 10,000 characters repeated 158 times is refused, 157 times is not.
        val long = "'" + "x".repeat(10_000) + "'"
        assertTrue(transpile("SELECT DECODE($long, " + (1..157).joinToString(", ") { "'$it', $it" } + ") FROM SIMPLE_T").contains("CASE"))
        tooLarge("SELECT DECODE($long, " + (1..158).joinToString(", ") { "'$it', $it" } + ") FROM SIMPLE_T", "DECODE")
        // DECODE's size is linear in its pairs.
        assertTrue(transpile("SELECT DECODE(b, " + (1..2000).joinToString(", ") { "$it, 'v$it'" } + ") FROM SIMPLE_T").contains("WHEN"))
    }

    @Test
    fun theSizeBoundIsOverTheStatement() {
        // Two calls each within the bound are refused together, at the second.
        tooLarge("SELECT GREATEST(${bs(120)}) AS v1, GREATEST(${bs(120)}) AS v2 FROM SIMPLE_T", "GREATEST")
        // An alias that ORDER BY repeats doubles the rewrite; the finished statement is measured.
        assertTrue(transpile("SELECT GREATEST(${bs(91)}) AS v FROM SIMPLE_T ORDER BY v").contains("CASE WHEN"))
        tooLarge("SELECT GREATEST(${bs(92)}) AS v FROM SIMPLE_T ORDER BY v", null)

        // A common table expression's body counts at each reference, since the planner inlines it there.
        fun cte(
            n: Int,
            references: Int,
        ): String {
            val from = (1..references).joinToString(", ") { "c AS c$it" }
            return "WITH c AS (SELECT GREATEST(${bs(n)}) AS v FROM SIMPLE_T) SELECT c1.v FROM $from"
        }
        assertTrue(transpile(cte(120, 1)).contains("CASE WHEN"))
        tooLarge(cte(120, 2), null)
        assertTrue(transpile(cte(60, 4)).contains("CASE WHEN"))
        tooLarge(cte(60, 5), null)
    }

    @Test
    fun aStatementThatRepeatsNothingIsNotMeasured() {
        // No rewrite repeats anything here, and a CTE referenced many times has a body that is not a rewrite: the
        // AST is the size of the text, which the depth bound already keeps walkable.
        assertTrue(transpile("SELECT b FROM SIMPLE_T WHERE b IN (" + (1..50_000).joinToString(", ") + ")").contains("IN"))
        assertTrue(transpile("SELECT " + (1..30_000).joinToString(", ") { "b AS x$it" } + " FROM SIMPLE_T").contains("x30000"))
        assertTrue(transpile("SELECT '" + "x".repeat(2_000_000) + "' FROM SIMPLE_T").length > 2_000_000)
        val fifty = "WITH c AS (SELECT b FROM SIMPLE_T) SELECT c1.b FROM " + (1..50).joinToString(", ") { "c AS c$it" }
        assertTrue(transpile(fifty).contains("c50"))
    }

    @Test
    fun longIdentifiersAreInvalid() {
        assertTrue(transpile("SELECT b AS " + "x".repeat(127) + " FROM SIMPLE_T").contains("x".repeat(127)))
        // 128 one-byte characters as an alias and as a table name, and 64 two-byte characters quoted: bytes, not characters.
        val long =
            listOf(
                "SELECT b AS ${"x".repeat(128)} FROM SIMPLE_T",
                "SELECT b FROM ${"x".repeat(128)}",
                "SELECT b AS \"${"é".repeat(64)}\" FROM SIMPLE_T",
            )
        for (sql in long) {
            val problem = failure(sql)
            assertEquals(RedshiftFrontend.KIND_INVALID, problem.kind(), problem.message())
            assertTrue(problem.message().endsWith("identifier longer than 127 bytes"), problem.message())
        }
    }

    @Test
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun deeplyNestedCaseCompilesOnEveryTarget() {
        // Each NVL2 is a CASE inside the IS NOT NULL operand of the next, which the planner casts to DYNAMIC. The
        // Redshift target used to convert such an operand twice per level, which took time exponential in the depth;
        // the timeout, on its own thread so that it can stop the test, fails fast if that comes back. The root
        // fixture `case-nested-00` covers the same shape on every target.
        val depth = RedshiftFrontend.MAX_NESTING
        val sql = "SELECT " + "NVL2(".repeat(depth) + "b" + ", b, 0)".repeat(depth) + " FROM SIMPLE_T"
        val plan = planner.plan(frontend().toAst(sql), session).plan
        val targets = listOf(PartiQLTarget, RedshiftTarget.STANDARD, TrinoTarget.STANDARD, SparkTarget.STANDARD, DuckDBTarget.STANDARD)
        for (target in targets) {
            val output = Scribe(ScribeContext.standard()).compile(plan, session, target).output.value
            assertEquals(depth, output.windowed(9).count { it == "CASE WHEN" }, target.toString())
        }
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
        val problem = failure("SELECT a FROM SIMPLE_T WHERE GETDATE() IS NULL")
        assertEquals("UNSUPPORTED_OPERATION", problem.name())
        assertEquals(1 to 30, problem.position())
        assertTrue(problem.message().contains("GETDATE"), problem.message())
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

    @Test
    fun powerIsACallOfPowForTheCatalogToDefine() {
        // POWER, POW, and `^` are written as `pow(a, b)`, which PartiQL's built-in functions do not define: against
        // the test catalogs as they are, the planner reports the function as not found, and nothing else.
        val ast = frontend().toAst("SELECT POWER(b, 2), POW(b, 2), b ^ 2 FROM SIMPLE_T")
        val problems = PErrorCollector()
        planner.plan(ast, RedshiftTestSession.withoutPow(), Context.of(problems))
        assertEquals(listOf(PError.FUNCTION_NOT_FOUND), problems.errors.map { it.code() }.distinct(), problems.errors.toString())
        assertEquals(3, problems.errors.size, problems.errors.toString())
        // Against a catalog that defines `pow` over two DOUBLE PRECISION values, the call resolves, the planner casts the
        // arguments as Redshift's POWER converts them, and every target writes the call.
        val plan = planner.plan(ast, session).plan
        val targets = listOf(PartiQLTarget, RedshiftTarget.STANDARD, TrinoTarget.STANDARD, SparkTarget.STANDARD, DuckDBTarget.STANDARD)
        for (target in targets) {
            val output = Scribe(ScribeContext.standard()).compile(plan, session, target).output.value
            assertEquals(3, Regex("pow").findAll(output).count(), "$target: $output")
            assertTrue(output.contains("AS DOUBLE"), "$target: $output")
        }
    }
}
