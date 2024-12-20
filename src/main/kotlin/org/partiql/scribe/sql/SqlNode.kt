package org.partiql.scribe.sql

/**
 * Abstract base class for all Scribe IR nodes.
 */
public sealed class SqlNode {

    private var tag: Int = 0

    public fun getTag(): Int {
        return tag
    }

    public fun setTag(tag: Int) {
        this.tag = tag
    }

    public abstract fun getChildren(): List<SqlNode>

    public abstract fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R

    /**
     * Pretty-print this [SqlNode] as SQL text with the given [SqlLayout]
     */
    @JvmOverloads
    public fun sql(layout: SqlLayout = SqlLayout.DEFAULT, dialect: SqlDialect = SqlDialect.DEFAULT): String {
        return dialect.apply(this).sql(layout)
    }
}
