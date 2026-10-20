package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprAnd
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
import org.partiql.scribe.targets.spark.SparkCalls
import org.partiql.scribe.targets.spark.SparkTarget

public class SparkFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : SparkTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = SparkFunctionExtensionCalls(context, routines)
}

/**
 * Spark translations for explicitly bound PartiQL function extensions.
 *
 * The routines are exact resolved identities for functions from `partiql-function-extensions`.
 */
public open class SparkFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : SparkCalls(context) {
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

    private fun contains(args: SqlArgs): Expr = exprCall(Identifier.delimited("array_contains"), args.map { it.expr })

    /**
     * `unix_timestamp(<timestamp_ntz>)` is time-zone independent but truncates toward zero, while PartiQL truncates
     * toward negative infinity. They differ only before 1970 with a fractional second
     * (`1969-12-31 23:59:59.6`: Spark `0`, PartiQL `-1`), so subtract 1 there. `floor` on a DOUBLE epoch isn't an
     * option: `unix_micros` and `date_trunc` take a session-time-zone TIMESTAMP, not TIMESTAMP_NTZ.
     */
    private fun toUnixTime(args: SqlArgs): Expr {
        val ts = args[0].expr
        val seconds = exprCast(exprCall(Identifier.regular("unix_timestamp"), listOf(ts)), DataType.BIGINT())
        val beforeEpoch =
            exprOperator("<", ts, exprLit(Literal.typedString(DataType.TIMESTAMP(), "1970-01-01 00:00:00")))
        val hasFraction =
            exprOperator(
                "<>",
                exprCall(Identifier.regular("date_part"), listOf(exprLit(Literal.string("SECOND")), ts)),
                exprCall(Identifier.regular("second"), listOf(ts)),
            )
        val correction =
            exprCase(
                null,
                listOf(exprCaseBranch(exprAnd(beforeEpoch, hasFraction), exprLit(Literal.intNum(1L)))),
                exprLit(Literal.intNum(0L)),
            )
        return exprOperator("-", seconds, correction)
    }

    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCast(
            exprCall(
                Identifier.regular("conv"),
                listOf(args[0].expr, intLiteral(16), intLiteral(10)),
            ),
            DataType.BIGINT(),
        )
}
