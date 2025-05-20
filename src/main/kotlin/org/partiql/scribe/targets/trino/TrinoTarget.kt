package org.partiql.scribe.targets.trino

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.plan.rex.Rex
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session

/**
 * Experimental Trino SQL transpilation target.
 */
public open class TrinoTarget : SqlTarget() {
    override val target: String = "Trino"

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
        when (val action = plan.action) {
            is Action.Query -> {
                val rex = TrinoRewriter(context).visit(action.rex, context) as Rex
                val query = Action.Query { rex }
                return Plan { query }
            }
            else ->
                context.getErrorListener().reportAndThrow(
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
        val transform = TrinoPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
