package org.partiql.scribe

import org.partiql.ast.Statement
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.planner.PartiQLPlanner

/**
 * ScribeCompiler is a full text-to-text compiler pipeline for the PartiQL Scribe project.
 *
 * @property parser
 * @property planner
 */
public class ScribeCompiler internal constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
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

    private fun plan(statement: Statement, session: PartiQLPlanner.Session): PartiQLPlan {
        val result = planner.plan(statement, session)
        val errors = result.problems.filter { it.details.severity == ProblemSeverity.ERROR }
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
