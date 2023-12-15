package org.partiql.scribe.targets.spark

import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.rexOpSelect
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget

object SparkTarget : SqlTarget() {
    override val target: String = "Spark"

    override val version: String = "0"

    override val dialect: SqlDialect = SparkDialect

    override val features: SqlFeatures = SparkFeatures

    override fun getCalls(onProblem: ProblemCallback): SqlCalls = SparkCalls(onProblem)

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan =
        SparkRewriter(onProblem).visitPartiQLPlan(plan, Unit) as PartiQLPlan

    // Deliberately kept as an empty class for now.
    private class SparkRewriter(val onProblem: ProblemCallback) : PlanRewriter<Unit>()
}