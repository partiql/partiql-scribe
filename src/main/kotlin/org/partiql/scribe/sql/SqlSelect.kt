package org.partiql.scribe.sql

/**
 * The SELECT statement can be a naked SELECT (most DBs support this), an expression (PartiQL only) or a normal query.
 *
 * ```
 * <select>
 *      ::= SELECT <select list>
 *        | <expression>
 *        | <query>
 * ```
 */
public class SqlSelect(query: SqlQuery) : SqlStatement() {

    private val query: SqlQuery = query

    public fun getQuery(): SqlQuery = query

    override fun getChildren(): List<SqlNode> = listOf(query)

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitSelect(this, ctx)

}

/**
 * This is the actual SELECT clause selection variant.
 *
 * ```
 * SELECT <select star>
 * SELECT <select list>
 * PIVOT <expr> AT <expr>
 * ```
 */
public abstract class SqlSelection : SqlNode()

/**
 * ```
 * *
 * ```
 */
public abstract class SqlSelectStar : SqlSelection()

/**
 * ```
 * <select item> [, <select item>]*
 * ```
 */
public class SqlSelectList(items: List<SqlSelectItem>) : SqlSelection() {

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
public class SqlSelectValue(constructor: SqlExpr) : SqlSelection() {

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
public class SqlSelectPivot(key: SqlExpr, value: SqlExpr) : SqlSelection() {

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