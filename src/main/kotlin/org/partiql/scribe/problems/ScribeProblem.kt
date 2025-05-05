package org.partiql.scribe.problems

import org.partiql.spi.Enum
import org.partiql.spi.UnsupportedCodeException

/**
 * Represents a problem that is encountered during transpilation.
 *
 * @property code
 * @property severity The severity of the problem.
 * @property properties The properties of the problem.
 */
public class ScribeProblem(
    private val code: Int,
    public val severity: Severity,
    private val properties: Map<String, Any>,
) : Enum(code) {
    override fun name(): String {
        return when (code) {
            INTERNAL_ERROR -> "INTERNAL_ERROR"
            UNSUPPORTED_OPERATION -> "UNSUPPORTED_OPERATION"
            UNSUPPORTED_PLAN_TO_AST_CONVERSION -> "UNSUPPORTED_PLAN_TO_AST_CONVERSION"
            INVALID_PLAN -> "INVALID_PLAN"
            else -> throw UnsupportedCodeException(code)
        }
    }

    public companion object {
        @JvmStatic
        public fun simpleError(
            code: Int,
            message: String,
        ): ScribeProblem {
            return ScribeProblem(
                code,
                Severity.error(),
                mapOf("MESSAGE" to message),
            )
        }

        // Static constants
        @JvmStatic
        public val INTERNAL_ERROR: Int = 1

        @JvmStatic
        public val UNSUPPORTED_OPERATION: Int = 2

        @JvmStatic
        public val UNSUPPORTED_PLAN_TO_AST_CONVERSION: Int = 3

        @JvmStatic
        public val INVALID_PLAN: Int = 4
    }

    // /
    // /
    // / PUBLIC METHODS
    // /
    // /
    @Throws(java.lang.ClassCastException::class)
    public fun <T> get(
        key: String,
        clazz: Class<T>,
    ): T? {
        val value: Any = properties[key] ?: return null
        return clazz.cast(value)
    }

    public fun <T> getOrNull(
        key: String,
        clazz: Class<T>,
    ): T? {
        return try {
            get(key, clazz)
        } catch (ex: ClassCastException) {
            null
        }
    }

    override fun toString(): String {
        val name =
            try {
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
