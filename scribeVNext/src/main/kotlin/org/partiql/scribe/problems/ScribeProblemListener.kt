package org.partiql.scribe.problems

interface ScribeProblemListener {
    fun report(problem: ScribeProblem)

    fun reportAndThrow(problem: ScribeProblem): Nothing {
        report(problem)
        throw ScribeException(problem)
    }

    companion object {
        @JvmStatic
        fun abortOnError(): ScribeProblemListener {
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
