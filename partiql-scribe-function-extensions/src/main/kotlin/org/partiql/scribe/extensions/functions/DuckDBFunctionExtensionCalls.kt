package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
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

    // PartiQL drops the fractional second (`...05.6` -> `...05`, `1969-12-31 23:59:59.6` -> `-1`). `epoch` returns a
    // DOUBLE, which can't hold microseconds far from 1970 (`9999-12-31 23:59:59.999999` rounds up a second) and which
    // CAST to BIGINT rounds, so truncate to whole seconds first.
    private fun toUnixTime(args: SqlArgs): Expr =
        exprCast(exprCall(Identifier.regular("epoch"), listOf(truncateToSecond(args[0].expr))), DataType.BIGINT())

    /**
     * PartiQL's `hex_to_bigint` is `Long.parseLong(s, 16)`, which accepts a leading `-` or `+`. DuckDB parses a
     * `0x`-prefixed hex string when casting to an integer type, but not a signed one (`'-0xFF'` fails). So strip the
     * sign, parse the magnitude as UBIGINT, and negate through HUGEINT so `-8000000000000000` reaches BIGINT's minimum.
     * The final CAST to BIGINT rejects anything out of range, like `parseLong`.
     */
    private fun hexToBigInt(args: SqlArgs): Expr {
        val s = args[0].expr
        val sign = exprCall(Identifier.regular("left"), listOf(s, exprLit(Literal.intNum(1L))))
        val magnitude = exprCall(Identifier.regular("substring"), listOf(s, exprLit(Literal.intNum(2L))))

        fun hex(digits: Expr): Expr = exprCast(exprOperator("||", exprLit(Literal.string("0x")), digits), UBIGINT)
        val negative = exprOperator("-", null, exprCast(hex(magnitude), HUGEINT))
        val parsed =
            exprCase(
                null,
                listOf(
                    exprCaseBranch(exprOperator("=", sign, exprLit(Literal.string("-"))), negative),
                    exprCaseBranch(exprOperator("=", sign, exprLit(Literal.string("+"))), hex(magnitude)),
                ),
                hex(s),
            )
        return exprCast(parsed, DataType.BIGINT())
    }

    private companion object {
        val UBIGINT: DataType = DataType.USER_DEFINED(Identifier.regular("UBIGINT"))
        val HUGEINT: DataType = DataType.USER_DEFINED(Identifier.regular("HUGEINT"))
    }
}
