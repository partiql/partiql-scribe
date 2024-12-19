package org.partiql.scribe.sql

/**
 * This most closely resembles the query-specification of SQL-99 and the QueryBody.SFW from the PartiQL AST.
 *
 * Note that SQL splits the query-specification with a table expression, but SQL-99 does not use the table-expression
 * outside the context of a query specification so the table expression has been merged here for simplicity.
 * It is easy to move it out if needed; the PartiQL AST merges them.
 *
 * The LET clause is purposely omitted because it out-of-scope.
 */
public class SqlQuery private constructor(
    select: SqlSelection,
    from: SqlFrom? = null,
    // where: SqlWhere? = null,
    // groupBy: SqlGroupBy? = null,
    // having: SqlHaving? = null,
) : SqlNode() {

    public companion object {

        /**
         * Create a SELECT query without a FROM.
         */
        @JvmStatic
        public fun select(select: SqlSelection): SqlQuery = SqlQuery(select)

        /**
         * Create a SELECT query with a FROM.
         */
        @JvmStatic
        public fun selectFrom(select: SqlSelection, from: SqlFrom): SqlQuery = SqlQuery(select, from)

    }

    private val select: SqlSelection = select
    private val from: SqlFrom? = null
    // public val where: SqlWhere? = null
    // public val groupBy: SqlGroupBy? = null
    // public val having: SqlHaving? = null

    public fun getSelect(): SqlSelection = select

    public fun getFrom(): SqlFrom? = from

    override fun getChildren(): List<SqlNode> {
        TODO("Not yet implemented")
    }

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}