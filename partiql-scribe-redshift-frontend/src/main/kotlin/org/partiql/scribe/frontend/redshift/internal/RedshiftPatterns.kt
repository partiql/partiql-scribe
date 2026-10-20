package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprPosition
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.spi.types.PType
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates the Redshift pattern-matching conditions: LIKE, ILIKE, SIMILAR TO, the LIKE operators `~~`, `~~*`, `!~~`,
 * `!~~*`, and the POSIX operators `~`, `!~`, `~*`, `!~*`; see [RedshiftExpressions].
 *
 * The default escape character of Redshift LIKE is a backslash, and PartiQL LIKE has no default escape character, so
 * the translation decodes a literal pattern with the Redshift rules and writes it again for PartiQL, with
 * `ESCAPE '\'` when a literal `%`, `_`, or backslash needs it.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/pattern-matching-conditions.html
 */
internal class RedshiftPatterns(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val types: OperandTypes,
) {
    /** A pattern, decoded into its parts. */
    private sealed class Part {
        /** `%`: any sequence of characters. */
        object Any : Part()

        /** `_`: one character. */
        object One : Part()

        /** A character that matches itself. */
        class Char(val codePoint: Int) : Part()
    }

    /**
     * `x [NOT] LIKE p [ESCAPE e]` and `x [NOT] ILIKE p [ESCAPE e]`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_patternmatching_condition_like.html
     */
    fun like(ctx: P.PredicateLikeContext): Expr {
        val not = ctx.NOT() != null
        return if (ctx.op.type == P.ILIKE) {
            ilike(ctx.op, ctx.lhs, ctx.rhs, ctx.escape, not)
        } else {
            like(ctx.op, ctx.lhs, ctx.rhs, ctx.escape, not)
        }
    }

    /**
     * `x ~~ p` is `x LIKE p`, `x ~~* p` is `x ILIKE p`, and `!~~` and `!~~*` are their negations.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_patternmatching_condition_like.html
     */
    fun likeOperator(ctx: P.PredicateLikeOperatorContext): Expr {
        val op = ctx.op
        val not = op.type == P.NOT_TILDE_TILDE || op.type == P.NOT_TILDE_TILDE_ASTERISK
        val insensitive = op.type == P.TILDE_TILDE_ASTERISK || op.type == P.NOT_TILDE_TILDE_ASTERISK
        val name = (if (not) "NOT " else "") + (if (insensitive) "ILIKE" else "LIKE")
        builder.note(op, "Redshift `${op.text}` was replaced by PartiQL `$name`")
        return if (insensitive) ilike(op, ctx.lhs, ctx.rhs, null, not) else like(op, ctx.lhs, ctx.rhs, null, not)
    }

    private fun like(
        op: Token,
        lhs: P.ValueExprContext,
        rhs: P.ValueExprContext,
        escape: P.ValueExprContext?,
        not: Boolean,
    ): Expr {
        val value = Operand(lhs, exprs.valueExpr(lhs))
        val pattern = exprs.valueExpr(rhs)
        val escapeChar = escapeChar(op, escape)
        val literal = stringLiteral(pattern)
        val parts = literal?.let { parse(op, it, escapeChar) }
        return text(op, value) { x ->
            if (literal == null || parts == null) {
                // A pattern that is not a literal cannot be decoded at translation time.
                val c = escapeChar ?: '\\'.code
                builder.note(
                    op,
                    "PartiQL raises an error for the escape character before a character other than `%`, `_`, or the escape " +
                        "character in a LIKE pattern; Redshift matches that character",
                )
                exprLike(x, pattern, exprLit(Literal.string(String(Character.toChars(c)))), not)
            } else {
                encode(op, x, parts, not, changed = literal, escapeChar = escapeChar, explicitEscape = escape != null)
            }
        }
    }

    /**
     * ILIKE is case-insensitive for ASCII letters only, so the translation requires a literal pattern whose characters
     * are ASCII, folds its letters to lower case, and matches `LOWER(x)`. Java lower-cases two non-ASCII characters to
     * ASCII text, U+0130 and U+212A; they are replaced with U+00B7 before LOWER, so that they match only `_` and `%`.
     * The PartiQL planner does not find an aggregate call in the argument of LOWER.
     */
    private fun ilike(
        op: Token,
        lhs: P.ValueExprContext,
        rhs: P.ValueExprContext,
        escape: P.ValueExprContext?,
        not: Boolean,
    ): Expr {
        if (containsAggregate(lhs)) {
            unsupported(op, "`ILIKE` on an aggregate expression (PartiQL does not plan it as an argument of LOWER)")
        }
        val value = Operand(lhs, exprs.valueExpr(lhs))
        val pattern = exprs.valueExpr(rhs)
        val escapeChar = escapeChar(op, escape)
        val literal = stringLiteral(pattern) ?: unsupported(op, "ILIKE with a pattern that is not a literal")
        val parts = parse(op, literal, escapeChar)
        if (parts.any { it is Part.Char && it.codePoint > ASCII_MAX }) unsupported(op, "`ILIKE` with a non-ASCII pattern")
        val folded = parts.map { if (it is Part.Char && it.codePoint in 'A'.code..'Z'.code) Part.Char(it.codePoint + CASE_OFFSET) else it }
        return text(op, value) { x ->
            builder.note(op, "Redshift `ILIKE` was replaced by PartiQL `LIKE` on the lower-case value")
            val lower =
                exprCall(
                    Identifier.regular("lower"),
                    listOf(replace(replace(x, DOTTED_CAPITAL_I, NEUTRAL), KELVIN_SIGN, NEUTRAL)),
                    null,
                )
            encode(op, lower, folded, not, changed = null, escapeChar = escapeChar, explicitEscape = escape != null)
        }
    }

    private fun replace(
        x: Expr,
        from: String,
        to: String,
    ): Expr = exprCall(Identifier.regular("replace"), listOf(x, exprLit(Literal.string(from)), exprLit(Literal.string(to))), null)

    /**
     * `x [NOT] SIMILAR TO p [ESCAPE e]`. A pattern with no regular expression metacharacters other than `%` and `_` is
     * a LIKE pattern; other patterns are unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/pattern-matching-conditions-similar-to.html
     */
    fun similarTo(ctx: P.PredicateSimilarToContext): Expr {
        val op = ctx.SIMILAR().symbol
        val not = ctx.NOT() != null
        val value = Operand(ctx.lhs, exprs.valueExpr(ctx.lhs))
        val pattern = exprs.valueExpr(ctx.rhs)
        val escapeChar = escapeChar(op, ctx.escape)
        val literal = stringLiteral(pattern) ?: unsupported(op, "SIMILAR TO with a pattern that is not a literal")
        val parts = mutableListOf<Part>()
        val points = literal.codePoints().toArray()
        var i = 0
        while (i < points.size) {
            val c = points[i]
            when {
                escapeChar != null && c == escapeChar -> {
                    if (i + 1 == points.size) unsupported(op, "SIMILAR TO pattern that ends with the escape character")
                    val next = points[i + 1]
                    // The escape character before an ordinary character is a regular expression escape, such as `\d`.
                    if (next != escapeChar && !isSimilarMetacharacter(next) && next != '%'.code && next != '_'.code) {
                        unsupported(op, "SIMILAR TO with regular expression metacharacters")
                    }
                    parts.add(Part.Char(next))
                    i += 2
                    continue
                }
                c == '%'.code -> if (parts.lastOrNull() !is Part.Any) parts.add(Part.Any)
                c == '_'.code -> parts.add(Part.One)
                isSimilarMetacharacter(c) -> unsupported(op, "SIMILAR TO with regular expression metacharacters")
                else -> parts.add(Part.Char(c))
            }
            i++
        }
        return text(op, value) { x ->
            builder.note(op, "Redshift `SIMILAR TO` was replaced by PartiQL `LIKE`")
            encode(op, x, parts, not, changed = null, escapeChar = escapeChar, explicitEscape = ctx.escape != null)
        }
    }

    /**
     * `x ~ p` matches the regular expression `p` anywhere in `x`, and `x !~ p` is its negation. A literal pattern
     * without metacharacters is the string itself, so the translation is `POSITION(p IN x) > 0`. Other patterns, and
     * the case-insensitive `~*` and `!~*`, are unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/pattern-matching-conditions-posix.html
     */
    fun posix(ctx: P.PredicatePosixContext): Expr {
        val op = ctx.op
        if (op.type == P.TILDE_ASTERISK || op.type == P.NOT_TILDE_ASTERISK) unsupported(op, "POSIX regular expression `${op.text}`")
        val value = Operand(ctx.lhs, exprs.valueExpr(ctx.lhs))
        val literal =
            stringLiteral(exprs.valueExpr(ctx.rhs)) ?: unsupported(op, "POSIX regular expression with a pattern that is not a literal")
        if (literal.any { it in POSIX_METACHARACTERS }) unsupported(op, "POSIX regular expression `${op.text}` with metacharacters")
        return text(op, value, allowChar = false) { x ->
            builder.note(op, "Redshift `${op.text}` was replaced by PartiQL `POSITION`")
            val position = exprPosition(exprLit(Literal.string(literal)), x)
            exprOperator(if (op.type == P.TILDE) ">" else "=", position, exprLit(Literal.intNum(0)))
        }
    }

    /**
     * Translates the pattern match of [value] with [match]. Redshift converts a value that is not a character string
     * to VARCHAR; see [RedshiftOperators.toText].
     */
    private fun text(
        op: Token,
        value: Operand,
        allowChar: Boolean = true,
        match: (Expr) -> Expr,
    ): Expr =
        types.resolveTyped(listOf(value)) { (x) ->
            if (!allowChar && x.type.code() == PType.CHAR) unsupported(op, "POSIX regular expression on a CHAR value")
            match(RedshiftOperators.toText(op, x, "`${op.text.uppercase()}`", trimChar = false))
        }

    /** The escape character of an ESCAPE clause, or the default backslash. */
    private fun escapeChar(
        op: Token,
        escape: P.ValueExprContext?,
    ): Int? {
        if (escape == null) return '\\'.code
        val value = stringLiteral(exprs.valueExpr(escape)) ?: unsupported(escape.start, "ESCAPE that is not a character string literal")
        if (value.codePointCount(0, value.length) != 1) unsupported(escape.start, "ESCAPE '$value'; expected one character")
        return value.codePointAt(0)
    }

    /** Decodes a LIKE pattern: the escape character makes the next character literal. */
    private fun parse(
        op: Token,
        pattern: String,
        escapeChar: Int?,
    ): List<Part> {
        val parts = mutableListOf<Part>()
        val points = pattern.codePoints().toArray()
        var i = 0
        while (i < points.size) {
            val c = points[i]
            when {
                escapeChar != null && c == escapeChar -> {
                    if (i + 1 == points.size) unsupported(op, "LIKE pattern that ends with the escape character")
                    parts.add(Part.Char(points[i + 1]))
                    i++
                }
                c == '%'.code -> if (parts.lastOrNull() !is Part.Any) parts.add(Part.Any)
                c == '_'.code -> parts.add(Part.One)
                else -> parts.add(Part.Char(c))
            }
            i++
        }
        return parts
    }

    /**
     * Writes [parts] as a PartiQL LIKE pattern of [value]. A literal `%`, `_`, or backslash is escaped with a backslash,
     * and then the pattern has `ESCAPE '\'`. A note is added when the pattern text or the escape changes; [changed] is
     * the Redshift pattern text, or null if the caller adds its own note.
     */
    private fun encode(
        op: Token,
        value: Expr,
        parts: List<Part>,
        not: Boolean,
        changed: String?,
        escapeChar: Int?,
        explicitEscape: Boolean,
    ): Expr {
        val special = setOf('%'.code, '_'.code, '\\'.code)
        val escaped = parts.any { it is Part.Char && it.codePoint in special }
        val text = StringBuilder()
        for (part in parts) {
            when (part) {
                Part.Any -> text.append('%')
                Part.One -> text.append('_')
                is Part.Char -> {
                    if (escaped && part.codePoint in special) text.append('\\')
                    text.appendCodePoint(part.codePoint)
                }
            }
        }
        val pattern = text.toString()
        if (changed != null && (pattern != changed || escaped != (explicitEscape && escapeChar == '\\'.code))) {
            builder.note(op, "Redshift LIKE pattern escapes were written for PartiQL `ESCAPE '\\'`")
        }
        val escape = if (escaped) exprLit(Literal.string("\\")) else null
        return exprLike(value, exprLit(Literal.string(pattern)), escape, not)
    }

    private fun stringLiteral(expr: Expr): String? = (expr as? ExprLit)?.lit?.takeIf { it.code() == Literal.STRING }?.stringValue()

    private fun isSimilarMetacharacter(c: Int): Boolean = c <= ASCII_MAX && c.toChar() in SIMILAR_METACHARACTERS

    private companion object {
        const val ASCII_MAX = 0x7F
        const val CASE_OFFSET = 'a'.code - 'A'.code

        /** U+0130 and U+212A, the non-ASCII characters whose Java lower case contains ASCII letters. */
        const val DOTTED_CAPITAL_I = "\u0130"
        const val KELVIN_SIGN = "\u212A"

        /** U+00B7, which no ASCII pattern character matches. */
        const val NEUTRAL = "\u00B7"

        /**
         * The regular expression metacharacters of SIMILAR TO other than `%` and `_`, and `.`, `^`, and `$`, whose meaning
         * in Redshift SIMILAR TO is not documented.
         */
        val SIMILAR_METACHARACTERS = setOf('|', '*', '+', '?', '{', '}', '(', ')', '[', ']', '.', '^', '$')

        /** https://docs.aws.amazon.com/redshift/latest/dg/pattern-matching-conditions-posix.html */
        val POSIX_METACHARACTERS = setOf('.', '[', ']', '(', ')', '{', '}', '*', '+', '?', '|', '^', '$', '\\')
    }
}
