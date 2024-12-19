package org.partiql.scribe.sql

/**
 * SqlDialect represents the base behavior for transforming an [SqlNode] tree into a [SqlBlock] tree for layout.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
public abstract class SqlDialect : SqlVisitor<SqlBlock, SqlBlock>() {

    /**
     * Default entry-point, can also be us.
     */
    public fun apply(node: SqlNode): SqlBlock {
        val head = SqlBlock.root()
        val tail = head
        node.accept(this, tail)
        return head
    }

    // override fun defaultReturn(node: SqlNode, tail: SqlBlock): SqlBlock =
    //     throw UnsupportedOperationException("Cannot print $node")

    // IDENTIFIERS & NAMES

    // public override fun visitIdentifier(node: SqlIdentifier, tail: SqlBlock): SqlBlock {
    //     return defaultVisit(node, tail)
    // }

    public override fun visitName(node: SqlName, tail: SqlBlock): SqlBlock {
        return tail .. node.getName()
    }

    // STATEMENTS

    public override fun visitStatement(node: SqlStatement, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    public override fun visitSelect(node: SqlSelect, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    //  [DML placeholder]

    //  [DDL placeholder]

    // QUERY

    public override fun visitQuery(node: SqlQuery, ctx: SqlBlock): SqlBlock {
        return super.visitQuery(node, ctx)
    }

    public override fun visitQueryExpr(node: SqlQueryExpr, ctx: SqlBlock): SqlBlock {
        return super.visitQueryExpr(node, ctx)
    }

    public override fun visitQueryBody(node: SqlQueryBody, ctx: SqlBlock): SqlBlock {
        return super.visitQueryBody(node, ctx)
    }

    public override fun visitQuerySpec(node: SqlQuerySpec, ctx: SqlBlock): SqlBlock {
        return super.visitQuerySpec(node, ctx)
    }

    public override fun visitWith(node: SqlWith, ctx: SqlBlock): SqlBlock {
        return super.visitWith(node, ctx)
    }

    public override fun visitSelection(node: SqlSelection, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
    }

    public override fun visitSelectStar(node: SqlSelectStar, tail: SqlBlock): SqlBlock {
        return defaultVisit(node, tail)
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
}
