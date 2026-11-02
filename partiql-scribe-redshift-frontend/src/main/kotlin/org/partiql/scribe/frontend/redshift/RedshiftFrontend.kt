package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.partiql.ast.Statement
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.frontend.redshift.antlr.RedshiftLexer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
import org.partiql.scribe.frontend.redshift.internal.TableReferences
import org.partiql.scribe.frontend.redshift.internal.TranslationStop
import org.partiql.scribe.problems.ScribeProblem

/**
 * Converts an Amazon Redshift query to a PartiQL AST. The caller plans the AST with the PartiQL planner against its
 * session and compiles the plan with [org.partiql.scribe.Scribe.compile] and any target; the front end itself needs no
 * session.
 *
 * ```
 * val frontend = RedshiftFrontend(context)
 * val tables = frontend.tables(sql)                                  // to build the Session
 * val ast = frontend.toAst(sql)
 * val plan = PartiQLPlanner.standard().plan(ast, session).plan
 * val output = Scribe(context).compile(plan, session, PartiQLTarget).output.value
 * ```
 *
 * The query is parsed with the Redshift grammar, which accepts exactly the constructs this front end converts; see the
 * grammar for the supported subset. The AST is built by [RedshiftAstBuilder]. Names and types are resolved by the
 * planner afterwards, so an unknown column, or an operation PartiQL does not define on the operand types, is reported
 * by the planner, not here.
 *
 * Problems are reported to the context's listener as Scribe problems with the `LINE` and `COLUMN` of their position
 * in the Redshift text. A syntax error, a construct without a conversion, and a query that Redshift rejects are
 * [ScribeProblem.UNSUPPORTED_OPERATION] errors whose message says which (the grammar accepts exactly what the front end
 * converts, so a syntax error is the unsupported construct); the one that stops the conversion is reported and thrown
 * as [org.partiql.scribe.problems.ScribeException]. Each rewrite that changes the form of the query is a
 * [ScribeProblem.TRANSLATION_INFO] note.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting of the source queries.
 */
public class RedshiftFrontend(
    private val context: ScribeContext,
    private val caseSensitiveIdentifiers: Boolean = false,
) {
    /**
     * Returns the tables that [sql] reads: the qualified name of every `FROM` item that names a table, as the query
     * writes it (`db.schema.table` gives `["db", "schema", "table"]`), folded as [toAst] folds identifiers, without
     * duplicates, in order of first appearance. Common table expressions are not tables. A caller uses it to build the
     * session that the AST is planned against: each table needs a schema that is a bag of rows with the column types.
     */
    public fun tables(sql: String): List<List<String>> = TableReferences(caseSensitiveIdentifiers).collect(parse(sql))

    /** Converts [sql] to a PartiQL AST. */
    public fun toAst(sql: String): Statement {
        val listener = context.getProblemListener()
        return try {
            RedshiftAstBuilder(caseSensitiveIdentifiers, listener).root(parse(sql))
        } catch (stop: TranslationStop) {
            listener.reportAndThrow(stop.problem)
        }
    }

    /** Parses [sql] with the Redshift grammar; the first syntax error is reported and thrown. */
    private fun parse(sql: String): RedshiftParser.RootContext {
        val lexer = RedshiftLexer(CharStreams.fromString(sql))
        val parser = RedshiftParser(CommonTokenStream(lexer))
        val errors = SyntaxErrors(context)
        lexer.removeErrorListeners()
        lexer.addErrorListener(errors)
        parser.removeErrorListeners()
        parser.addErrorListener(errors)
        return parser.root()
    }

    private class SyntaxErrors(private val context: ScribeContext) : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            context.getProblemListener().reportAndThrow(TranslationStop.error(line, charPositionInLine + 1, "Syntax error: $msg"))
        }
    }
}
