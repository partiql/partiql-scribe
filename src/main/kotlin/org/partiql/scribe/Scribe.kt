package org.partiql.scribe

import org.partiql.plan.PartiQLPlan

/**
 * PartiQL Scribe compiler plan->T part.
 */
public class Scribe {

    /**
     * Entry-point for the scribe compiler.
     *
     * @param T         Return type of target output (eg. SQL Targets return a string)
     * @param plan      Input PartiQL Plan to compile.
     * @param target    Compilation target.
     * @return
     */
    @Throws(ScribeException::class)
    public fun <T> compile(plan: PartiQLPlan, target: ScribeTarget<T>): Result<T> {
        try {
            val collector = ProblemCollector()

            // Assert zero planning problems
            plan.validate(collector::callback)
            if (collector.problems.isNotEmpty()) {
                // don't try to compile if planning encountered errors
                throw ScribeException("PartiQL query plan has errors", null, collector.problems)
            }

            // Plan OK, compile to desired target
            val output = target.compile(plan, collector::callback)

            return Result(plan, output, collector.problems)
        } catch (e: ScribeException) {
            throw e
        } catch (cause: Throwable) {
            throw ScribeException(cause.message, cause)
        }
    }

    public data class Result<T>(
        val input: PartiQLPlan,
        val output: ScribeOutput<T>,
        val problems: List<ScribeProblem>,
    )

    private class ProblemCollector {
        //
        internal val problems = mutableListOf<ScribeProblem>()

        internal fun callback(problem: ScribeProblem) {
            problems.add(problem)
        }
    }

    private fun PartiQLPlan.validate(callback: ProblemCallback) {
        this.statement.accept(ScribePlanValidator, callback)
    }
}
