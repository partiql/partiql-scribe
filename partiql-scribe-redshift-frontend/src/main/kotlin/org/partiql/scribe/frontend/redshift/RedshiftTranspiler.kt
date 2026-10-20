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
import org.partiql.scribe.frontend.redshift.internal.OperandTypes
import org.partiql.scribe.frontend.redshift.internal.PartiQLOutputTarget
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
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
            } catch (ex: UnsupportedConstructException) {
                return result(RedshiftTranspileResult.Status.UNSUPPORTED, problems + ex.toProblem())
            }

        // 3: PartiQL AST to plan.
        val plannerErrors = CollectingPErrorListener()
        val plan = planner.plan(statement, session, Context.of(plannerErrors)).plan
        if (plannerErrors.errors.isNotEmpty()) {
            val semanticProblems = plannerErrors.errors.map { RedshiftProblem(RedshiftProblem.Code.SEMANTIC_ERROR, it.toString()) }
            return result(RedshiftTranspileResult.Status.INVALID_INPUT, problems + semanticProblems)
        }

        // 4: Plan to PartiQL text.
        val scribeListener = CollectingScribeListener()
        val partiql =
            try {
                Scribe(scribeContext = ScribeContextOf(scribeListener)).compile(plan, session, PartiQLOutputTarget(problems)).output.value
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

        // Verify that the output is valid PartiQL: it parses, and it plans against the same session.
        val outputErrors = CollectingPErrorListener()
        val outputContext = Context.of(outputErrors)
        partiqlParser.parse(partiql, outputContext).statements.singleOrNull()?.let { planner.plan(it, session, outputContext) }
        if (outputErrors.errors.isNotEmpty()) {
            val internal =
                outputErrors.errors.map {
                    RedshiftProblem(RedshiftProblem.Code.INTERNAL_ERROR, "Output is not valid PartiQL: $it; output: $partiql")
                }
            return result(RedshiftTranspileResult.Status.INTERNAL_ERROR, problems + internal)
        }
        return RedshiftTranspileResult(RedshiftTranspileResult.Status.VALID, partiql, problems)
    }

    /**
     * Parses [sql] and returns the PartiQL AST, before planning.
     *
     * The rules whose translation depends on the static types of their operands get the types from probe passes,
     * which plan the AST against [session]; see [OperandTypes].
     *
     * @throws RedshiftSyntaxException at the first syntax error.
     * @throws UnsupportedConstructException at the first construct that has no translation.
     */
    internal fun toPartiQLAst(
        sql: String,
        notes: MutableList<RedshiftProblem>,
        session: Session,
    ): Statement {
        val lexer = RedshiftLexer(CharStreams.fromString(sql))
        val parser = RedshiftParser(CommonTokenStream(lexer))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ThrowingErrorListener)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener)
        val tree = parser.root()
        val types = OperandTypes()
        val probeSession = types.probeSession(session)
        for (pass in 1..OperandTypes.MAX_PASSES) {
            types.startProbe()
            val probe = RedshiftAstBuilder(caseSensitiveIdentifiers, mutableListOf(), types).root(tree)
            if (!types.probed) break
            // A probe pass only reads types: the final plan reports the problems of the query.
            val plan =
                try {
                    planner.plan(probe, probeSession, Context.of(IgnoringPErrorListener)).plan
                } catch (ex: RuntimeException) {
                    break
                }
            if (!types.collect(plan)) break
        }
        types.startFinal()
        return RedshiftAstBuilder(caseSensitiveIdentifiers, notes, types).root(tree)
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
     * (for example an unknown table or function); these count as errors, because Redshift rejects such a query.
     */
    private class CollectingPErrorListener : PErrorListener {
        val errors = mutableListOf<PError>()

        override fun report(error: PError) {
            val isError = error.severity.code() == PSeverity.ERROR || error.code() in WARNINGS_AS_ERRORS
            if (isError) errors.add(error)
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
