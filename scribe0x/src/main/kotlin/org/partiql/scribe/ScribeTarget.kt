package org.partiql.scribe

import org.partiql.plan.PartiQLPlan

/**
 * A target defines the PartiQL Plan to output T compilation.
 */
public interface ScribeTarget<T> {

    /**
     * Target identifier, useful for debugging information.
     */
    public val target: String

    /**
     * Target version, useful for distinguishing slight variations of targets.
     */
    public val version: String

    /**
     * Implement [PartiQLPlan] to desired transpiler output here.
     *
     * @param plan
     * @param onProblem
     * @return
     */
    public fun compile(plan: PartiQLPlan, onProblem: ProblemCallback): ScribeOutput<T>
}
