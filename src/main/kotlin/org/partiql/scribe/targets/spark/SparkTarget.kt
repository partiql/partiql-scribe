package org.partiql.scribe.targets.spark

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session

public open class SparkTarget : SqlTarget() {
    override val target: String = "Redshift"

    override val version: String = "0"

    override val features: SqlFeatures = SparkFeatures()

    public companion object {
        @JvmStatic
        public val STANDARD: SparkTarget = SparkTarget()
    }

    override val dialect: SqlDialect = SparkDialect()

    override fun getCalls(context: ScribeContext): SqlCalls = SparkCalls(context)

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan {
        SparkRewriter(context)
        return plan
    }

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform = SparkPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
