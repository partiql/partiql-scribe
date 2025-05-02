package org.partiql.scribe.sql

import org.partiql.ast.AstNode
import org.partiql.plan.PartiQLPlan
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeOutput
import org.partiql.scribe.ScribeTag
import org.partiql.scribe.ScribeTarget
import org.partiql.scribe.VersionProvider
import org.partiql.types.StaticType
import org.partiql.plan.Statement as PlanStatement

public class SqlOutput(
    tag: ScribeTag,
    value: String,
    schema: StaticType,
) : ScribeOutput<String>(tag, value, schema) {

    override fun toString(): String = value

    override fun toDebugString(): String = buildString {
        appendLine("SQL: ")
        appendLine(value)
        appendLine()
        appendLine("Schema: ")
        appendLine(schema)
    }
}

/**
 * This is a base [ScribeTarget] for SQL dialects.
 */
public abstract class SqlTarget : ScribeTarget<String> {

    /**
     * Default SQL dialect for AST -> SQL.
     */
    open val dialect: SqlDialect = SqlDialect.PARTIQL

    /**
     * Default SQL formatting layout.
     */
    open val layout: SqlLayout = SqlLayout.DEFAULT

    /**
     * Default validator after planning. This is invoked after planning and before the [rewrite].
     *
     * @see [SqlFeatures]
     */
    open val features: SqlFeatures = SqlFeatures.Defensive()

    /**
     * Default SQL call transformation logic.
     */
    open fun getCalls(onProblem: ProblemCallback) = SqlCalls.DEFAULT

    /**
     * Entry-point for manipulations of the [PartiQLPlan] tree.
     */
    abstract fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan

    /**
     * Apply the plan rewrite, then use the given [SqlDialect] to output SQL text.
     */
    override fun compile(plan: PartiQLPlan, onProblem: ProblemCallback): ScribeOutput<String> {
        features.validate(plan, onProblem)
        val newPlan = rewrite(plan, onProblem)
        if (newPlan.statement !is PlanStatement.Query) {
            error("Scribe currently only supports query statements")
        }
        val schema = (newPlan.statement as PlanStatement.Query).root.type
        val newAst = unplan(newPlan, onProblem)
        val block = dialect.apply(newAst)
        val sql = block.sql(layout)

        val tag = ScribeTag(
            scribeVersion = VersionProvider.version,
            scribeCommit = VersionProvider.commit,
            target = this.target,
            targetVersion = this.version,
        )

        return SqlOutput(tag, sql, schema)
    }

    /**
     * Default Plan to AST translation. This method is only for potential edge cases
     */
    open fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): AstNode {
        val transform = SqlTransform(plan.catalogs, getCalls(onProblem), onProblem)
        val statement = transform.apply(plan.statement)
        return statement
    }
}
