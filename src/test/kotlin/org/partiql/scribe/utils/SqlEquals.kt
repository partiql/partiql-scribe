package org.partiql.scribe.utils

/**
 * Unit tests compare SQL text to SQL text. We wish to have finer control over how two SQL strings are compared.
 *
 * We do not wish to compare ASTs as some dialects output SQL which is invalid PartiQL syntax. This interface is a
 * place to advance our SQL string comparison logic .. such as ignoring keyword case.
 */
interface SqlEquals {

    fun assertEquals(expected: String, actual: String, debugMessage: StringBuilder.() -> Unit)
}

/**
 * A simple SQL string comparator
 */
object SqlEqualsNaive : SqlEquals {

    override fun assertEquals(expected: String, actual: String, debugMessage: StringBuilder.() -> Unit) {
        val e = expected.clean()
        val a = actual.clean()
        // TODO some nicely formatted error message w/ comparison failures
        assert(e == a) {
            buildString {
                appendLine()
                appendLine("Expect: \"$e\"")
                appendLine("Actual: \"$a\"")
                appendLine()
                debugMessage()
            }
        }
    }

    /**
     * - Remove comments
     * - Remove `;`
     */
    private fun String.clean(): String {
        val sb = StringBuilder()
        lines().forEachIndexed { index, line ->
            var l = line.trim()
            if (line.startsWith("--")) {
                return@forEachIndexed
            }
            if (line.endsWith(";")) {
                l = line.trimEnd(';')
            }
            if (l.isEmpty()) {
                return@forEachIndexed
            }
            if (sb.isNotEmpty()) {
                sb.append(' ')
            }
            sb.append(l)
        }
        return sb.toString()
    }
}
