package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.targets.duckdb.DuckDBCalls
import org.partiql.scribe.targets.duckdb.DuckDBTarget

/**
 * Experimental DuckDB target with PartiQL function extension support. See [DuckDBTarget].
 */
public class DuckDBFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : DuckDBTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = DuckDBFunctionExtensionCalls(context, routines)
}

/**
 * Experimental DuckDB translations for explicitly bound PartiQL function extensions.
 *
 * `date_add('<part>', ...)` is handled by [commonRules], which retargets it to [DuckDBCalls.dateAdd].
 */
public open class DuckDBFunctionExtensionCalls(
    private val context: ScribeContext,
    routines: Set<RoutineRef>,
) : DuckDBCalls(context) {
    public override val routineRules: Map<RoutineRef, SqlCallFn> =
        bind(
            routines,
            commonRules(this, context.getProblemListener()) +
                mapOf(
                    CONTAINS to ::contains,
                    TO_UNIXTIME to ::toUnixTime,
                    HEX_TO_BIGINT to ::hexToBigInt,
                ),
        )

    // https://duckdb.org/docs/current/sql/functions/list.html#containslist-element
    private fun contains(args: SqlArgs): Expr = exprCall(Identifier.regular("contains"), args.map { it.expr })

    // `epoch` returns seconds as a DOUBLE, and DuckDB's CAST to BIGINT rounds. PartiQL truncates toward negative
    // infinity, so `floor` first (`...05.6` -> `...05`, `1969-12-31 23:59:59.6` -> `-1`, not `0`).
    private fun toUnixTime(args: SqlArgs): Expr =
        exprCast(
            exprCall(
                Identifier.regular("floor"),
                listOf(exprCall(Identifier.regular("epoch"), args.map { it.expr })),
            ),
            DataType.BIGINT(),
        )

    // DuckDB parses a `0x`-prefixed hex string when casting to an integer type.
    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCast(
            exprOperator("||", exprLit(Literal.string("0x")), args[0].expr),
            DataType.BIGINT(),
        )
}
