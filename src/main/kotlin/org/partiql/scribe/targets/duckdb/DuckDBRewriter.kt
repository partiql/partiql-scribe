package org.partiql.scribe.targets.duckdb

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.scribe.ScribeContext

/**
 * Plan -> plan rewriter for the DuckDB target.
 *
 * Currently minimal: it drops standalone [RelExclude] nodes feeding a [RelProject] (matching the other
 * SQL targets) and otherwise passes the plan through unchanged. DuckDB-specific rewrites (array index
 * shifting, BAG -> ARRAY, IS MISSING -> IS NULL, etc.) will be added here.
 */
public open class DuckDBRewriter(internal val context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    override fun visitProject(
        rel: RelProject,
        ctx: ScribeContext?,
    ): Operator {
        val input = rel.input
        val newNode =
            if (input is RelExclude) {
                val newProject = RelProject.create(input.input, rel.projections)
                newProject.type = rel.type
                newProject
            } else {
                rel
            }
        return super.visitProject(newNode, ctx)
    }
}
