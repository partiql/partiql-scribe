package org.partiql.scribe

import org.partiql.ast.Statement
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
            val ast = parse(statement)
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
}
