package org.partiql.scribe.sql

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.plan.rex.Rex
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.ScribeOutput
import org.partiql.scribe.ScribeTag
import org.partiql.scribe.ScribeTarget
import org.partiql.spi.catalog.Session

/**
 * Base [ScribeTarget] for SQL dialects.
 */
public abstract class SqlTarget : ScribeTarget<String> {
    public open fun getAstToSql(context: ScribeContext): AstToSql = object : AstToSql(context) {}

    public open val layout: SqlLayout = SqlLayout.STANDARD

    public open val features: SqlFeatures = SqlFeatures.Defensive()

    public open fun getCalls(context: ScribeContext): SqlCalls = SqlCalls.standard(context)

    public abstract fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan

    override fun compile(
        plan: Plan,
        session: Session,
        context: ScribeContext,
    ): ScribeOutput<String> {
        // 1st validate the features used in the provided plan
        features.validate(plan, context)
        // 2nd rewrite plan according to plan -> plan' rewrites; get schema from the plan
        val newPlan = rewrite(plan, context)
        val topLevelRexType = newPlan.topLevelRex().type
        val schema = topLevelRexType.pType
        // 3rd convert plan into ast
        val newAst = planToAst(newPlan, session, context)
        // 4th convert ast into sql text
        val block = getAstToSql(context).transform(newAst)
        val sql = block.sql(layout)
        // 5th generate final output
        val tag = ScribeTag()
        return SqlOutput(tag, sql, schema)
    }

    public open fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform = PlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }

    private fun Plan.topLevelRex(): Rex {
        return ((this.action) as Action.Query).rex
    }
}
