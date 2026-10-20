package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import org.partiql.scribe.utils.SqlEqualsNaive
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs each Redshift input in `redshift-frontend/inputs` and compares the result with the test of the same name in
 * `redshift-frontend/outputs`.
 *
 * An expected output is either PartiQL text, or a status in brackets for a failed translation, optionally with a
 * substring of the first problem message: `[UNSUPPORTED{message="QUALIFY"}];`
 */
class RedshiftTranspilerSuite {
    private val inputs = load("/redshift-frontend/inputs")
    private val outputs = load("/redshift-frontend/outputs")
    private val session = SessionProvider().getSession()
    private val transpiler = RedshiftTranspiler()

    @TestFactory
    fun suite(): List<DynamicNode> =
        outputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { expected ->
                        dynamicTest(expected.key.name) {
                            val input = inputs[expected.key] ?: fail("No input for ${expected.key}")
                            check(input.statement, expected.statement)
                        }
                    },
                )
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
    }
}
