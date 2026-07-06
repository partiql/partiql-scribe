package org.partiql.scribe.targets.partiql

import org.partiql.ast.AstNode
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session

/**
 * Default PartiQL Target does nothing as there is no need to rewrite the plan.
 */
public object PartiQLTarget : SqlTarget() {
    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override val features: SqlFeatures = SqlFeatures.Permissive()

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan = plan

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform =
            object : PlanToAst(session, getCalls(context), context) {
                override fun getRexConverter(locals: Locals): RexConverter {
                    return PartiQLRexConverter(this, locals, context)
                }
            }
        return transform.apply(newPlan)
    }
}
