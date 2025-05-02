package org.partiql.scribe.problems

class ScribeException(val error: ScribeProblem) : RuntimeException() {
    override fun toString(): String = "ScribeException{" +
            "error=" + error +
            '}'
}
