package org.partiql.scribe.sql

/**
 * SqlDialect represents the base behavior for transforming an [SqlNode] tree into a [SqlBlock] tree for layout.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
public abstract class SqlDialect : SqlVisitor<SqlBlock, SqlBlock>() {

    // @formatter:off

    public companion object {

        @JvmField
        public val DEFAULT: SqlDialect = object : SqlDialect() {}
    }

    /**
     * Default entry-point, can also be use for non-parenthesized queries.
     */
    public fun apply(node: SqlNode): SqlBlock {
        val head = SqlBlock.root()
        val tail = head
        node.accept(this, tail)
        return head
    }

    /**
     * Default behavior is to parenthesize all query expressions.
     *
     * @param node
     * @param tail
     */
    public open fun visitExprWrapped(node: SqlExpr, tail: SqlBlock): SqlBlock = when (node) {
        // is ExprQuery -> {
        //     var t = tail
        //     t = t .. "(" .. visitQuery(node, t) ..  ")"
        //     t
        // }
        else -> visitExpr(node, tail)
    }

    public override fun defaultReturn(node: SqlNode, tail: SqlBlock): SqlBlock =
        throw UnsupportedOperationException("Cannot print $node")

    // IDENTIFIERS & NAMES

    // public override fun visitIdentifier(node: SqlIdentifier, tail: SqlBlock): SqlBlock {
    //     return defaultVisit(node, tail)
    // }

    public override fun visitName(node: SqlName, tail: SqlBlock): SqlBlock {
        return tail .. node.getName()
    }

    // STATEMENTS

    public override fun visitStatement(node: SqlStatement, tail: SqlBlock): SqlBlock = when (node) {
        is SqlStatementSelect -> visitStatementSelect(node, tail)
    }

    public override fun visitStatementSelect(node: SqlStatementSelect, tail: SqlBlock): SqlBlock {
        return visitQuery(node.getQuery(), tail)
    }

    //  [DML placeholder]

    //  [DDL placeholder]

    // QUERY

    public override fun visitQuery(node: SqlQuery, tail: SqlBlock): SqlBlock {
        var t = tail
        // BODY
        t = visitQueryExpr(node.getQuery(), t)
        // // ORDER BY
        // t = if (node.orderBy != null) visitOrderBy(node.orderBy, t .. " ") else t
        // // LIMIT
        // t = if (node.limit != null) expr(node.limit, t .. " LIMIT ") else t
        // // OFFSET
        // t = if (node.offset != null) expr(node.offset, t .. " OFFSET ") else t
        return t
    }

    public override fun visitQueryExpr(node: SqlQueryExpr, tail: SqlBlock): SqlBlock {
        var t = tail
        // WITH
        t = visitOptional(node.getWith(), t) {  visitWith(it, t .. "WITH ")}
        // BODY
        t = visitQueryBody(node.getBody(), t)
        return t
    }

    public override fun visitQuerySpec(node: SqlQuerySpec, tail: SqlBlock): SqlBlock {
        var t = tail
        // SELECT
        t = visitSelect(node.getSelect(), t)
        // FROM
        t = visitOptional(node.getFrom(), t) { visitFrom(it, t .. " FROM ") }
        // // WHERE
        // t = if (node.getWhere() != null) visitExpr(node.getWhere()!!, t .. " WHERE ") else t
        // // GROUP BY
        // t = if (node.getGroupBy() != null) visitExpr(node.getGroupBy()!!, t .. " GROUP BY ") else t
        // // HAVING
        // t = if (node.getHaving() != null) visitExpr(node.getHaving()!!, t .. " HAVING ") else t
        // // WINDOW
        // t = if (node.getWindow() != null) visitExpr(node.getWindow()!!, t .. " WINDOW ") else t
        return t
    }

    public override fun visitWith(node: SqlWith, tail: SqlBlock): SqlBlock {
        return super.visitWith(node, tail)
    }

    public override fun visitSelectStar(node: SqlSelectStar, tail: SqlBlock): SqlBlock {
        return tail .. "SELECT *"
    }

    public override fun visitSelectList(node: SqlSelectList, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    public override fun visitSelectItem(node: SqlSelectItem, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    public override fun visitSelectValue(node: SqlSelectValue, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    public override fun visitSelectPivot(node: SqlSelectPivot, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    // FROM

    public override fun visitFrom(node: SqlFrom, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t .. node.getTable()
        return t
    }

    // EXPRESSIONS

    public override fun visitExpr(node: SqlExpr, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    /**
     * Inline this ugly pattern for optional nodes.
     */
    public inline fun <reified T : SqlNode> visitOptional(node: T?, tail: SqlBlock, visit: (T) -> SqlBlock): SqlBlock {
        return if (node != null) visit(node) else tail
    }
}
