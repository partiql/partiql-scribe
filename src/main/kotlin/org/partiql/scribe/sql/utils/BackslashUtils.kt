package org.partiql.scribe.sql.utils

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprLit

// Helpers for targets that read a backslash as an escape character, both inside string literals and as the default
// `LIKE` escape character (Spark, Redshift). PartiQL has neither: `'\n'` is two characters, and `LIKE` without `ESCAPE`
// has no escape character. Escaping backslashes makes the target read the same value and pattern as PartiQL.

/**
 * Returns [node] with each backslash in a string literal doubled, e.g. PartiQL `'C:\temp'` -> `'C:\\temp'`.
 */
internal fun escapeBackslashes(node: ExprLit): ExprLit {
    val lit = node.lit
    return when (lit.code()) {
        Literal.STRING -> exprLit(Literal.string(escapeBackslashes(lit.stringValue())))
        Literal.TYPED_STRING -> exprLit(Literal.typedString(lit.dataType(), escapeBackslashes(lit.stringValue())))
        else -> node
    }
}

/**
 * Returns [node] with each backslash in the pattern escaped when there is no `ESCAPE` clause, so that the target's
 * default backslash escape matches a literal backslash. A string literal pattern is escaped directly; any other pattern
 * is wrapped in `REPLACE(pattern, '\', '\\')`. String literals are escaped again by [escapeBackslashes] when printed.
 */
internal fun escapeBackslashes(node: ExprLike): ExprLike {
    if (node.escape != null) {
        return node
    }
    val pattern = node.pattern
    val escapedPattern =
        if (pattern is ExprLit && pattern.lit.code() == Literal.STRING) {
            exprLit(Literal.string(escapeBackslashes(pattern.lit.stringValue())))
        } else {
            exprCall(
                Identifier.regular("REPLACE"),
                listOf(pattern, exprLit(Literal.string("\\")), exprLit(Literal.string("\\\\"))),
            )
        }
    return exprLike(node.value, escapedPattern, null, node.isNot)
}

private fun escapeBackslashes(value: String): String = value.replace("\\", "\\\\")
