package org.partiql.scribe.problems

/**
 * A listener to  report problems encountered during transpilation.
 */
public interface ScribeProblemListener {
    /**
     * Defines the default behavior for a listener when encountering a [ScribeProblem].
     */
    public fun report(problem: ScribeProblem)

    /**
     * Helper function to report a problem and throw a [ScribeException].
     */
    public fun reportAndThrow(problem: ScribeProblem): Nothing {
        report(problem)
        throw ScribeException(problem)
    }

    public companion object {
        /**
         * Returns a listener that will throw a [ScribeException] when encountering an error.
         */
        @JvmStatic
        public fun abortOnError(): ScribeProblemListener {
            return object : ScribeProblemListener {
                override fun report(problem: ScribeProblem) {
                    if (problem.severity.code() == Severity.ERROR) {
                        throw ScribeException(problem)
                    }
                }
            }
        }
    }
}
