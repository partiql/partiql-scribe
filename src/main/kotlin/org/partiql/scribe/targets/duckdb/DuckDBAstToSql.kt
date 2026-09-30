package org.partiql.scribe.targets.duckdb

import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.sql.SqlBlock
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat

/**
 * AST -> SQL text rendering for the DuckDB dialect.
 *
 * Currently only lowercases session attributes (e.g. CURRENT_USER); DuckDB-specific type and literal
 * rendering will be added as features are implemented.
 */
public open class DuckDBAstToSql(context: ScribeContext) : AstToSql(context) {
    override fun visitExprSessionAttribute(
        node: ExprSessionAttribute,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat node.sessionAttribute.name().lowercase()
    }
}
