package org.partiql.scribe

/**
 * Simple handler. The extension methods make me think I've recreated a class..
 */
typealias ProblemCallback = (ScribeProblem) -> Unit

public fun ProblemCallback.info(message: String) = this(
    ScribeProblem(
        level = ScribeProblem.Level.INFO,
        message = message
    )
)

public fun ProblemCallback.warn(message: String) = this(
    ScribeProblem(
        level = ScribeProblem.Level.WARNING,
        message = message
    )
)

public fun ProblemCallback.error(message: String) = this(
    ScribeProblem(
        level = ScribeProblem.Level.ERROR,
        message = message
    )
)
