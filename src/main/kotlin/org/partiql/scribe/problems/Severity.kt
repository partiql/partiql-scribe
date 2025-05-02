package org.partiql.scribe.problems

import org.partiql.spi.Enum

/**
 * Differs from partiql-lang-kotlin's Severity class to provide additional control.
 */
class Severity(internal val code: Int): Enum(code) {
    override fun name(): String {
        return when (code) {
            ERROR -> return "ERROR"
            WARNING -> return "WARNING"
            else -> return "UNKNOWN"
        }
    }

    companion object {
        @JvmStatic
        public final val ERROR = 1

        @JvmStatic
        public final val WARNING = 2

        @JvmStatic
        public fun ERROR() = Severity(ERROR)

        @JvmStatic
        public fun WARNING() = Severity(WARNING)
    }
}
