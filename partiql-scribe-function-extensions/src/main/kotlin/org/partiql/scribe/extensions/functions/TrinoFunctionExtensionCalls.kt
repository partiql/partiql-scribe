package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.targets.trino.TrinoCalls
import org.partiql.scribe.targets.trino.TrinoTarget

public class TrinoFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : TrinoTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = TrinoFunctionExtensionCalls(context, routines)
}

/**
 * Trino translations for explicitly bound PartiQL function extensions.
 */
public open class TrinoFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : TrinoCalls(context) {
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

    private fun contains(args: SqlArgs): Expr = exprCall(Identifier.delimited("contains"), args.map { it.expr })

    /**
     * PartiQL reads the TIMESTAMP as UTC and truncates toward negative infinity. Trino's `to_unixtime(TIMESTAMP)`
     * reads it in the session time zone, and its CAST from DOUBLE to BIGINT rounds. So pin the zone with
     * `with_timezone(ts, 'UTC')` and `floor` first (`...05.6` -> `...05`, `1969-12-31 23:59:59.6` -> `-1`).
     */
    private fun toUnixTime(args: SqlArgs): Expr {
        val utc = exprCall(Identifier.regular("with_timezone"), listOf(args[0].expr, exprLit(Literal.string("UTC"))))
        return exprCast(
            exprCall(
                Identifier.regular("floor"),
                listOf(exprCall(Identifier.regular("to_unixtime"), listOf(utc))),
            ),
            DataType.BIGINT(),
        )
    }

    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCall(
            Identifier.regular("from_base"),
            listOf(args[0].expr, intLiteral(16)),
        )
}
