package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.partiql.ast.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.frontend.redshift.antlr.RedshiftLexer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import org.partiql.scribe.frontend.redshift.internal.InvalidQueryException
import org.partiql.scribe.frontend.redshift.internal.NameAnalysis
import org.partiql.scribe.frontend.redshift.internal.OperandTypes
import org.partiql.scribe.frontend.redshift.internal.PartiQLOutputTarget
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
import org.partiql.scribe.frontend.redshift.internal.RedshiftCatalog
import org.partiql.scribe.frontend.redshift.internal.RedshiftSyntaxException
import org.partiql.scribe.frontend.redshift.internal.UnsupportedConstructException
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.problems.Severity
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity as PSeverity

/**
 * Translates Amazon Redshift queries to PartiQL.
 *
 * The translation has four steps:
 *  1. Parse the Redshift text with the Redshift grammar.
 *  2. Build a PartiQL AST from the parse tree. Dialect differences are rewritten here.
 *  3. Plan the AST with the PartiQL planner against [Session]. This resolves names and types.
 *  4. Print the plan as PartiQL with [PartiQLOutputTarget], then parse and plan the output to verify it.
 *
 * The translation is deterministic: the same input and session always give the same result.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting of the source queries.
 */
public class RedshiftTranspiler(
    private val caseSensitiveIdentifiers: Boolean = false,
) {
    private val planner = PartiQLPlanner.standard()
    private val partiqlParser = PartiQLParser.standard()

    public fun transpile(
        sql: String,
        session: Session,
    ): RedshiftTranspileResult {
        val problems = mutableListOf<RedshiftProblem>()

        // 1 and 2: Redshift text to PartiQL AST.
        val statement =
            try {
                toPartiQLAst(sql, problems, session)
            } catch (ex: RedshiftSyntaxException) {
                return result(RedshiftTranspileResult.Status.INVALID_INPUT, problems + ex.toProblem())
            } catch (ex: InvalidQueryException) {
                val problem = RedshiftProblem(RedshiftProblem.Code.SEMANTIC_ERROR, ex.message, ex.line, ex.column)
                return result(RedshiftTranspileResult.Status.INVALID_INPUT, problems + problem)
            } catch (ex: UnsupportedConstructException) {
                return result(RedshiftTranspileResult.Status.UNSUPPORTED, problems + ex.toProblem())
            }

        // 3: PartiQL AST to plan.
        val inputProblems = CollectingPErrorListener()
        val plan = planner.plan(statement, session, Context.of(inputProblems)).plan
        if (inputProblems.errors.any { it.code() == PError.SET_OP_SCHEMA_INCOMPATIBLE }) {
            // PLK 1.6.1 rejects a set operation over columns of the dynamic type (SUPER values), which Redshift allows.
            val problem =
                RedshiftProblem(RedshiftProblem.Code.UNSUPPORTED, "set operation over SUPER values or over columns of incompatible types")
            return result(RedshiftTranspileResult.Status.UNSUPPORTED, problems + problem)
        }
        if (inputProblems.errors.isNotEmpty()) {
            val semanticProblems = inputProblems.errors.map { RedshiftProblem(RedshiftProblem.Code.SEMANTIC_ERROR, it.toString()) }
            return result(RedshiftTranspileResult.Status.INVALID_INPUT, problems + semanticProblems)
        }

        // 4: Plan to PartiQL text.
        val scribeListener = CollectingScribeListener()
        val partiql =
            try {
                Scribe(scribeContext = ScribeContextOf(scribeListener)).compile(plan, session, PartiQLOutputTarget()).output.value
            } catch (ex: ScribeException) {
                null
            }
        if (partiql == null || scribeListener.errors.isNotEmpty()) {
            val scribeProblems =
                scribeListener.errors.map {
                    RedshiftProblem(RedshiftProblem.Code.UNSUPPORTED, it.getOrNull("MESSAGE", String::class.java) ?: it.toString())
                }
            return result(RedshiftTranspileResult.Status.UNSUPPORTED, problems + scribeProblems)
        }

        // Verify that the output is valid PartiQL: it parses, and it plans against the same session without a problem
        // that the plan of the input does not have.
        val outputProblems = CollectingPErrorListener()
        val outputContext = Context.of(outputProblems)
        partiqlParser.parse(partiql, outputContext).statements.singleOrNull()?.let { planner.plan(it, session, outputContext) }
        val invalid =
            outputProblems.errors.map {
                RedshiftProblem(RedshiftProblem.Code.INTERNAL_ERROR, "Output is not valid PartiQL: $it; output: $partiql")
            } +
                newWarnings(inputProblems.warnings, outputProblems.warnings).map {
                    RedshiftProblem(RedshiftProblem.Code.INTERNAL_ERROR, "Output plans with a new problem: $it; output: $partiql")
                }
        if (invalid.isNotEmpty()) return result(RedshiftTranspileResult.Status.INTERNAL_ERROR, problems + invalid)
        return RedshiftTranspileResult(RedshiftTranspileResult.Status.VALID, partiql, problems)
    }

    /**
     * Returns the warnings of the output plan that the input plan does not have, compared by code and count.
     *
     * A printer defect can make the output refer to different variables than the input, which the planner reports as a
     * warning, not an error: a path that never succeeds (`PATH_KEY_NEVER_SUCCEEDS`), an expression that is always
     * MISSING (`ALWAYS_MISSING`), or a variable that it does not find. A warning that the input plan also has (for
     * example a SUPER path that never succeeds) is not a defect, so the check compares the number of warnings of each
     * code. It does not compare `EXPERIMENTAL` warnings: the PartiQL parser reports window functions in the output,
     * and the input does not go through the parser.
     */
    private fun newWarnings(
        input: List<PError>,
        output: List<PError>,
    ): List<PError> {
        val inputCounts = input.groupingBy { it.code() }.eachCount()
        return output.groupBy { it.code() }.flatMap { (code, warnings) -> warnings.drop(inputCounts[code] ?: 0) }
    }

    /**
     * Parses [sql] and returns the PartiQL AST, before planning.
     *
     * The rules whose translation depends on the static types of their operands get the types from probe passes,
     * which plan the AST against [session]; see [OperandTypes].
     *
     * The first pass also finds the FROM aliases that the later passes rename; see [NameAnalysis].
     *
     * @throws RedshiftSyntaxException at the first syntax error.
     * @throws UnsupportedConstructException at the first construct that has no translation.
     * @throws InvalidQueryException at a query that Redshift rejects.
     */
    internal fun toPartiQLAst(
        sql: String,
        notes: MutableList<RedshiftProblem>,
        session: Session,
    ): Statement {
        val lexer = RedshiftLexer(CharStreams.fromString(sql))
        val tokens = CommonTokenStream(lexer)
        val parser = RedshiftParser(tokens)
        lexer.removeErrorListeners()
        lexer.addErrorListener(ThrowingErrorListener)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener)
        val tree = parser.root()
        checkComments(tokens)
        val catalog = RedshiftCatalog(session)
        val analysis = NameAnalysis(tokens.tokens.mapTo(HashSet()) { it.text.removeSurrounding("\"").lowercase() })
        val types = OperandTypes()
        val probeSession = types.probeSession(session)
        for (pass in 1..OperandTypes.MAX_PASSES) {
            types.startProbe()
            val probe = RedshiftAstBuilder(caseSensitiveIdentifiers, mutableListOf(), types, catalog, analysis).root(tree)
            // A pass that renames FROM aliases makes a different AST in the next pass.
            if (analysis.finishPass()) continue
            if (!types.probed) break
            // A probe pass only reads types: the final plan reports the problems of the query.
            val plan =
                try {
                    planner.plan(probe, probeSession, Context.of(IgnoringPErrorListener)).plan
                } catch (ex: RuntimeException) {
                    break
                }
            if (!types.collect(probe, plan)) break
        }
        types.startFinal()
        return RedshiftAstBuilder(caseSensitiveIdentifiers, notes, types, catalog, analysis).root(tree)
    }

    /**
     * Redshift does not document whether block comments nest in SQL statements (PostgreSQL nests them, and PL/pgSQL
     * does not), and the two readings give different queries, so a block comment that contains the start of another
     * block comment is unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_PLpgSQL-structure.html
     */
    private fun checkComments(tokens: CommonTokenStream) {
        for (token in tokens.tokens) {
            if (token.type == RedshiftLexer.COMMENT_BLOCK && token.text.indexOf("/*", startIndex = 2) >= 0) {
                throw UnsupportedConstructException.at(token, "nested block comment")
            }
        }
    }

    private fun result(
        status: RedshiftTranspileResult.Status,
        problems: List<RedshiftProblem>,
    ): RedshiftTranspileResult = RedshiftTranspileResult(status, null, problems)

    private object IgnoringPErrorListener : PErrorListener {
        override fun report(error: PError) {}
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

    /**
     * Collects planner and parser errors. The planner reports some problems as warnings that make the plan unusable
     * (for example an unknown table or function); these count as errors, because Redshift rejects such a query. The
     * other warnings, except `EXPERIMENTAL`, are in [warnings].
     */
    private class CollectingPErrorListener : PErrorListener {
        val errors = mutableListOf<PError>()
        val warnings = mutableListOf<PError>()

        override fun report(error: PError) {
            val isError = error.severity.code() == PSeverity.ERROR || error.code() in WARNINGS_AS_ERRORS
            when {
                isError -> errors.add(error)
                error.code() != PError.EXPERIMENTAL -> warnings.add(error)
            }
        }

        private companion object {
            val WARNINGS_AS_ERRORS =
                setOf(
                    PError.FUNCTION_NOT_FOUND,
                    PError.FUNCTION_TYPE_MISMATCH,
                    PError.FUNCTION_AMBIGUOUS,
                    PError.VAR_REF_NOT_FOUND,
                    PError.VAR_REF_AMBIGUOUS,
                    PError.UNDEFINED_CAST,
                )
        }
    }

    private class CollectingScribeListener : ScribeProblemListener {
        val errors = mutableListOf<ScribeProblem>()

        override fun report(problem: ScribeProblem) {
            if (problem.severity.code() == Severity.ERROR) errors.add(problem)
        }
    }

    private class ScribeContextOf(private val listener: ScribeProblemListener) : ScribeContext {
        override fun getProblemListener(): ScribeProblemListener = listener
    }
}
