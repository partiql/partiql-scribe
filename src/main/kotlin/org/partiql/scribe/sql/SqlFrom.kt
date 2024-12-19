package org.partiql.scribe.sql

/**
 * TEMPORARY
 */
public class SqlFrom(table: String) : SqlNode() {

    /**
     * FROM <table>
     */
    private val table: String = table

    public fun getTable(): String {
        return table
    }

    override fun getChildren(): List<SqlNode> {
        TODO("Not yet implemented")
    }

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}
