package org.partiql.scribe.analyzer

import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.Statement
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.scribe.sql.SqlQuery
import org.partiql.scribe.sql.SqlQueryExpr
import org.partiql.scribe.sql.SqlSelect
import org.partiql.scribe.sql.SqlStatement

/**
 * The [SqlAnalyzer] is responsible for converting a PartiQL AST into typed Scribe IR (SqlNode).
 *
 * Notes,
 *  - all private methods use "ast" as the parameter name because local variables won't be named ast.
 */
public class SqlAnalyzer {

    /**
     * Analyzes a PartiQL statement and returns typed Scribe IR.
     */
    public fun analyze(statement: Statement): SqlStatement {
        // convert
        val ir = when (statement) {
            is Query -> analyze(statement)
            else -> error("Unsupported statement type: $statement")
        }
        // apply additional analyzer rules
        return ir
    }

    /**
     * Each query introduces its own scope.
     */
    private fun analyze(ast: Query): SqlSelect {
        if (ast.expr !is ExprQuerySet) {
            error("Unsupported top-level expression query.")
        }
        TODO()
    }

    /**
     *
     */
    private fun analyze(ast: ExprQuerySet): SqlSelect {

        if (ast.orderBy != null) {
            error("Unsupported ORDER BY clause.")
        }
        if (ast.limit != null) {
            error("Unsupported LIMIT clause.")
        }
        if (ast.offset != null) {
            error("Unsupported OFFSET clause.")
        }

        //
        val expr = analyze(ast.body)
        val query = SqlQuery.query(expr)

        return SqlSelect.query(query)
    }

    /**
     */
    private fun analyze(ast: QueryBody): SqlQueryExpr {
        TODO()
    }
}
