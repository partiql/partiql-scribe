package org.partiql.scribe.problems

public class ScribeException(public val error: ScribeProblem) : RuntimeException() {
    override fun toString(): String = "ScribeException{" +
            "error=" + error +
            '}'
}
