/*
 * Regression tests for three converter crashes:
 *   - removePathRoot on an empty-steps ExprPath            (NoSuchElementException)
 *   - removePathRoot on a non-string literal bracket key   (UnsupportedOperationException)
 *   - Locals.getExprOrNull with an out-of-range CTE offset (IndexOutOfBoundsException)
 *
 * Each test exercises the exact input shape that threw before the guard was added,
 * and asserts the function now returns a sensible value instead of throwing.
 */
package org.partiql.scribe.sql

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.scribe.sql.utils.removePathRoot
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConverterCrashGuardsTest {
    // An ExprPath with no steps must be returned unchanged, not crash on steps.first().
    @Test
    fun `removePathRoot returns an empty-steps path unchanged`() {
        val root = exprVarRef(Identifier.delimited("T"), isQualified = false)
        val emptyPath: ExprPath = exprPath(root, emptyList())

        val result: Expr = removePathRoot(emptyPath)

        assertEquals(emptyPath, result, "a root-only path should be returned as-is")
    }

    // A bracket key that is a non-string literal (e.g. T[1]) must not reach stringValue(),
    // which throws UnsupportedOperationException on a non-string literal.
    @Test
    fun `removePathRoot does not call stringValue on a non-string bracket key`() {
        val root = exprVarRef(Identifier.delimited("T"), isQualified = false)
        val intKey = exprLit(Literal.intNum(1)) // T[1]
        val path: ExprPath = exprPath(root, listOf(exprPathStepElement(intKey)))

        // Must not throw; the non-string key falls through to the return-as-is branch.
        val result: Expr = removePathRoot(path)

        assertTrue(result is ExprPath, "a non-string bracket key should be left unchanged")
        assertEquals(path, result)
    }

    // A CTE-present scope must resolve offsets exclusively against its CTE list. An offset past
    // the CTE list must return null, not fall through to env and resolve an unrelated field that
    // happens to sit at the same positional offset. The populated env makes that fall-through
    // observable: pre-guard, offset 1 missed the single-element CTE list and resolved env[1].
    @Test
    fun `getExprOrNull does not fall through to env when a cte offset misses`() {
        val locals =
            Locals(
                env =
                    listOf(
                        PTypeField.of("env_field_0", PType.integer()),
                        PTypeField.of("env_field_1", PType.integer()),
                    ),
                ctes = listOf("cte0"),
            )

        // offset 1 is out of range for the single-element ctes list but in range for env.
        val result = locals.getExprOrNull(offset = 1)

        assertNull(result, "a CTE miss must return null, not resolve an unrelated env field")
    }

    // An out-of-range CTE offset must honor the OrNull contract (return null), not throw.
    @Test
    fun `getExprOrNull returns null for an out-of-range cte offset`() {
        val locals =
            Locals(
                env = emptyList(),
                ctes = listOf("cte0"),
            )

        // offset 5 is out of range for a single-element ctes list.
        val result = locals.getExprOrNull(offset = 5)

        assertNull(result, "an out-of-range CTE offset should return null, not throw")
    }
}
