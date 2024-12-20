package org.partiql.scribe.analyzer

import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.Statement
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.scribe.sql.SqlFrom
import org.partiql.scribe.sql.SqlQuery
import org.partiql.scribe.sql.SqlQueryExpr
import org.partiql.scribe.sql.SqlQuerySpec
import org.partiql.scribe.sql.SqlStatementSelect
import org.partiql.scribe.sql.SqlSelectStar
import org.partiql.scribe.sql.SqlStatement

/**
 * The [SqlAnalyzer] is responsible for converting a PartiQL AST into typed Scribe IR (SqlNode).
 *
 * Notes,
 *  - all private methods use "ast" as the parameter name because local variables won't be named ast.
 *  - current JUST a translation with checks in the interest of time...
 */
public class SqlAnalyzer {

    /**
     * Analyzes a PartiQL statement and returns typed Scribe IR.
     */
    public fun analyze(statement: Statement): SqlStatement {
        // convert
        val ir = when (statement) {
            is Query -> analyzeQuery(statement)
            else -> error("Unsupported statement type: $statement")
        }
        // apply additional analyzer rules
        return ir
    }

    /**
     * Each query introduces its own scope (not their yet...)
     */
    private fun analyzeQuery(ast: Query): SqlStatementSelect {
        if (ast.expr is ExprQuerySet) {
            return analyzeExprQuerySet(ast.expr as ExprQuerySet)
        }
        error("Unsupported top-level expression query.")
    }

    /**
     * The PartiQL ExprQuerySet is the top-level expression query.
     */
    private fun analyzeExprQuerySet(ast: ExprQuerySet): SqlStatementSelect {
        if (ast.orderBy != null) {
            error("Unsupported ORDER BY clause.")
        }
        if (ast.limit != null) {
            error("Unsupported LIMIT clause.")
        }
        if (ast.offset != null) {
            error("Unsupported OFFSET clause.")
        }
        val expr = analyzeQueryBody(ast.body)
        val query = SqlQuery.query(expr)
        return SqlStatementSelect.query(query)
    }

    /**
     * The PartiQL QueryBody is a set-op of SFW; it's modeled like this parsing precedence.
     */
    private fun analyzeQueryBody(ast: QueryBody): SqlQueryExpr {
        if (ast is QueryBody.SFW) {
            return analyzeQueryBodySFW(ast)
        }
        error("Unsupported QueryBody")
    }

    /**
     * The PartiQL SELECT-FROM-WHERE QueryBody.
     */
    private fun analyzeQueryBodySFW(ast: QueryBody.SFW): SqlQueryExpr {
        if (ast.exclude != null) {
            error("Unsupported EXCLUDE clause.")
        }
        if (ast.let != null) {
            error("Unsupported LET clause.")
        }
        if (ast.where != null) {
            error("Unsupported WHERE clause.")
        }
        if (ast.groupBy != null) {
            error("Unsupported GROUP BY clause.")
        }
        if (ast.having != null) {
            error("Unsupported HAVING clause.")
        }

        // TODO implement me
        val selection = SqlSelectStar()
        val from = SqlFrom("example")
        val body = SqlQuerySpec.selectFrom(selection, from)

        return SqlQueryExpr.query(body)
    }
}
