package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.ScribeTest
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reverse round trip over the Scribe fixtures.
 *
 * For each test, Scribe translates the PartiQL input in `inputs/` to the Redshift text in `outputs/redshift/`, and to
 * the normalized PartiQL text in `outputs/partiql/`. This report translates the Redshift text back to PartiQL and
 * compares the result with the normalized PartiQL text of the same test.
 *
 * The Scribe Redshift target quotes every identifier and keeps its case, so the report translates with
 * `caseSensitiveIdentifiers = true` (the Redshift `enable_case_sensitive_identifier` setting).
 *
 * A mismatch is not always a defect: the forward translation can add casts or rewrite expressions, so the round trip
 * is not exact. The report fails only on an internal error, or when the number of matches falls below [MIN_MATCHES].
 * Increase [MIN_MATCHES] when a change adds matches.
 */
class RoundTripReport {
    private val redshift = load("/outputs/redshift")
    private val partiql = load("/outputs/partiql")
    private val session = SessionProvider(scalarOverloads = mapOf("my_udf" to Functions.scalarUdf)).getSession()

    @Test
    fun report() {
        val transpiler = RedshiftTranspiler(caseSensitiveIdentifiers = true)
        val outcomes = mutableMapOf<String, MutableList<String>>()
        val mismatches = mutableListOf<String>()
        var matches = 0
        var total = 0
        for (test in redshift.iterator().asSequence().sortedBy { it.key.toString() }) {
            val redshiftText = body(test)
            val expected = partiql[test.key]?.let(::body)
            if (isExpectedError(redshiftText) || expected == null || isExpectedError(expected)) continue
            total++
            val result = transpiler.transpile(redshiftText, session)
            val outcome =
                when (result.status) {
                    RedshiftTranspileResult.Status.VALID ->
                        if (normalize(result.partiql!!) == normalize(expected)) {
                            matches++
                            "MATCH"
                        } else {
                            mismatches.add("${test.key}\n  expected: ${normalize(expected)}\n  actual:   ${normalize(result.partiql)}")
                            "VALID, DIFFERENT TEXT"
                        }
                    else -> "${result.status}: ${bucket(result.problems.first { it.code.isError })}"
                }
            outcomes.getOrPut(outcome) { mutableListOf() }.add(test.key.toString())
        }

        println("Round trip over $total Scribe fixtures: $matches match")
        outcomes.entries.sortedByDescending { it.value.size }.forEach { (outcome, keys) ->
            println(String.format("%5d  %s  (e.g. %s)", keys.size, outcome, keys.first()))
        }

        mismatches.take(MISMATCH_SAMPLES).forEach(::println)

        val internal = outcomes.filterKeys { it.startsWith("INTERNAL_ERROR") }
        assertTrue(internal.isEmpty(), "Internal errors: $internal")
        assertTrue(matches >= MIN_MATCHES, "Matches fell from $MIN_MATCHES to $matches")
        assertEquals(total, outcomes.values.sumOf { it.size })
    }

    /**
     * Groups problems by construct, without locations and literal values, so that the report shows the most common
     * gaps first.
     */
    private fun bucket(problem: RedshiftProblem): String =
        when (problem.code) {
            RedshiftProblem.Code.SYNTAX_ERROR -> "syntax: " + problem.message.substringBefore(" expecting")
            RedshiftProblem.Code.SEMANTIC_ERROR -> "semantic: " + Regex("code=(\\w+)").find(problem.message)?.groupValues?.get(1)
            else -> problem.message.removePrefix("Unsupported Redshift construct: ").substringBefore(";").take(80)
        }

    private fun body(test: ScribeTest): String =
        test.statement.lines().filterNot { it.trimStart().startsWith("--") }.joinToString("\n").trim()

    private fun isExpectedError(text: String): Boolean = text.startsWith("[")

    private fun normalize(sql: String): String = sql.trim().removeSuffix(";").replace(Regex("\\s+"), " ")

    private fun load(path: String) = ScribeTestProvider().apply { load(this::class.java.getResource(path)!!.toURI().toPath()) }

    private companion object {
        const val MIN_MATCHES = 449
        const val MISMATCH_SAMPLES = 15
    }
}
