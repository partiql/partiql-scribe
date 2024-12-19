package org.partiql.scribe.sql

/**
 * TEMPORARY WITH
 */
public class SqlWith : SqlNode() {

    override fun getChildren(): List<SqlNode> = emptyList()

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitWith(this, ctx)
}
