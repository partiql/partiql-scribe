package org.partiql.scribe.targets.redshift

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session

public open class RedshiftTarget : SqlTarget() {
    override val target: String = "Redshift"

    override val version: String = "0"

    override val features: SqlFeatures = RedshiftFeatures()

    public companion object {
        @JvmStatic
        public val STANDARD: RedshiftTarget = RedshiftTarget()
    }

    override val dialect: SqlDialect = RedshiftDialect()

    override fun getCalls(context: ScribeContext): SqlCalls = RedshiftCalls(context)

    override fun rewrite(plan: Plan, context: ScribeContext): Plan {
        RedshiftRewriter(context)
        TODO()
    }

    override fun planToAst(newPlan: Plan, session: Session, context: ScribeContext): AstNode {
        val transform = RedshiftPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
