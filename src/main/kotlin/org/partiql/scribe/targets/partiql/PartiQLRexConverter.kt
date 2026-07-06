package org.partiql.scribe.targets.partiql

import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.plan.rex.RexPathKey
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter

public open class PartiQLRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context) {
    override fun visitPathKey(
        rex: RexPathKey,
        ctx: Unit,
    ): Expr {
        val prev = visitRex(rex.operand, ctx)
        val step = exprPathStepElement(visitRex(rex.key, ctx))
        return if (prev is ExprPath) {
            exprPath(prev.root, prev.steps + step)
        } else {
            exprPath(prev, listOf(step))
        }
    }
}
