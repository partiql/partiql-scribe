package org.partiql.scribe.problems

import org.partiql.spi.Enum
import org.partiql.spi.UnsupportedCodeException

class ScribeProblem(
    public val code: Int,
    public val severity: Severity,
    public val properties: Map<String, Any>
): Enum(code) {
    override fun name(): String {
        return when (code) {
            INTERNAL_ERROR -> "INTERNAL_ERROR"
            UNSUPPORTED_OPERATION -> "UNSUPPORTED_OPERATION"
            UNSUPPORTED_PLAN_TO_AST_CONVERSION -> "UNSUPPORTED_PLAN_TO_AST_CONVERSION"
            INVALID_PLAN -> "INVALID_PLAN"
            else -> throw UnsupportedCodeException(code)
        }
    }

    companion object {
        @JvmStatic
        fun simpleError(code: Int, message: String): ScribeProblem {
            return ScribeProblem(
                code,
                Severity.ERROR(),
                mapOf("MESSAGE" to message)
            )
        }

        // Static constants
        @JvmStatic
        public final val INTERNAL_ERROR = 1

        @JvmStatic
        public final val UNSUPPORTED_OPERATION = 2

        @JvmStatic
        public final val UNSUPPORTED_PLAN_TO_AST_CONVERSION = 3

        @JvmStatic
        public final val INVALID_PLAN = 4
    }

    ///
    ///
    /// PUBLIC METHODS
    ///
    ///
    @Throws(java.lang.ClassCastException::class)
    fun <T> get(key: String, clazz: Class<T>): T? {
        val value: Any = properties[key] ?: return null
        return clazz.cast(value)
    }

    fun <T> getOrNull(key: String, clazz: Class<T>): T? {
        return try {
            get(key, clazz)
        } catch (ex: ClassCastException) {
            null
        }
    }

    override fun toString(): String {
        val name = try {
            name()
        } catch (e: UnsupportedCodeException) {
            code().toString()
        }
        return "ScribeError{" +
                "code=" + name +
                ", severity=" + severity +
                ", properties=" + properties +
                '}'
    }
}
