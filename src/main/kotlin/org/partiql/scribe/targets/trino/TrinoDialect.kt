package org.partiql.scribe.targets.trino

import org.partiql.ast.Expr
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect

public object TrinoDialect : SqlDialect() {

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, head: SqlBlock): SqlBlock {
        return SqlBlock.Link(head, SqlBlock.Text(node.attribute.name.lowercase()))
    }
}
