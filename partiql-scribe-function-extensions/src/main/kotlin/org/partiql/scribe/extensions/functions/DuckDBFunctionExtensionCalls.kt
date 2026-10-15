package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.targets.duckdb.DuckDBCalls
import org.partiql.scribe.targets.duckdb.DuckDBTarget
import org.partiql.spi.types.PType
import java.util.Locale

public class DuckDBFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : DuckDBTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = DuckDBFunctionExtensionCalls(context, routines)
}

/**
 * DuckDB translations for explicitly bound PartiQL function extensions.
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
                    DATE_ADD to ::dateAdd,
                    CONTAINS to ::contains,
                    TO_UNIXTIME to ::toUnixTime,
                    HEX_TO_BIGINT to ::hexToBigInt,
                ),
        )

    /**
     * `date_add('<part>', <amount>, <datetime>)` -> `date_add(<datetime>, to_<part>s(<amount>))`, e.g.
     * `date_add('day', 1, ts)` -> `date_add(ts, to_days(1))`. Unlike the built-in `DATE_ADD`, the part is a string
     * literal. DuckDB's `date_add` on a DATE returns a TIMESTAMP, so a DATE operand is cast back to DATE.
     */
    private fun dateAdd(args: SqlArgs): Expr {
        val listener = context.getProblemListener()
        val part = (args.firstOrNull()?.expr as? ExprLit)?.lit
        if (args.size != 3 || part?.code() != Literal.STRING) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Function extension `date_add` requires a string datetime part, quantity, and timestamp",
                ),
            )
        }
        val fn =
            when (part.stringValue().lowercase(Locale.ROOT)) {
                "year" -> "to_years"
                "month" -> "to_months"
                "day" -> "to_days"
                "hour" -> "to_hours"
                "minute" -> "to_minutes"
                "second" -> "to_seconds"
                else ->
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            ScribeProblem.UNSUPPORTED_OPERATION,
                            "Unsupported datetime part for function extension `date_add`: ${part.stringValue()}",
                        ),
                    )
            }
        val datetime = args[2]
        val call =
            exprCall(
                Identifier.regular("date_add"),
                listOf(datetime.expr, exprCall(Identifier.regular(fn), listOf(args[1].expr))),
            )
        return if (datetime.type.code() == PType.DATE) exprCast(call, DataType.DATE()) else call
    }

    // https://duckdb.org/docs/current/sql/functions/list.html#containslist-element
    private fun contains(args: SqlArgs): Expr = exprCall(Identifier.regular("contains"), args.map { it.expr })

    // `epoch` returns seconds as a DOUBLE; cast to BIGINT like the other targets.
    private fun toUnixTime(args: SqlArgs): Expr =
        exprCast(
            exprCall(Identifier.regular("epoch"), args.map { it.expr }),
            DataType.BIGINT(),
        )

    // DuckDB parses a `0x`-prefixed hex string when casting to an integer type.
    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCast(
            exprOperator("||", exprLit(Literal.string("0x")), args[0].expr),
            DataType.BIGINT(),
        )
}
