package org.partiql.scribe.problems

public interface ScribeProblemListener {
    public fun report(problem: ScribeProblem)

    public fun reportAndThrow(problem: ScribeProblem): Nothing {
        report(problem)
        throw ScribeException(problem)
    }

    public companion object {
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
