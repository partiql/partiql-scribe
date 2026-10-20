package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.scribe.utils.ScribeTest
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import org.partiql.spi.Context
import java.io.File
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Round trip over the Scribe fixtures.
 *
 * For each test, Scribe translates the PartiQL input in `inputs/` to the Redshift text in `outputs/redshift/`, and to
 * the normalized PartiQL text in `outputs/partiql/`. This suite translates the Redshift text back to PartiQL.
 *
 * Each fixture is in one of two sets in `redshift-frontend/roundtrip/`:
 *  - `happy.txt`: the translation is VALID, and it passes the oracle named on its line:
 *    - `PARTIQL`: the output is equal to the Scribe PartiQL fixture.
 *    - `REDSHIFT`: Scribe translates the output to the same Redshift text again. Use this oracle when the Scribe
 *      Redshift target adds casts or rewrites, so that the PartiQL text cannot be equal.
 *  - `unhappy.txt`: the translation does not round-trip; the line gives the expected status and reason. For
 *    `MISMATCH`, the translation is VALID but neither oracle passes, and the reason is the expected output.
 *
 * The Scribe Redshift target quotes every identifier and keeps its case, so the suite translates with
 * `caseSensitiveIdentifiers = true` (the Redshift `enable_case_sensitive_identifier` setting).
 *
 * To classify the fixtures again after a change, run the tests with `-PupdateRoundTrip`, then review the change to
 * both files.
 */
class RoundTripSuite {
    private val redshift = load("/outputs/redshift")
    private val partiql = load("/outputs/partiql")
    private val session = SessionProvider(scalarOverloads = mapOf("my_udf" to Functions.scalarUdf)).getSession()
    private val transpiler = RedshiftTranspiler(caseSensitiveIdentifiers = true)

    /** The fixtures that have both a Redshift text and a PartiQL text; Scribe error expectations are not included. */
    private val fixtures: Map<String, Pair<String, String>> =
        redshift.iterator().asSequence()
            .mapNotNull { test ->
                val redshiftText = body(test)
                val partiqlText = partiql[test.key]?.let(::body)
                if (redshiftText.startsWith("[") || partiqlText == null || partiqlText.startsWith("[")) {
                    null
                } else {
                    test.key.toString() to (redshiftText to partiqlText)
                }
            }
            .toMap()
            .toSortedMap()

    @Test
    fun everyFixtureIsInOneSet() {
        val happy = readHappy().keys
        val unhappy = readUnhappy().keys
        assertTrue((happy intersect unhappy).isEmpty(), "In both sets: ${happy intersect unhappy}")
        val missing = fixtures.keys - happy - unhappy
        val unknown = (happy + unhappy) - fixtures.keys
        assertTrue(missing.isEmpty(), "Not in a set; run with -PupdateRoundTrip: $missing")
        assertTrue(unknown.isEmpty(), "Not a Scribe fixture: $unknown")
    }

    @TestFactory
    fun happy(): List<DynamicNode> =
        readHappy().map { (key, oracle) ->
            dynamicTest(key) {
                val (redshiftText, partiqlText) = fixtures[key] ?: fail("No fixture $key")
                val outcome = classify(redshiftText, partiqlText)
                assertEquals("HAPPY\t$oracle", outcome, "Input: $redshiftText")
            }
        }.let { listOf(dynamicContainer("happy", it)) }

    @TestFactory
    fun unhappy(): List<DynamicNode> =
        readUnhappy().map { (key, expected) ->
            dynamicTest(key) {
                val (redshiftText, partiqlText) = fixtures[key] ?: fail("No fixture $key")
                assertEquals(expected, classify(redshiftText, partiqlText), "Input: $redshiftText")
            }
        }.let { listOf(dynamicContainer("unhappy", it)) }

    /**
     * Returns `HAPPY<tab>oracle`, or `status<tab>reason` for a fixture that does not round-trip.
     */
    private fun classify(
        redshiftText: String,
        partiqlText: String,
    ): String {
        val result = transpiler.transpile(redshiftText, session)
        if (result.status != RedshiftTranspileResult.Status.VALID) {
            return "${result.status}\t${reason(result.problems.first { it.code.isError })}"
        }
        val output = result.partiql!!
        return when {
            normalize(output) == normalize(partiqlText) -> "HAPPY\tPARTIQL"
            toRedshift(output)?.let(::normalize) == normalize(redshiftText) -> "HAPPY\tREDSHIFT"
            // Neither oracle passes; the reason is the output, so that a change to it fails the test.
            else -> "MISMATCH\t${normalize(output)}"
        }
    }

    /**
     * Translates PartiQL text to Redshift with the Scribe Redshift target, or returns null if Scribe cannot.
     */
    private fun toRedshift(partiqlText: String): String? {
        val errors = PErrorCollector()
        val context = Context.of(errors)
        val statement = PartiQLParser.standard().parse(partiqlText, context).statements.singleOrNull() ?: return null
        val plan = PartiQLPlanner.standard().plan(statement, session, context).plan
        if (errors.errors.isNotEmpty()) return null
        return try {
            Scribe(ScribeContext.standard()).compile(plan, session, RedshiftTarget.STANDARD).output.value
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * The reason without locations and literal values, so that it does not change when unrelated text changes.
     */
    private fun reason(problem: RedshiftProblem): String =
        when (problem.code) {
            RedshiftProblem.Code.SYNTAX_ERROR -> "syntax: " + problem.message.substringBefore(" expecting")
            RedshiftProblem.Code.SEMANTIC_ERROR -> "semantic: " + Regex("code=(\\w+)").find(problem.message)?.groupValues?.get(1)
            else -> problem.message.removePrefix("Unsupported Redshift construct: ").substringBefore(";").take(100)
        }.replace(Regex("\\s+"), " ")

    private fun update() {
        val happy = StringBuilder(HEADER_HAPPY)
        val unhappy = StringBuilder(HEADER_UNHAPPY)
        for ((key, texts) in fixtures) {
            val outcome = classify(texts.first, texts.second)
            if (outcome.startsWith("HAPPY\t")) {
                happy.appendLine("$key\t${outcome.removePrefix("HAPPY\t")}")
            } else {
                unhappy.appendLine("$key\t$outcome")
            }
        }
        File(UPDATE_DIR, "happy.txt").writeText(happy.toString())
        File(UPDATE_DIR, "unhappy.txt").writeText(unhappy.toString())
    }

    /** In update mode, writes both sets once, before any test reads them. */
    private val updated: Unit by lazy { if (UPDATE) update() }

    private fun readHappy(): Map<String, String> = read("happy.txt").associate { it[0] to it[1] }

    private fun readUnhappy(): Map<String, String> = read("unhappy.txt").associate { it[0] to "${it[1]}\t${it[2]}" }

    private fun read(name: String): List<List<String>> {
        updated
        val file =
            if (UPDATE) {
                File(
                    UPDATE_DIR,
                    name,
                )
            } else {
                File(this::class.java.getResource("/redshift-frontend/roundtrip/$name")!!.toURI())
            }
        return file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split("\t") }
    }

    private fun body(test: ScribeTest): String =
        test.statement.lines().filterNot { it.trimStart().startsWith("--") }.joinToString("\n").trim()

    private fun normalize(sql: String): String = sql.trim().removeSuffix(";").replace(Regex("\\s+"), " ")

    private fun load(path: String) = ScribeTestProvider().apply { load(this::class.java.getResource(path)!!.toURI().toPath()) }

    private companion object {
        val UPDATE_DIR: String? = System.getProperty("redshift.roundtrip.update")
        val UPDATE = UPDATE_DIR != null

        const val HEADER_HAPPY =
            "# Scribe fixtures that round-trip. Columns: test key, oracle (PARTIQL or REDSHIFT). See RoundTripSuite.\n"
        const val HEADER_UNHAPPY =
            "# Scribe fixtures that do not round-trip. Columns: test key, expected status, reason. See RoundTripSuite.\n"
    }
}
