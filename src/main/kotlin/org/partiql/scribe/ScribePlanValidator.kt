package org.partiql.scribe

import org.partiql.plan.Fn
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.visitor.PlanBaseVisitor

object ScribePlanValidator : PlanBaseVisitor<Unit, ProblemCallback>() {

    override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) {}

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: ProblemCallback) {
        ctx.err(node.message)
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: ProblemCallback) {
        ctx.err(node.message)
    }

    private fun ProblemCallback.err(message: String) = this(ScribeProblem(ScribeProblem.Level.ERROR, message))
}
