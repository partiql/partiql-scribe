package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.extensions.functions.compat.datetime.DateAdd
import org.partiql.extensions.functions.compat.datetime.UtcNow
import org.partiql.extensions.functions.custom.collection.Contains
import org.partiql.extensions.functions.custom.conversion.HexToBigInt
import org.partiql.extensions.functions.custom.datetime.DateTrunc
import org.partiql.extensions.functions.custom.datetime.ToChar
import org.partiql.extensions.functions.custom.datetime.ToUnixTime
import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.plan.RoutineRef
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.spi.types.PType
import java.util.Locale

internal val DATE_ADD = DateAdd.overloads().first().signature.name
internal val UTCNOW = UtcNow.overloads().first().signature.name
internal val CONTAINS = Contains.overloads().first().signature.name
internal val HEX_TO_BIGINT = HexToBigInt.overloads().first().signature.name
internal val TO_UNIXTIME = ToUnixTime.overloads().first().signature.name
internal val POW = Pow.overloads().first().signature.name
internal val DATE_TRUNC = DateTrunc.overloads().first().signature.name
internal val TO_CHAR = ToChar.overloads().first().signature.name

internal val DATE_TRUNC_PARTS =
    setOf("millisecond", "second", "minute", "hour", "day", "week", "month", "quarter", "year")

/**
 * The rules shared by every target. [target] names the target in error messages. [zonedTimestamps] is true when the
 * target's `TIMESTAMP WITH TIME ZONE` keeps the value's own offset, as the function extensions do; see
 * [checkDatetimeArgument].
 */
internal fun commonRules(
    calls: SqlCalls,
    listener: ScribeProblemListener,
    target: String,
    zonedTimestamps: Boolean = false,
): Map<String, SqlCallFn> =
    mapOf(
        DATE_ADD to { args -> dateAdd(calls, listener, args) },
        UTCNOW to { args -> calls.retarget(UTCNOW, args) },
        POW to { args -> exprCall(Identifier.delimited(POW), args.map { it.expr }) },
        DATE_TRUNC to { args -> dateTrunc(listener, args, target, zonedTimestamps) },
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

/**
 * `date_trunc('<part>', <timestamp | timestamp with time zone | date>)` is spelled the same in Redshift, Spark, Trino,
 * and DuckDB. A date argument is cast to TIMESTAMP so every target returns a TIMESTAMP, matching the extension (Trino
 * would otherwise return DATE).
 */
private fun dateTrunc(
    listener: ScribeProblemListener,
    args: SqlArgs,
    target: String,
    zonedTimestamps: Boolean,
): Expr {
    val part = (args.firstOrNull()?.expr as? ExprLit)?.lit
    if (args.size != 2 || part?.code() != Literal.STRING) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Function extension `date_trunc` requires a string literal datetime part and a timestamp or date",
            ),
        )
    }
    val normalizedPart = part.stringValue().lowercase(Locale.ROOT)
    if (normalizedPart !in DATE_TRUNC_PARTS) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Unsupported datetime part for function extension `date_trunc`: ${part.stringValue()}",
            ),
        )
    }
    val source = args[1]
    checkDatetimeArgument(listener, DATE_TRUNC, source, target, zonedTimestamps)
    val value = if (source.type.code() == PType.DATE) exprCast(source.expr, DataType.TIMESTAMP()) else source.expr
    return exprCall(Identifier.regular("date_trunc"), listOf(exprLit(Literal.string(normalizedPart)), value))
}

/**
 * Accepts only the datetime argument types that [target] translates with the extension's meaning: TIMESTAMP and DATE,
 * plus TIMESTAMP WITH TIME ZONE when [zonedTimestamps] is set.
 *
 * - The function extensions format and truncate a `TIMESTAMP WITH TIME ZONE` in the value's own offset. Redshift,
 *   Spark, and DuckDB store an instant and use the session time zone instead, so `23:20:30-05:00` would become `04:20`
 *   (UTC); Trino keeps the offset.
 * - Any other type, including a dynamic value, is rejected: its run-time type is unknown, so a DATE would skip the cast
 *   to TIMESTAMP (Trino would return DATE), a zoned value would use the session time zone, and Redshift has no
 *   `TO_CHAR` or `date_trunc` for SUPER.
 */
internal fun checkDatetimeArgument(
    listener: ScribeProblemListener,
    function: String,
    source: SqlArg,
    target: String,
    zonedTimestamps: Boolean,
) {
    val message =
        when (source.type.code()) {
            PType.TIMESTAMP, PType.DATE -> return
            PType.TIMESTAMPZ ->
                if (zonedTimestamps) {
                    return
                } else {
                    "Function extension `$function` with a TIMESTAMP WITH TIME ZONE is unsupported for $target, " +
                        "which uses the session time zone instead of the value's own offset. " +
                        "Cast the argument to TIMESTAMP first."
                }
            else ->
                "Function extension `$function` requires a " +
                    (if (zonedTimestamps) "TIMESTAMP, DATE, or TIMESTAMP WITH TIME ZONE" else "TIMESTAMP or DATE") +
                    " argument for $target, but got ${if (source.type.code() == PType.DYNAMIC) "a dynamic value" else source.type}. " +
                    "Cast the argument to TIMESTAMP first."
        }
    listener.reportAndThrow(ScribeProblem.simpleError(ScribeProblem.UNSUPPORTED_OPERATION, message))
}

internal fun intLiteral(value: Int): Expr = exprLit(Literal.intNum(value.toLong()))

/**
 * `date_trunc('second', <timestamp>)`, used by `to_unixtime` to drop the fractional second before converting to a
 * DOUBLE epoch.
 */
internal fun truncateToSecond(ts: Expr): Expr = exprCall(Identifier.regular("date_trunc"), listOf(exprLit(Literal.string("second")), ts))
