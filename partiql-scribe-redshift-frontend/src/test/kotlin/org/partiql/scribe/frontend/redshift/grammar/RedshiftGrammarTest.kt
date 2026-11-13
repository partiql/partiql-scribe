package org.partiql.scribe.frontend.redshift.grammar

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The grammar fixtures under `src/test/resources/grammar`.
 *
 * - `accept/` files: statements the grammar must parse without a syntax error. Each file covers one area of the grammar.
 * - `reject/` files: statements the grammar must refuse, each with the expected `line:column` (1-based) of the first
 *   syntax error, so that a user of the parser gets a precise location.
 *
 * These fixtures test the grammar only. Whether a parsed statement can be translated to PartiQL is decided by the
 * translation, which is tested by its own suite.
 *
 * The grammar is Scribe's own, not an official Amazon Redshift grammar; see the disclaimer at the top of
 * `ScribeRedshift.g4`. These fixtures describe what that grammar accepts, which is the subset the frontend translates.
 */
class RedshiftGrammarTest {
    @TestFactory
    fun accept(): List<DynamicNode> =
        Fixtures.load("accept").map { (file, fixtures) ->
            dynamicContainer(
                file,
                fixtures.map { fixture ->
                    dynamicTest(fixture.key) {
                        val result = parse(fixture.statement)
                        assertTrue(result.isValid, "Expected to parse:\n${fixture.statement}\nErrors: ${result.errors}")
                    }
                },
            )
        }

    @TestFactory
    fun reject(): List<DynamicNode> =
        Fixtures.load("reject").map { (file, fixtures) ->
            dynamicContainer(
                file,
                fixtures.map { fixture ->
                    dynamicTest(fixture.key) {
                        val result = parse(fixture.statement)
                        assertTrue(result.errors.isNotEmpty(), "Expected a syntax error:\n${fixture.statement}")
                        val expected = checkNotNull(fixture.position) { "Reject fixture ${fixture.key} needs an expected line:column" }
                        val first = result.errors.first()
                        assertEquals(
                            expected,
                            first.line to first.column,
                            "Expected the first syntax error at ${expected.first}:${expected.second}:\n" +
                                "${fixture.statement}\nErrors: ${result.errors}",
                        )
                    }
                },
            )
        }
}
