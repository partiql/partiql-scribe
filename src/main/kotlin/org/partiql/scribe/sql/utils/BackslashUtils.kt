package org.partiql.scribe.sql.utils

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit

// Helpers for targets that read a backslash as an escape character, both inside string literals and as the default
// `LIKE` escape character (Spark, Redshift). PartiQL has neither: `'\n'` is two characters, and `LIKE` without `ESCAPE`
// has no escape character. Escaping happens in two passes:
//  1. [escapeLikePattern] runs in the target's `like` rule and escapes the pattern for LIKE's default escape.
//  2. [escapeStringLiteral] runs in the target's printer and escapes every string literal for the SQL lexer.

/**
 * Returns [node] with each backslash in a string literal doubled, e.g. PartiQL `'C:\temp'` -> `'C:\\temp'`.
 */
internal fun escapeStringLiteral(node: ExprLit): ExprLit {
    val lit = node.lit
    return when (lit.code()) {
        Literal.STRING -> exprLit(Literal.string(doubleBackslashes(lit.stringValue())))
        // Scribe only builds typed strings for datetime values, which have no backslash; escaped in case that changes.
        Literal.TYPED_STRING -> exprLit(Literal.typedString(lit.dataType(), doubleBackslashes(lit.stringValue())))
        else -> node
    }
}

/**
 * Returns the `LIKE` [pattern] with each backslash escaped, for a `LIKE` with no `ESCAPE` clause, so that the target's
 * default backslash escape matches a literal backslash. A string literal is escaped directly, a NULL or MISSING literal
 * is returned as-is, and any other pattern is wrapped in `REPLACE(pattern, '\', '\\')`. The string literals in the
 * result hold the plain value; [escapeStringLiteral] escapes them again when printed.
 */
internal fun escapeLikePattern(pattern: Expr): Expr {
    if (pattern is ExprLit) {
        return when (pattern.lit.code()) {
            Literal.STRING -> exprLit(Literal.string(doubleBackslashes(pattern.lit.stringValue())))
            Literal.NULL, Literal.MISSING -> pattern
            else -> replaceBackslashes(pattern)
        }
    }
    return replaceBackslashes(pattern)
}

private fun replaceBackslashes(pattern: Expr): Expr =
    exprCall(
        Identifier.regular("REPLACE"),
        listOf(pattern, exprLit(Literal.string("\\")), exprLit(Literal.string("\\\\"))),
    )

private fun doubleBackslashes(value: String): String = value.replace("\\", "\\\\")
