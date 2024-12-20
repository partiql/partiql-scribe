package org.partiql.scribe.sql

/**
 * This is the SELECT clause variant.
 *
 * ```
 * SELECT <select star>
 * SELECT <select list>
 * SELECT VALUE <constructor>
 * PIVOT <expr> AT <expr>
 * ```
 */
public sealed class SqlSelect : SqlNode()

/**
 * ```
 * <select> <asterisk>
 * ```
 */
public class SqlSelectStar : SqlSelect() {

    override fun getChildren(): List<SqlNode> = emptyList()

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitSelectStar(this, ctx)
}

/**
 * ```
 * <select item> [, <select item>]*
 * ```
 */
public class SqlSelectList(items: List<SqlSelectItem>) : SqlSelect() {

    private val items: List<SqlSelectItem> = items

    override fun getChildren(): List<SqlNode> = items

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}

/**
 * ```
 * <expr> [AS] <alias>
 * ```
 */
public class SqlSelectItem(expr: SqlExpr, alias: SqlName) : SqlNode() {

    private val expr: SqlExpr = expr

    private val alias: SqlName = alias

    public fun getExpr(): SqlExpr = expr

    public fun getAlias(): SqlName = alias

    override fun getChildren(): List<SqlNode> = listOf(expr, alias)

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitSelectItem(this, ctx)
}

/**
 * ```
 * SELECT VALUE <constructor>
 * ```
 */
public class SqlSelectValue(constructor: SqlExpr) : SqlSelect() {

    public val constructor: SqlExpr = constructor

    override fun getChildren(): List<SqlNode> = TODO("Not yet implemented")

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}

/**
 * ```
 * PIVOT value=<expr> AT key=<expr>
 * ```
 */
public class SqlSelectPivot(key: SqlExpr, value: SqlExpr) : SqlSelect() {

    /**
     * AT key=<expr>
     */
    private val key: SqlExpr = key

    /**
     * PIVOT value=<expr>
     */
    private val value: SqlExpr = value

    public fun getKey(): SqlExpr = key

    public fun getValue(): SqlExpr = value

    override fun getChildren(): List<SqlNode> = TODO("Not yet implemented")

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R =  visitor.visitSelectPivot(this, ctx)
}