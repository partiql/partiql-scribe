package org.partiql.scribe.frontend.redshift

/**
 * The result of one Redshift-to-PartiQL translation.
 *
 * @property status The outcome of the translation.
 * @property partiql The PartiQL query text. It is not null only when [status] is [Status.VALID].
 * @property problems The errors and translation notes, in the order the translator found them.
 */
public class RedshiftTranspileResult internal constructor(
    public val status: Status,
    public val partiql: String?,
    public val problems: List<RedshiftProblem>,
) {
    public enum class Status {
        /** The translation succeeded, and the PartiQL parser accepts the output. */
        VALID,

        /** The input is valid Redshift, but it uses a construct that the translator does not support. */
        UNSUPPORTED,

        /** The input is not valid: a syntax error, or a reference that the catalog does not resolve. */
        INVALID_INPUT,

        /** The translator produced output that the PartiQL parser rejects. This is a translator defect. */
        INTERNAL_ERROR,
    }

    override fun toString(): String = "RedshiftTranspileResult(status=$status, partiql=$partiql, problems=$problems)"
}

/**
 * An error or a note from the translator.
 *
 * @property line The 1-based line in the Redshift input, if the problem has a location.
 * @property column The 1-based column in the Redshift input, if the problem has a location.
 */
public class RedshiftProblem internal constructor(
    public val code: Code,
    public val message: String,
    public val line: Int? = null,
    public val column: Int? = null,
) {
    public enum class Code(public val isError: Boolean) {
        SYNTAX_ERROR(true),
        UNSUPPORTED(true),
        SEMANTIC_ERROR(true),
        INTERNAL_ERROR(true),

        /** A rewrite that changes the form of the query. Read these notes when you review a translation. */
        TRANSLATION_INFO(false),
    }

    override fun toString(): String {
        val location = if (line != null && column != null) " at $line:$column" else ""
        return "$code$location: $message"
    }
}
