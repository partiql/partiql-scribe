package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.partiql.scribe.frontend.redshift.internal.OperandTypes
import org.partiql.scribe.frontend.redshift.internal.RedshiftSyntaxException
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import org.partiql.scribe.utils.SqlEqualsNaive
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The conformance suite of the Redshift frontend. Each case is a Redshift input in `redshift-frontend/inputs` and the
 * expected result of the same name in `redshift-frontend/outputs`.
 *
 * An expected output is either PartiQL text, or a status in brackets for a failed translation, optionally with a
 * substring of the first problem message: `[UNSUPPORTED{message="QUALIFY"}];`
 *
 * A case whose marker is `-- --#[name] TODO(redshift)` waits for the change that translates its construct; that change
 * enables it. Whatever its marker, every input is parsed by [grammar], so the grammar covers the whole suite.
 */
class RedshiftTranspilerSuite {
    private val inputs = load("/redshift-frontend/inputs")
    private val outputs = load("/redshift-frontend/outputs")
    private val session = SessionProvider().getSession()
    private val transpiler = RedshiftTranspiler()

    /** Every input parses, except an input whose expected output is the syntax error. */
    @TestFactory
    fun grammar(): List<DynamicNode> =
        inputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { input ->
                        dynamicTest("$group/${input.key.name}") {
                            val expectedFailure = outputs[input.key]?.let { FAILURE.matchEntire(body(it.statement)) }
                            try {
                                transpiler.tables(input.statement)
                            } catch (ex: RedshiftSyntaxException) {
                                val (status, message) = expectedFailure?.destructured ?: fail("Input: ${input.statement}\n$ex")
                                assertEquals(RedshiftTranspileResult.Status.INVALID_INPUT.name, status, "Input: ${input.statement}\n$ex")
                                assertTrue(ex.message.contains(message), "Expected message to contain '$message'; got: $ex")
                            }
                        }
                    },
                )
            }

    @TestFactory
    fun suite(): List<DynamicNode> =
        outputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { expected ->
                        // The group is in the name too: a test report does not show the container of a dynamic test.
                        dynamicTest("$group/${expected.key.name}") {
                            val input = inputs[expected.key] ?: fail("No input for ${expected.key}")
                            check(input.statement, expected.statement)
                        }
                    },
                )
            }

    /** The test provider keeps one test for each group and name, so a repeated name would hide a test. */
    @Test
    fun testNamesAreUniqueInEachGroup() {
        for (dir in listOf("inputs", "outputs")) {
            val root = this::class.java.getResource("/redshift-frontend/$dir")!!.toURI().toPath().toFile()
            for (group in root.listFiles { f -> f.isDirectory }!!) {
                val names =
                    group.listFiles { f -> f.extension == "sql" }!!.flatMap { file ->
                        file.readLines().mapNotNull { NAME.matchEntire(it)?.groupValues?.get(1) }
                    }
                val repeated = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                assertTrue(repeated.isEmpty(), "Repeated test names in $dir/${group.name}: $repeated")
            }
        }
    }

    /**
     * The probe passes that the fixtures need. The typed rules of the Redshift catalog resolve the operand types in one
     * pass; see [org.partiql.scribe.frontend.redshift.internal.OperandTypes]. The histogram is printed for diagnostics.
     */
    @Test
    fun probePasses() {
        val passes = sortedMapOf<Int, Int>()
        var most: Pair<Int, String> = 0 to ""
        for ((key, input) in inputs.iterator().asSequence().map { it.key to it.statement }) {
            transpiler.transpile(input, session)
            val n = transpiler.lastProbePasses
            passes.merge(n, 1, Int::plus)
            if (n > most.first) most = n to "${key.group}/${key.name}"
        }
        println("Probe passes: $passes; most: ${most.second} (${most.first})")
        assertTrue(most.first <= OperandTypes.MAX_PASSES, "A fixture needs more than ${OperandTypes.MAX_PASSES} probe passes: $most")
    }

    private fun check(
        input: String,
        expectedText: String,
    ) {
        val result = transpiler.transpile(input, session)
        val expected = body(expectedText)
        val expectedFailure = FAILURE.matchEntire(expected)
        if (expectedFailure == null) {
            assertEquals(RedshiftTranspileResult.Status.VALID, result.status, "Input: $input\nResult: $result")
            SqlEqualsNaive.assertEquals(expected, result.partiql!!) {
                appendLine("Input: $input")
                appendLine("Problems: ${result.problems}")
            }
        } else {
            val (status, message) = expectedFailure.destructured
            assertEquals(status, result.status.name, "Input: $input\nResult: $result")
            if (message.isNotEmpty()) {
                val first = result.problems.first { it.code.isError }
                assertTrue(first.message.contains(message), "Expected message to contain '$message'; got: $first")
            }
        }
    }

    private fun body(statement: String): String = statement.lines().filterNot { it.startsWith("--#[") }.joinToString("\n").trim()

    private fun load(path: String) = ScribeTestProvider().apply { load(this::class.java.getResource(path)!!.toURI().toPath()) }

    private companion object {
        val FAILURE = Regex("""\[(\w+)(?:\{message="(.*)"})?];""")
        val NAME = Regex("""--#\[(.+)]""")
    }
}
