package org.partiql.scribe.sql.utils

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BackslashUtilsTest {
    @Test
    fun likePatternEscapesAStringLiteral() {
        val pattern = escapeLikePattern(exprLit(Literal.string("a\\_%"))) as ExprLit
        assertEquals("a\\\\_%", pattern.lit.stringValue())
    }

    @Test
    fun likePatternLeavesNullAndMissingLiterals() {
        // No target can print a MISSING pattern as valid SQL, so these are checked here rather than in a fixture.
        for (lit in listOf(Literal.nul(), Literal.missing())) {
            val pattern = exprLit(lit)
            assertSame(pattern, escapeLikePattern(pattern))
        }
    }

    @Test
    fun likePatternWrapsAnyOtherExpressionInReplace() {
        val call = escapeLikePattern(exprVarRef(Identifier.regular("p"), false)) as ExprCall
        assertEquals("REPLACE", call.function.identifier.text)
        assertTrue(call.args.size == 3)
    }

    @Test
    fun stringLiteralDoublesBackslashes() {
        val lit = escapeStringLiteral(exprLit(Literal.string("C:\\temp")))
        assertEquals("C:\\\\temp", lit.lit.stringValue())
    }
}
