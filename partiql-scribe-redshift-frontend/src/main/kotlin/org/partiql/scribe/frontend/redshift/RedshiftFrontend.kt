package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.partiql.ast.Statement
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.frontend.redshift.antlr.RedshiftLexer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
import org.partiql.scribe.frontend.redshift.internal.TranslationStop
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.problems.Severity

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
 * The query is parsed with the Redshift grammar, which accepts the subset of Redshift this front end is for; see the
 * grammar for it. The AST is built by [RedshiftAstBuilder]: what parses either converts or is refused at its position.
 * Names and types are resolved by the planner afterwards, so an unknown column, or an operation PartiQL does not define
 * on the operand types, is reported by the planner, not here.
 *
 * Problems are reported to the context's listener as Scribe problems with the `LINE` and `COLUMN` of their position
 * in the Redshift text. A syntax error, a construct without a conversion, and a query that Redshift rejects are
 * [ScribeProblem.UNSUPPORTED_OPERATION] errors; the [KIND] property says which ([KIND_SYNTAX], [KIND_UNSUPPORTED],
 * [KIND_INVALID]), and so does the start of the message. The one that stops the conversion is reported and thrown as
 * [org.partiql.scribe.problems.ScribeException]. A rewrite that changes the shape of the query (a `DISTINCT` query with
 * `LIMIT` becomes a subquery) is a [ScribeProblem.TRANSLATION_INFO] note.
 *
 * A query that nests deeper than [MAX_NESTING] is refused, so that the depth of the text bounds the stack that parsing
 * and converting it use, and the depth of the AST bounds what the planner and the targets recurse over.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting of the source queries.
 */
public class RedshiftFrontend
    @JvmOverloads
    constructor(
        private val context: ScribeContext,
        private val caseSensitiveIdentifiers: Boolean = false,
    ) {
        /**
         * Returns the tables that [sql] reads: the qualified name of every `FROM` item that names a table, as the query
         * writes it (`db.schema.table` gives `["db", "schema", "table"]`), folded as [toAst] folds identifiers, without
         * duplicates, in order of first appearance. Common table expressions are not tables. A caller uses it to build
         * the session that the AST is planned against: each table needs a schema that is a bag of rows with the column
         * types.
         *
         * The names come from the same conversion as [toAst], so a query that [toAst] refuses is refused here, with the
         * same problem; the notes of the conversion are not reported.
         */
        public fun tables(sql: String): List<List<String>> {
            val listener = context.getProblemListener()
            val errorsOnly =
                object : ScribeProblemListener {
                    override fun report(problem: ScribeProblem) {
                        if (problem.severity.code() == Severity.ERROR) listener.report(problem)
                    }
                }
            return build(sql, errorsOnly).tables.toList()
        }

        /** Converts [sql] to a PartiQL AST. */
        public fun toAst(sql: String): Statement = build(sql, context.getProblemListener()).statement

        private class Built(val statement: Statement, val tables: Set<List<String>>)

        private fun build(
            sql: String,
            listener: ScribeProblemListener,
        ): Built {
            try {
                val builder = RedshiftAstBuilder(caseSensitiveIdentifiers, listener)
                val statement = builder.root(parse(sql))
                return Built(statement, builder.tables)
            } catch (stop: TranslationStop) {
                context.getProblemListener().reportAndThrow(stop.problem)
            }
        }

        /**
         * Parses [sql] with the Redshift grammar; the first syntax error is reported and thrown. The text is parsed with
         * SLL prediction first, which is faster and keeps the parser's decision cache small, and again with full LL
         * prediction only when SLL finds an error, as that error may be SLL's alone.
         */
        private fun parse(sql: String): RedshiftParser.RootContext {
            val lexer = RedshiftLexer(CharStreams.fromString(sql))
            val errors = SyntaxErrors(context)
            lexer.removeErrorListeners()
            lexer.addErrorListener(errors)
            val tokens = CommonTokenStream(lexer)
            tokens.fill()
            checkNesting(tokens.tokens)
            val parser = RedshiftParser(tokens)
            parser.removeErrorListeners()
            parser.interpreter.predictionMode = PredictionMode.SLL
            parser.errorHandler = BailErrorStrategy()
            try {
                return parser.root()
            } catch (_: ParseCancellationException) {
                parser.reset()
                parser.interpreter.predictionMode = PredictionMode.LL
                parser.errorHandler = DefaultErrorStrategy()
                parser.addErrorListener(errors)
                return parser.root()
            }
        }

        /**
         * Refuses a text whose nesting the parser would recurse over more than [MAX_NESTING] deep: parentheses or `CASE`
         * expressions open at once, or a run of prefix operators (`NOT`, `-`, `+`), which the grammar nests one inside
         * the other. Everything else that nests in the grammar needs a parenthesis.
         */
        private fun checkNesting(tokens: List<Token>) {
            var depth = 0
            var prefixes = 0
            for (token in tokens) {
                if (token.channel != Token.DEFAULT_CHANNEL) continue // whitespace and comments
                when (token.type) {
                    RedshiftLexer.PAREN_LEFT, RedshiftLexer.CASE -> {
                        if (++depth > MAX_NESTING) {
                            throw TranslationStop.unsupported(
                                token,
                                "more than $MAX_NESTING nested parentheses or CASE expressions",
                            )
                        }
                    }
                    RedshiftLexer.PAREN_RIGHT, RedshiftLexer.END -> depth--
                }
                prefixes =
                    when (token.type) {
                        RedshiftLexer.NOT, RedshiftLexer.MINUS, RedshiftLexer.PLUS -> prefixes + 1
                        else -> 0
                    }
                if (prefixes > MAX_NESTING) throw TranslationStop.unsupported(token, "more than $MAX_NESTING prefix operators in a row")
            }
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
                context.getProblemListener().reportAndThrow(TranslationStop.syntax(line, charPositionInLine + 1, msg))
            }
        }

        public companion object {
            /**
             * The deepest a query may nest, in the text and in the AST: parentheses or `CASE` expressions open at once,
             * prefix operators in a row, and levels of the AST, where each subquery, `CASE`, join, `UNION`, and each
             * operator of a chain (`a AND b AND c` is two deep) is a level. A deeper query is refused as [KIND_UNSUPPORTED].
             */
            @JvmField
            public val MAX_NESTING: Int = 200

            /** The property of an error problem that says which kind it is: [KIND_SYNTAX], [KIND_UNSUPPORTED], or [KIND_INVALID]. */
            public const val KIND: String = "KIND"

            /** The text is not in the grammar; the message starts with `Syntax error:`. */
            public const val KIND_SYNTAX: String = "SYNTAX"

            /** A construct that parses but has no conversion; the message starts with `Unsupported Redshift construct:`. */
            public const val KIND_UNSUPPORTED: String = "UNSUPPORTED"

            /** A query that Redshift itself rejects; the message starts with `Invalid Redshift query:`. */
            public const val KIND_INVALID: String = "INVALID"
        }
    }
