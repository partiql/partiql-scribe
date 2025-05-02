package org.partiql.scribe

import org.partiql.scribe.problems.ScribeProblemListener

interface ScribeContext {
    fun getErrorListener(): ScribeProblemListener

    companion object {
        fun standard(): ScribeContext {
            return object : ScribeContext {
                override fun getErrorListener(): ScribeProblemListener {
                    return ScribeProblemListener.abortOnError()
                }
            }
        }
    }
}
