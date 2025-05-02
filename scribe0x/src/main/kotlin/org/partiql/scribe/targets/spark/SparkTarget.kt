package org.partiql.scribe.targets.spark

import org.partiql.plan.PartiQLPlan
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget

public open class SparkTarget : SqlTarget() {

    override val target: String = "Spark"

    override val version: String = "3.1"

    companion object {

        @JvmStatic
        public val DEFAULT = SparkTarget()
    }

    override val dialect: SqlDialect = SparkDialect()

    override val features: SqlFeatures = SparkFeatures()

    override fun getCalls(onProblem: ProblemCallback): SqlCalls = SparkCalls(onProblem)

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan =
        SparkRewriter(onProblem).visitPartiQLPlan(plan, null) as PartiQLPlan
}
