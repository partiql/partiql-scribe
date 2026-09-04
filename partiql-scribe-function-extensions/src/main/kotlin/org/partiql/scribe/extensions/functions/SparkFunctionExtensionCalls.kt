package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
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

    private fun toUnixTime(args: SqlArgs): Expr =
        exprCast(
            exprCall(Identifier.regular("unix_timestamp"), args.map { it.expr }),
            DataType.BIGINT(),
        )

    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCast(
            exprCall(
                Identifier.regular("conv"),
                listOf(args[0].expr, intLiteral(16), intLiteral(10)),
            ),
            DataType.BIGINT(),
        )
}
