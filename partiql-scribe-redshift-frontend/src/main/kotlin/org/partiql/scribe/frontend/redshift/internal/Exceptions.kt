package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.scribe.frontend.redshift.RedshiftProblem

/**
 * Stops the translation at a construct that the translator recognizes but does not translate.
 */
internal class UnsupportedConstructException(
    val construct: String,
    val line: Int,
    val column: Int,
) : RuntimeException("Unsupported Redshift construct: $construct") {
    fun toProblem(): RedshiftProblem = RedshiftProblem(RedshiftProblem.Code.UNSUPPORTED, message!!, line, column)

    companion object {
        fun at(
            ctx: ParserRuleContext,
            construct: String,
        ): UnsupportedConstructException = at(ctx.start, construct)

        fun at(
            token: Token,
            construct: String,
        ): UnsupportedConstructException = UnsupportedConstructException(construct, token.line, token.charPositionInLine + 1)
    }
}

/**
 * Stops the translation at the first syntax error.
 */
internal class RedshiftSyntaxException(
    override val message: String,
    val line: Int,
    val column: Int,
) : RuntimeException(message) {
    fun toProblem(): RedshiftProblem = RedshiftProblem(RedshiftProblem.Code.SYNTAX_ERROR, message, line, column)
}
