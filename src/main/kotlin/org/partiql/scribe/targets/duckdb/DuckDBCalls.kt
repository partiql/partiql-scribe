package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls

public open class DuckDBCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["map_contains_key"] = ::mapContainsKey
            this["map_get"] = ::mapGet
            this["size"] = ::sizeFn
            this["cardinality"] = ::cardinalityFn
            this["exists"] = ::existsFn
        }

    /**
     * PartiQL `map_contains_key(map, key)` -> DuckDB `map_contains(map, key)`
     */
    private fun mapContainsKey(args: SqlArgs): Expr {
        val mapContainsId = Identifier.regular("map_contains")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_contains_key` was replaced by DuckDB `map_contains(map, key)`.",
            ),
        )
        val mapExpr = args[0].expr
        val keyExpr = args[1].expr
        return exprCall(mapContainsId, listOf(mapExpr, keyExpr))
    }

    /**
     * PartiQL `map_get(map, key)` -> DuckDB `map_extract_value(map, key)`
     *
     * DuckDB's `element_at` / `map_extract` return a LIST (`[value]`, or `[]` for a missing key); `map_extract_value`
     * returns the value itself (NULL for a missing key), matching PartiQL `map_get`.
     */
    private fun mapGet(args: SqlArgs): Expr {
        val id = Identifier.regular("map_extract_value")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `map_get` was replaced by DuckDB `map_extract_value`",
            ),
        )
        return exprCall(id, listOf(args[0].expr, args[1].expr))
    }

    /**
     * PartiQL `size(collection)` -> DuckDB `cardinality(collection)`
     */
    private fun sizeFn(args: SqlArgs): Expr {
        val id = Identifier.regular("cardinality")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `size` was replaced by DuckDB `cardinality`",
            ),
        )
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * PartiQL `cardinality(collection)` -> DuckDB `cardinality(collection)` (native)
     */
    private fun cardinalityFn(args: SqlArgs): Expr {
        val id = Identifier.regular("cardinality")
        return exprCall(id, listOf(args[0].expr))
    }

    /**
     * PartiQL `exists(collection)` -> DuckDB `cardinality(collection) > 0`
     */
    private fun existsFn(args: SqlArgs): Expr {
        val cardId = Identifier.regular("cardinality")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `exists` was replaced by DuckDB `cardinality(...) > 0`",
            ),
        )
        val cardCall = exprCall(cardId, listOf(args[0].expr))
        return exprOperator(">", cardCall, exprLit(Literal.intNum(0)))
    }
}
