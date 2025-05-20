package org.partiql.scribe.targets.redshift

import org.partiql.ast.AstNode
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.plan.rex.Rex
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
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

    override fun getAstToSql(context: ScribeContext): AstToSql = RedshiftAstToSql(context)

    override fun getCalls(context: ScribeContext): SqlCalls = RedshiftCalls(context)

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan {
        when (val action = plan.action) {
            is Action.Query -> {
                val rex = RedshiftRewriter(context).visit(action.rex, context) as Rex
                val query = Action.Query { rex }
                return Plan { query }
            }
            else ->
                context.getProblemListener().reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "Can only translate a query statement. Received $action",
                    ),
                )
        }
    }

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform = RedshiftPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
