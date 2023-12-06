package org.partiql.scribe

/**
 * Top-level wrapper of any fatal problem.
 */
public class ScribeException(
    override val message: String?,
    override val cause: Throwable?,
    public val problems: List<ScribeProblem> = emptyList(),
) : Exception() {

    override fun toString(): String = buildString {
        appendLine(message)
        problems.forEach { appendLine(it) }
    }
}
