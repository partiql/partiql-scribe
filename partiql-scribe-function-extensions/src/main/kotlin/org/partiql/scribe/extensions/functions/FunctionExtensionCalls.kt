package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.extensions.functions.compat.datetime.DateAdd
import org.partiql.extensions.functions.compat.datetime.UtcNow
import org.partiql.extensions.functions.custom.collection.Contains
import org.partiql.extensions.functions.custom.conversion.HexToBigInt
import org.partiql.extensions.functions.custom.datetime.ToUnixTime
import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.plan.RoutineRef
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import java.util.Locale

internal val DATE_ADD = DateAdd.overloads().first().signature.name
internal val UTCNOW = UtcNow.overloads().first().signature.name
internal val CONTAINS = Contains.overloads().first().signature.name
internal val HEX_TO_BIGINT = HexToBigInt.overloads().first().signature.name
internal val TO_UNIXTIME = ToUnixTime.overloads().first().signature.name
internal val POW = Pow.overloads().first().signature.name

internal fun commonRules(
    calls: SqlCalls,
    listener: ScribeProblemListener,
): Map<String, SqlCallFn> =
    mapOf(
        DATE_ADD to { args -> dateAdd(calls, listener, args) },
        UTCNOW to { args -> calls.retarget(UTCNOW, args) },
        POW to { args -> exprCall(Identifier.delimited(POW), args.map { it.expr }) },
    )

internal fun bind(
    routines: Set<RoutineRef>,
    translations: Map<String, SqlCallFn>,
): Map<RoutineRef, SqlCallFn> =
    routines.associateWith { routineRef ->
        val name = routineRef.name.getName()
        requireNotNull(translations[name]) { "Unsupported PartiQL function extension `$name`" }
    }

private fun dateAdd(
    calls: SqlCalls,
    listener: ScribeProblemListener,
    args: SqlArgs,
): Expr {
    val part = (args.firstOrNull()?.expr as? ExprLit)?.lit
    if (args.size != 3 || part?.code() != Literal.STRING) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Function extension `date_add` requires a string datetime part, quantity, and timestamp",
            ),
        )
    }
    val normalizedPart = part.stringValue().lowercase(Locale.ROOT)
    if (normalizedPart !in setOf("year", "month", "day", "hour", "minute", "second")) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Unsupported datetime part for function extension `date_add`: ${part.stringValue()}",
            ),
        )
    }
    return calls.retarget("date_add_$normalizedPart", args.drop(1))
}

internal fun intLiteral(value: Int): Expr = exprLit(Literal.intNum(value.toLong()))
