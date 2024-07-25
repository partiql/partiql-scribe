package org.partiql.scribe.targets.trino

import org.partiql.plan.PartiQLPlan
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget

/**
 * Experimental Trino SQL transpilation target.
 */
public open class TrinoTarget : SqlTarget() {

    override val target: String = "Trino"

    override val version: String = "3"

    companion object {

        @JvmStatic
        public val DEFAULT = TrinoTarget()
    }

    /**
     * Trino SQL dialect.
     */
    override val dialect = TrinoDialect()

    /**
     * Wire the Trino call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = TrinoCalls(onProblem)

    /**
     * Trino feature set allow list.
     */
    override val features: SqlFeatures = TrinoFeatures()

    /**
     * Apply the base Trino rewriter logic.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) =
        TrinoRewriter(onProblem).visitPartiQLPlan(plan, ctx = null) as PartiQLPlan
}
