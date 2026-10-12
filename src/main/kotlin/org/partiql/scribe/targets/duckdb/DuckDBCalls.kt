package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.spi.types.PType

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
     * PartiQL `size(collection)` -> DuckDB `len(list)` / `cardinality(map)`
     */
    private fun sizeFn(args: SqlArgs): Expr {
        val call = collectionSize(args[0])
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `size` was replaced by DuckDB `${call.function.identifier.text}`",
            ),
        )
        return call
    }

    /**
     * PartiQL `cardinality(collection)` -> DuckDB `len(list)` / `cardinality(map)`
     */
    private fun cardinalityFn(args: SqlArgs): Expr = collectionSize(args[0])

    /**
     * PartiQL `exists(collection)` -> DuckDB `len(list) > 0` / `cardinality(map) > 0`
     */
    private fun existsFn(args: SqlArgs): Expr {
        val call = collectionSize(args[0])
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `exists` was replaced by DuckDB `${call.function.identifier.text}(...) > 0`",
            ),
        )
        return exprOperator(">", call, exprLit(Literal.intNum(0)))
    }

    /**
     * DuckDB's `cardinality` only accepts a MAP ("Cardinality can only operate on MAPs"), so a list (PartiQL
     * array/bag) uses `len` instead.
     */
    private fun collectionSize(arg: SqlArg): ExprCall {
        val name =
            when (arg.type.code()) {
                PType.ARRAY, PType.BAG -> "len"
                else -> "cardinality"
            }
        return exprCall(Identifier.regular(name), listOf(arg.expr)) as ExprCall
    }
}
