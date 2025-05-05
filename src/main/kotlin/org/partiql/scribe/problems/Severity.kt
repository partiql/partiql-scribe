package org.partiql.scribe.problems

import org.partiql.spi.Enum

/**
 * Severity of a problem encountered during transpilation.
 *
 * This class is purposely separate from partiql-lang-kotlin's Severity class to provide additional control.
 */
public class Severity(private val code: Int) : Enum(code) {
    override fun name(): String {
        return when (code) {
            ERROR -> return "ERROR"
            WARNING -> return "WARNING"
            else -> return "UNKNOWN"
        }
    }

    public companion object {
        @JvmStatic
        public val ERROR: Int = 1

        @JvmStatic
        public val WARNING: Int = 2

        @JvmStatic
        public fun error(): Severity = Severity(ERROR)

        @JvmStatic
        public fun warning(): Severity = Severity(WARNING)
    }
}
