package org.partiql.scribe

import org.partiql.ast.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.scribe.analyzer.SqlAnalyzer

/**
 * Scribe top-level interface.
 */
public class Scribe {

    private val parser = PartiQLParser.standard()

    public fun compile(source: String): String {
        val statement = parse(source)

        // create the scribe ir
        val analyzer = SqlAnalyzer()
        val ir = analyzer.analyze(statement)

        // layout as sql text
        // TODO

        return ""
    }

    private fun parse(source: String): Statement {
        val res = parser.parse(source)
        if (res.statements.size == 0) {
            throw Exception("No statements found")
        }
        if (res.statements.size > 1) {
            throw Exception("Multiple statements found")
        }
        return res.statements[0]
    }
}
