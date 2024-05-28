package org.partiql.scribe

import org.partiql.ast.AstNode
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.util.AstRewriter
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.planner.PartiQLPlanner
import org.partiql.types.function.FunctionSignature

/**
 * ScribeCompiler is a full text-to-text compiler pipeline for the PartiQL Scribe project.
 *
 * @property parser
 * @property planner
 */
public class ScribeCompiler internal constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val functions: List<FunctionSignature.Scalar>
) {

    // backend
    private val scribe: Scribe = Scribe()

    /**
     * Entry-point to scribe via text.
     *
     * @param T
     * @param statement
     * @param target
     * @param session
     * @return
     */
    @Throws(ScribeException::class)
    public fun <T> compile(
        statement: String,
        target: ScribeTarget<T>,
        session: PartiQLPlanner.Session,
    ): Scribe.Result<T> {
        try {
            var ast = parse(statement)

            // TODO: REMOVE ME
            //  PartiQL came from Ion SQL which uses the INT name for the unbounded integer.
            //  In SQL, the INT name must have some finite precision and most systems use a 32-bit integer.
            //  Scribe is built to interface with other systems, so we must change all occurrences of the INT
            //  type name with INT4. In short, all systems do INT = INT4 but PartiQL has INT4 != INT.
            //  >>>> ISSUE — https://github.com/partiql/partiql-lang-kotlin/issues/1471
            ast = replaceIntWithInt4(ast)

            val plan = plan(ast, session)
            return scribe.compile(plan, target)
        } catch (e: ScribeException) {
            throw e
        } catch (cause: Throwable) {
            throw ScribeException(cause.message, cause)
        }
    }

    private fun parse(source: String): Statement {
        val result = parser.parse(source)
        return result.root
    }

    private class PlanProblemCallback: ProblemCallback {
        val problems = mutableListOf<Problem>()

        override fun invoke(p1: Problem) {
            problems.add(p1)
        }
    }

    private fun plan(statement: Statement, session: PartiQLPlanner.Session): PartiQLPlan {
        val callback = PlanProblemCallback()
        val result = planner.plan(statement, session, onProblem = callback)
        val errors = callback.problems.filter { it.details.severity == ProblemSeverity.ERROR }
        if (errors.isNotEmpty()) {
            throw RuntimeException("Planner encountered errors: ${errors.joinToString()}")
        }
        return result.plan
    }

    companion object {

        @JvmStatic
        public fun builder(): ScribeCompilerBuilder = ScribeCompilerBuilder()
    }

    /**
     * Rewrite all occurrences of INT with INT4.
     */
    private fun replaceIntWithInt4(ast: Statement): Statement {
        val rewriter = object : AstRewriter<Unit>() {
            override fun visitTypeInt(node: Type.Int, ctx: Unit): AstNode = Type.Int4()
        }
        return rewriter.visitStatement(ast, Unit) as Statement
    }
}
