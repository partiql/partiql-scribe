package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftLexer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import org.partiql.scribe.frontend.redshift.internal.RedshiftSyntaxException
import org.partiql.scribe.frontend.redshift.internal.TableReferences
import org.partiql.scribe.frontend.redshift.internal.UnsupportedConstructException
import org.partiql.spi.catalog.Session

/**
 * Translates Amazon Redshift queries to PartiQL.
 *
 * This version parses Redshift with the Redshift grammar, reports syntax errors with their positions, and lists the
 * tables a query reads. [transpile] has its final contract, and refuses every well-formed query as unsupported: the
 * translation follows, construct by construct, each change enabling the cases of the conformance suite it earns.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting of the source queries. By
 * default, quoted and unquoted identifiers fold to lower case; when true, only unquoted identifiers fold.
 */
public class RedshiftTranspiler(
    private val caseSensitiveIdentifiers: Boolean = false,
) {
    /**
     * Returns the tables that [sql] reads: the qualified name of every `FROM` item that names a table, as the query
     * writes it (`andes.provider.table` gives `["andes", "provider", "table"]`), folded as [transpile] folds identifiers,
     * without duplicates, in order of first appearance. Common table expressions and references to other `FROM` items
     * are not tables. A caller uses it to build the [Session] of [transpile]: each table needs a schema that is a bag
     * of rows with the column types.
     *
     * @throws RedshiftSyntaxException at the first syntax error.
     */
    public fun tables(sql: String): List<List<String>> = TableReferences(caseSensitiveIdentifiers).collect(parse(sql).first)

    /**
     * Translates [sql] against [session], which holds the schema of every table the query reads. The result is
     * [RedshiftTranspileResult.Status.INVALID_INPUT] for a syntax error or a reference that [session] does not
     * resolve, [RedshiftTranspileResult.Status.UNSUPPORTED] for a construct without a translation, and
     * [RedshiftTranspileResult.Status.VALID] with the PartiQL text otherwise. Problems carry their position in [sql].
     */
    @Suppress("UNUSED_PARAMETER")
    public fun transpile(
        sql: String,
        session: Session,
    ): RedshiftTranspileResult {
        val root =
            try {
                parse(sql).first
            } catch (ex: RedshiftSyntaxException) {
                return RedshiftTranspileResult(RedshiftTranspileResult.Status.INVALID_INPUT, null, listOf(ex.toProblem()))
            }
        val problem = UnsupportedConstructException.at(root, "query translation").toProblem()
        return RedshiftTranspileResult(RedshiftTranspileResult.Status.UNSUPPORTED, null, listOf(problem))
    }

    /** Parses [sql] with the Redshift grammar and returns the parse tree and its tokens. */
    internal fun parse(sql: String): Pair<RedshiftParser.RootContext, CommonTokenStream> {
        val lexer = RedshiftLexer(CharStreams.fromString(sql))
        val tokens = CommonTokenStream(lexer)
        val parser = RedshiftParser(tokens)
        lexer.removeErrorListeners()
        lexer.addErrorListener(ThrowingErrorListener)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener)
        return parser.root() to tokens
    }

    private object ThrowingErrorListener : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            throw RedshiftSyntaxException(msg, line, charPositionInLine + 1)
        }
    }
}
