package org.partiql.scribe.targets.spark

import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

object SparkTarget : SqlTarget() {
    override val target: String = "Spark"

    override val version: String = "3.1"

    override val dialect: SqlDialect = SparkDialect

    override val features: SqlFeatures = SparkFeatures

    override fun getCalls(onProblem: ProblemCallback): SqlCalls = SparkCalls(onProblem)

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan =
        SparkRewriter(onProblem).visitPartiQLPlan(plan, Unit) as PartiQLPlan

    private class SparkRewriter(val onProblem: ProblemCallback) : PlanRewriter<Unit>() {

        override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Unit): PlanNode {
            if ((node.statement !is Statement.Query) || (node.statement as Statement.Query).root.op !is Rex.Op.Select) {
                error("Spark does not support top level expression")
            }
            return super.visitPartiQLPlan(node, ctx)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): PlanNode {
            when (val type = node.constructor.type) {
                is StructType -> {
                    val open = !(type.contentClosed && type.constraints.contains(TupleConstraint.Open(false)))
                    val unordered = !type.constraints.contains(TupleConstraint.Ordered)
                    if (open || unordered) {
                        error("SELECT VALUE of open, unordered structs is NOT supported.")
                    }
                }
                else -> error("SELECT VALUE is NOT supported.")
            }
            return super.visitRexOpSelect(node, ctx)
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Unit): PlanNode {
            if (node.key.op !is Rex.Op.Lit) {
                error("Spark does not support path non-literal path expressions, found ${node.key.op}")
            }
            if (node.key.type !is StringType) {
                error("Spark path expression must be a string literal.")
            }
            return super.visitRexOpPathKey(node, ctx)
        }

        private fun error(message: String) {
            onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
        }
    }
}
