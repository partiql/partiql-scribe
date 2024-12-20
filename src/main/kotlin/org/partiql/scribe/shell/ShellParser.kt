package org.partiql.scribe.shell

import org.jline.reader.EOFError
import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.Parser.ParseContext.ACCEPT_LINE
import org.jline.reader.Parser.ParseContext.UNSPECIFIED
import org.jline.reader.impl.DefaultParser

/**
 * Line parser which executes on successive newlines or ';'
 */
internal object ShellParser : Parser {

    private val default = DefaultParser()
    private val nonTerminal = setOf(ACCEPT_LINE, UNSPECIFIED)
    private val suffixes = setOf("\n", ";")

    override fun parse(line: String, cursor: Int, ctx: Parser.ParseContext): ParsedLine {
        if (line.isBlank() || ctx == Parser.ParseContext.COMPLETE) {
            return default.parse(line, cursor, ctx)
        }
        if (line.startsWith(".")) {
            return default.parse(line, cursor, ctx)
        }
        if (nonTerminal.contains(ctx) && !line.endsWith(suffixes)) {
            throw EOFError(-1, -1, null)
        }
        return default.parse(line, cursor, ctx)
    }

    private fun String.endsWith(suffixes: Set<String>): Boolean {
        for (suffix in suffixes) {
            if (this.endsWith(suffix)) {
                return true
            }
        }
        return false
    }

    override fun isEscapeChar(ch: Char): Boolean = false
}
