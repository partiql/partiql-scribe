package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.Severity

/**
 * Stops the AST build at a problem with a position in the Redshift text. The front end reports [problem] to the
 * context's listener and rethrows it as a Scribe exception.
 */
internal class TranslationStop(val problem: ScribeProblem) : RuntimeException(problem.toString()) {
    companion object {
        /** A construct that the front end recognizes but does not convert. */
        fun unsupported(
            ctx: ParserRuleContext,
            construct: String,
        ): TranslationStop = unsupported(ctx.start, construct)

        fun unsupported(
            token: Token,
            construct: String,
        ): TranslationStop = TranslationStop(error(token.line, token.charPositionInLine + 1, "Unsupported Redshift construct: $construct"))

        /** A query that Redshift itself rejects, such as an ordinal out of range. */
        fun invalid(
            ctx: ParserRuleContext,
            message: String,
        ): TranslationStop = TranslationStop(error(ctx.start.line, ctx.start.charPositionInLine + 1, "Invalid Redshift query: $message"))

        /**
         * An [ScribeProblem.UNSUPPORTED_OPERATION] error at a position of the Redshift text, with the 1-based `LINE` and
         * `COLUMN` as properties next to `MESSAGE`.
         */
        fun error(
            line: Int,
            column: Int,
            message: String,
        ): ScribeProblem = problem(ScribeProblem.UNSUPPORTED_OPERATION, Severity.error(), line, column, message)

        /** A [ScribeProblem.TRANSLATION_INFO] note at a position of the Redshift text. */
        fun info(
            line: Int,
            column: Int,
            message: String,
        ): ScribeProblem = problem(ScribeProblem.TRANSLATION_INFO, Severity.info(), line, column, message)

        private fun problem(
            code: Int,
            severity: Severity,
            line: Int,
            column: Int,
            message: String,
        ): ScribeProblem = ScribeProblem(code, severity, mapOf("MESSAGE" to message, "LINE" to line, "COLUMN" to column))
    }
}
