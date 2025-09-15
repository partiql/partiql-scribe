package org.partiql.scribe.targets.spark

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

public open class SparkTarget : SqlTarget() {
    override val target: String = "Spark"

    override val version: String = "0"

    override val features: SqlFeatures = SparkFeatures()

    public companion object {
        @JvmStatic
        public val STANDARD: SparkTarget = SparkTarget()
    }

    override fun getAstToSql(context: ScribeContext): AstToSql = SparkAstToSql(context)

    override fun getCalls(context: ScribeContext): SqlCalls = SparkCalls(context)

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan {
        when (val action = plan.action) {
            is Action.Query -> {
                val rex = SparkRewriter(context).visit(action.rex, context) as Rex
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
        val transform = SparkPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
