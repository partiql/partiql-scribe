package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.Severity

/**
 * Stops the AST build at a problem with a position in the Redshift text. The front end reports [problem] to the
 * context's listener and rethrows it as a Scribe exception.
 *
 * Every error is a [ScribeProblem.UNSUPPORTED_OPERATION] with the properties `MESSAGE`, `LINE`, `COLUMN`, and `KIND`,
 * one of [SYNTAX], [UNSUPPORTED], and [INVALID]; the message starts with the kind in words.
 */
internal class TranslationStop(val problem: ScribeProblem) : RuntimeException(problem.toString()) {
    companion object {
        /** The text is not in the grammar. */
        const val SYNTAX = "SYNTAX"

        /** A construct that the front end recognizes but does not convert. */
        const val UNSUPPORTED = "UNSUPPORTED"

        /** A query that Redshift itself rejects, such as an ordinal out of range. */
        const val INVALID = "INVALID"

        fun syntax(
            line: Int,
            column: Int,
            message: String,
        ): ScribeProblem = error(line, column, SYNTAX, "Syntax error: $message")

        fun unsupported(
            ctx: ParserRuleContext,
            construct: String,
        ): TranslationStop = unsupported(ctx.start, construct)

        fun unsupported(
            token: Token,
            construct: String,
        ): TranslationStop = unsupported(token.line, token.charPositionInLine + 1, construct)

        fun unsupported(
            line: Int,
            column: Int,
            construct: String,
        ): TranslationStop = TranslationStop(error(line, column, UNSUPPORTED, "Unsupported Redshift construct: $construct"))

        fun invalid(
            ctx: ParserRuleContext,
            message: String,
        ): TranslationStop = invalid(ctx.start, message)

        fun invalid(
            token: Token,
            message: String,
        ): TranslationStop = TranslationStop(error(token.line, token.charPositionInLine + 1, INVALID, "Invalid Redshift query: $message"))

        /** An error at a position of the Redshift text, with the 1-based `LINE` and `COLUMN` and the `KIND` next to `MESSAGE`. */
        fun error(
            line: Int,
            column: Int,
            kind: String,
            message: String,
        ): ScribeProblem =
            ScribeProblem(
                ScribeProblem.UNSUPPORTED_OPERATION,
                Severity.error(),
                mapOf("MESSAGE" to message, "LINE" to line, "COLUMN" to column, "KIND" to kind),
            )

        /** A [ScribeProblem.TRANSLATION_INFO] note at a position of the Redshift text. */
        fun info(
            line: Int,
            column: Int,
            message: String,
        ): ScribeProblem =
            ScribeProblem(ScribeProblem.TRANSLATION_INFO, Severity.info(), mapOf("MESSAGE" to message, "LINE" to line, "COLUMN" to column))
    }
}
