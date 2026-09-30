package org.partiql.scribe.targets.duckdb

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

/**
 * Experimental DuckDB SQL transpilation target.
 *
 * DuckDB's dialect is close to standard SQL, so most of the translation is shared with the other SQL targets. This
 * target was built by porting the Trino target (DuckDB and Trino agree on 1-based array indexing, `ARRAY[...]`
 * literals, `ROW(...)` construction, and most scalar functions) and then adapting the cases where DuckDB genuinely
 * diverges (e.g. `split` -> `string_split`).
 *
 * Known follow-ups that are not yet DuckDB-specialized (they currently inherit the ported Trino behavior): the
 * EXCLUDE collection-wildcard helpers (`cardinality`/`element_at`/`transform`), `IS MISSING` handling, and
 * `CLOB` -> `VARCHAR`. These are not exercised by the current test suite.
 */
public open class DuckDBTarget : SqlTarget() {
    override val target: String = "DuckDB"

    override val version: String = "0"

    override val features: SqlFeatures = DuckDBFeatures()

    public companion object {
        @JvmStatic
        public val STANDARD: DuckDBTarget = DuckDBTarget()
    }

    override fun getAstToSql(context: ScribeContext): AstToSql = DuckDBAstToSql(context)

    override fun getCalls(context: ScribeContext): SqlCalls = DuckDBCalls(context)

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan {
        when (val action = plan.action) {
            is Action.Query -> {
                val rex = DuckDBRewriter(context).visit(action.rex, context) as Rex
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
        val transform = DuckDBPlanToAst(session, getCalls(context), context)
        val astStatement = transform.apply(newPlan)
        return astStatement
    }
}
