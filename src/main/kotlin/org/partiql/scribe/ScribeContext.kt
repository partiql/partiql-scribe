package org.partiql.scribe

import org.partiql.scribe.problems.ScribeProblemListener

public interface ScribeContext {
    public fun getProblemListener(): ScribeProblemListener

    public companion object {
        public fun standard(): ScribeContext {
            return object : ScribeContext {
                override fun getProblemListener(): ScribeProblemListener {
                    return ScribeProblemListener.abortOnError()
                }
            }
        }
    }
}
