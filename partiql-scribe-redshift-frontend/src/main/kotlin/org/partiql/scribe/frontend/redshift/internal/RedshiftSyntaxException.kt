package org.partiql.scribe.frontend.redshift.internal

import org.partiql.scribe.frontend.redshift.RedshiftProblem

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
