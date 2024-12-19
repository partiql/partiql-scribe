package org.partiql.scribe.sql

public class SqlExpr(column: String) : SqlNode() {

    /**
     * TEMPORARY TO GET SCAFFOLDING
     */
    private val column: String = column

    override fun getChildren(): List<SqlNode> = emptyList()

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitExpr(this, ctx)
}