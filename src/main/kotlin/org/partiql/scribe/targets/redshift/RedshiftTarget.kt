package org.partiql.scribe.targets.redshift

import org.partiql.plan.PartiQLPlan
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget

/**
 * Experimental Redshift SQL target.
 */
public open class RedshiftTarget : SqlTarget() {

    override val target: String = "Redshift"

    override val version: String = "0"

    /**
     * Redshift feature set allow list.
     */
    override val features: SqlFeatures = RedshiftFeatures()

    companion object {

        @JvmStatic
        public val DEFAULT = RedshiftTarget()
    }

    override val dialect: SqlDialect = RedshiftDialect()

    /**
     * Wire the Redshift call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = RedshiftCalls(onProblem)

    /**
     * Rewrite a PartiQLPlan in terms of Redshift features.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) =
        RedshiftRewriter(onProblem).visitPartiQLPlan(plan, null) as PartiQLPlan
}
