package org.partiql.scribe

import org.partiql.scribe.problems.ScribeProblemListener

public interface ScribeContext {
    public fun getErrorListener(): ScribeProblemListener

    public companion object {
        public fun standard(): ScribeContext {
            return object : ScribeContext {
                override fun getErrorListener(): ScribeProblemListener {
                    return ScribeProblemListener.abortOnError()
                }
            }
        }
    }
}
