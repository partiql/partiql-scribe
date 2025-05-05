package org.partiql.scribe.targets.trino

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session

public open class TrinoTarget : SqlTarget() {
    override val target: String = "Redshift"

    override val version: String = "0"

    override val features: SqlFeatures = TrinoFeatures()

    public companion object {
        @JvmStatic
        public val STANDARD: TrinoTarget = TrinoTarget()
    }

    override val dialect: SqlDialect = TrinoDialect()

    override fun getCalls(context: ScribeContext): SqlCalls = TrinoCalls(context)

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan {
        TrinoRewriter(context)
        TODO()
    }

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform = TrinoPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
