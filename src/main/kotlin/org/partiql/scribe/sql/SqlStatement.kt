package org.partiql.scribe.sql

/**
 * A general SQL statement such as SELECT, INSERT/UPDATE/DELETE, CREATE TABLE, etc.
 */
public sealed class SqlStatement : SqlNode()

/**
 * The SELECT statement can be a naked SELECT (most DBs support this), an expression (PartiQL only) or a query.
 *
 * ```
 * // current
 * <select statement> ::= <query>
 *
 * // future
 * <select>
 *      ::= SELECT <select list>
 *        | <expression>
 *        | <query>
 * ```
 *
 * For now, I just model <cursor specification> (aka <query>) from SQL-99 as the "SELECT" statement.
 */
public class SqlStatementSelect private constructor(query: SqlQuery) : SqlStatement() {

    public companion object {

        /**
         * Create a SELECT statement from a query.
         */
        @JvmStatic
        public fun query(query: SqlQuery): SqlStatementSelect = SqlStatementSelect(query)
    }

    private val query: SqlQuery = query

    public fun getQuery(): SqlQuery = query

    override fun getChildren(): List<SqlNode> = listOf(query)

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitStatementSelect(this, ctx)

}

