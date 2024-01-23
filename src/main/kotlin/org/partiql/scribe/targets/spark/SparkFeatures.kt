package org.partiql.scribe.targets.spark

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

object SparkFeatures : SqlFeatures.Permissive() {
    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: ProblemCallback) =
        when(val statement = node.statement) {
            is Statement.Query -> {
                when (statement.root.op) {
                    is Rex.Op.Select -> visitChildren(node, ctx)
                    else -> ctx.error("Spark does not support top level expression")
                }
            }
        }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: ProblemCallback) = node.accept(ProjectionValidator, ctx)

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: ProblemCallback) {
        when (val type = node.constructor.type) {
            is StructType -> {
                // We don't need the assertion on that the struct key is a string literal here
                // because, when the key is not a string literal, we can not type the struct field.
                // the return the struct will be an open struct with no field.
                if (type.contentClosed && type.constraints.contains(TupleConstraint.Ordered)) {
                    visitChildren(node, ctx)
                } else {
                    ctx.error("SELECT VALUE of open, unordered structs is NOT supported.")
                }
            }

            else -> ctx.error("SELECT VALUE is NOT supported.")
        }
    }

    override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: ProblemCallback) {
        if (node.key.op !is Rex.Op.Lit) {
            ctx.error("Spark path step must an identifier, e.g. `x`.`y`")
        }
    }

    private object ProjectionValidator : PlanBaseVisitor<Unit, ProblemCallback>() {
        override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) = Unit

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: ProblemCallback) {
            super.visitRelOpProject(node, ctx)
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: ProblemCallback) {
            // Stops expression like a[CAST(b AS STRING)]
            if (node.key.op !is Rex.Op.Lit) {
                ctx.error("Spark path step must an identifier, e.g. `x`.`y`")
            }
        }

    }
}