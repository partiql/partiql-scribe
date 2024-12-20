package org.partiql.scribe.sql

/**
 * The [SqlQuery] node is most like the <cursor specification> of SQL-99.
 *
 * ```
 * <cursor specification>
 *      ::= <query expression> [ <order by clause> ] [ <limit clause> ] [ <offset clause ]
 * ```
 *
 * Note: both limit and offset are not SQL-99 standard.
 */
public class SqlQuery private constructor(query: SqlQueryExpr) : SqlNode() {

    public companion object {

        @JvmStatic
        public fun query(query: SqlQueryExpr): SqlQuery = SqlQuery(query)
    }

    private val query: SqlQueryExpr = query

    // private val orderBy: SqlOrderBy? = null
    // private val limit: SqlLimit? = null
    // private val offset: SqlOffset? = null

    public fun getQuery(): SqlQueryExpr = query

    override fun getChildren(): List<SqlNode> = listOfNotNull(query)

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitQuery(this, ctx)
}

/**
 * TODO
 */
public class SqlQueryExpr private constructor(with: SqlWith?, body: SqlQueryBody) : SqlNode() {

    public companion object {

        @JvmStatic
        public fun with(with: SqlWith, body: SqlQueryBody): SqlQueryExpr = SqlQueryExpr(with, body)

        @JvmStatic
        public fun query(body: SqlQueryBody): SqlQueryExpr = SqlQueryExpr(null, body)
    }

    private val with: SqlWith? = null

    private val body: SqlQueryBody = body

    public fun getWith(): SqlWith? = with

    public fun getBody(): SqlQueryBody = body

    override fun getChildren(): List<SqlNode> = listOfNotNull(with, body)

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitQueryExpr(this, ctx)
}

/**
 * This is the <query expression body> from the SQL-99 grammar.
 */
public sealed class SqlQueryBody : SqlNode()

/**
 * This most closely resembles the query-specification of SQL-99 and the QueryBody.SFW from the PartiQL AST.
 *
 * Note that SQL splits the query-specification with a table expression, but SQL-99 does not use the table-expression
 * outside the context of a query specification so the table expression has been merged here for simplicity.
 * It is easy to move it out if needed; the PartiQL AST merges them.
 *
 * The LET clause is purposely omitted because it out-of-scope.
 */
public class SqlQuerySpec private constructor(
    private val select: SqlSelect,
    private val from: SqlFrom? = null,
    // private val where: SqlWhere? = null,
    // private val groupBy: SqlGroupBy? = null,
    // private val having: SqlHaving? = null,
) : SqlQueryBody() {

    public companion object {

        /**
         * Create a SELECT query without a FROM.
         */
        @JvmStatic
        public fun select(select: SqlSelect): SqlQuerySpec = SqlQuerySpec(select)

        /**
         * Create a SELECT query with a FROM.
         */
        @JvmStatic
        public fun selectFrom(select: SqlSelect, from: SqlFrom): SqlQuerySpec = SqlQuerySpec(select, from)
    }

    public fun getSelect(): SqlSelect = select

    public fun getFrom(): SqlFrom? = from

    override fun getChildren(): List<SqlNode> {
        TODO("Not yet implemented")
    }

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}
