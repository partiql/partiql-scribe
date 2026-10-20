package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprExtract
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.math.BigInteger
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates the Redshift date and time constructs that have their own syntax: datetime and interval literals,
 * `DATEADD`, `DATEDIFF`, `EXTRACT`, `AT TIME ZONE`, the current date and time, and the `+` and `-` operators on
 * datetime and interval values. The datetime functions with the function call syntax, and the rules that both share,
 * are in [RedshiftDatetimeFunctions].
 *
 * Several translations count whole units from a reference value: PartiQL `DATE_DIFF(unit, R, x)` counts the whole
 * units from `R` to `x`, and `R` is at the start of every year, month, day, hour, minute, and second (and is a
 * Monday), so the count is also the number of unit boundaries between `R` and `x`. See [REFERENCE].
 *
 * The Redshift session time zone is assumed to be UTC, the Redshift default, and TIMESTAMPTZ values in the source data
 * are assumed to be UTC instants (see the README).
 */
internal class RedshiftDatetime(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val types: OperandTypes,
) {
    private val functions = builder.functions.datetime

    /** The `+` and `-` operators that have an interval literal without a qualifier as an operand. */
    private val unqualifiedIntervalOperators = HashSet<Token>()

    /** The `+` and `-` operators that have an interval literal operand whose fields Redshift does not normalize. */
    private val unnormalizedIntervalOperators = HashSet<Token>()

    /**
     * Translates `DATEADD(part, n, x)` and `DATEDIFF(part, a, b)`. The date part is a name or a character string.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    fun dateFunction(ctx: P.AtomDateFunctionContext): Expr {
        val partName = datePartName(ctx.part)
        val args = ctx.expr().map { Operand(it, exprs.expr(it)) }
        val name = ctx.name.text.uppercase()
        return builder.functions.checked(
            ctx.name,
            name,
            if (ctx.name.type == P.DATEADD) {
                dateAdd(ctx.name, ctx.part.start, partName, args[0], args[1])
            } else {
                dateDiff(ctx.name, ctx.part.start, partName, args[0], args[1])
            },
        )
    }

    private fun datePartName(ctx: P.DatePartContext): String =
        if (ctx.STRING_LITERAL() != null) exprs.stringValue(ctx.STRING_LITERAL().symbol) else ctx.text

    /**
     * `DATEDIFF(part, a, b)` counts the `part` boundaries between `a` and `b`; PartiQL `DATE_DIFF` counts whole units
     * of elapsed time. The translation counts the whole units from the reference [REFERENCE] (`R`) to each value:
     * `DATE_DIFF(DAY, R, b) - DATE_DIFF(DAY, R, a)`. TIME values count from midnight, and TIMETZ values are converted to
     * UTC first. Milliseconds and microseconds are computed from whole seconds and the fraction of `EXTRACT(SECOND)`.
     * The difference of two DATE values in days is the PartiQL `DATE_DIFF(DAY, a, b)`.
     *
     * Quarter and week boundaries are not documented for DATEDIFF, and Redshift has no DATEDIFF for TIMESTAMPTZ.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    private fun dateDiff(
        name: Token,
        partToken: Token,
        partName: String,
        a: Operand,
        b: Operand,
    ): Expr {
        val part = DatePart.of(partName)
        if (part == null || part !in DIFF_PARTS) throw UnsupportedConstructException.at(partToken, "datepart `$partName` in DATEDIFF")
        return types.resolve(listOf(a.ctx to a.expr, b.ctx to b.expr)) { e, t ->
            if (functions.waiting(t)) return@resolve types.pending(e)
            val kinds = t.map(Kind::of)
            val isTime = kinds.any { it == Kind.TIME || it == Kind.TIMETZ }
            val target = if (isTime) PType.TIME else PType.TIMESTAMP
            val values =
                listOf(0, 1).map { i ->
                    val operand = if (i == 0) a else b
                    val other = kinds[1 - i]
                    functions.datetimeArgument("DATEDIFF", operand, e[i], kinds[i], target, dateOperand = other == Kind.DATE)
                }
            val (x, y) = values
            val kx = x.second
            val ky = y.second
            when {
                kx == Kind.TIMESTAMPTZ || ky == Kind.TIMESTAMPTZ ->
                    throw UnsupportedConstructException.at(name, "DATEDIFF with a TIMESTAMPTZ argument")
                kx.isDateOrTimestamp && ky.isDateOrTimestamp -> {
                    if (part == DatePart.DAY && kx == Kind.DATE && ky == Kind.DATE) {
                        builder.note(name, "Redshift `DATEDIFF` was replaced by PartiQL `DATE_DIFF`")
                        return@resolve dateDiffCall(DatePart.DAY, x.first, y.first)
                    }
                    builder.note(name, boundaryNote(partName, part, "a fixed reference timestamp"))
                    boundaries(part, REFERENCE, x.first, y.first)
                }
                kx.isTimeOfDay && ky.isTimeOfDay -> {
                    if (part !in TIME_PARTS) {
                        throw UnsupportedConstructException.at(partToken, "DATEDIFF datepart `$partName` of a TIME value")
                    }
                    val utc = kx == Kind.TIMETZ || ky == Kind.TIMETZ
                    builder.note(name, boundaryNote(partName, part, if (utc) "TIME '00:00:00' of the UTC times" else "TIME '00:00:00'"))
                    boundaries(part, MIDNIGHT, utcTime(x.first, kx), utcTime(y.first, ky))
                }
                else -> throw UnsupportedConstructException.at(name, "DATEDIFF ${argumentsText(kx, ky)}")
            }
        }
    }

    private fun boundaryNote(
        partName: String,
        part: DatePart,
        reference: String,
    ): String {
        val unit = if (part == DatePart.MILLISECOND || part == DatePart.MICROSECOND) "SECOND" else part.name
        return "Redshift `DATEDIFF($partName, ...)` counts ${part.name.lowercase()} boundaries; it was replaced by a difference of " +
            "PartiQL `DATE_DIFF($unit, ...)` values from $reference"
    }

    /** The arguments of an unsupported call, by kind: `with a TIMESTAMPTZ argument`, `of TIME and TIMESTAMP values`. */
    private fun argumentsText(vararg kinds: Kind): String {
        val unusual = kinds.firstOrNull { !it.isTemporal }
        return if (unusual != null) "with ${unusual.argument}" else "of ${kinds.distinct().joinToString(" and ") { it.display }} values"
    }

    /**
     * The number of [part] boundaries from [a] to [b], counted from [reference]: [REFERENCE] for DATE and TIMESTAMP
     * values, [MIDNIGHT] for TIME values.
     */
    private fun boundaries(
        part: DatePart,
        reference: Expr,
        a: Expr,
        b: Expr,
    ): Expr =
        when (part) {
            DatePart.MILLISECOND, DatePart.MICROSECOND -> {
                val perSecond = if (part == DatePart.MILLISECOND) 1_000 else 1_000_000
                val nanosPerUnit = 1_000_000_000 / perSecond

                fun units(x: Expr): Expr =
                    exprOperator(
                        "+",
                        exprOperator("*", dateDiffCall(DatePart.SECOND, reference, x), int(perSecond)),
                        exprOperator("/", nanosOfSecond(x), int(nanosPerUnit)),
                    )
                exprOperator("-", units(b), units(a))
            }
            else -> exprOperator("-", dateDiffCall(part, reference, b), dateDiffCall(part, reference, a))
        }

    /**
     * `DATEADD(part, n, x)` adds `n` units to `x` and returns TIMESTAMP for DATE and TIMESTAMP values; PartiQL
     * `DATE_ADD` returns DATE for a DATE value, so the value is cast to TIMESTAMP first. Adding years moves February 29
     * to March 1 in Redshift, and to February 28 in PartiQL, so the year rule adds the day back. A week is 7 days.
     * Milliseconds and microseconds use interval arithmetic. TIMETZ values are converted to UTC, and the result is a
     * TIMETZ value in UTC, as in Redshift. The number `n` must be an integer.
     *
     * The end of month behavior of quarters, and the century numbering of decades, centuries, and millennia, are not
     * documented for DATEADD. The result type for a TIMESTAMPTZ value is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     */
    private fun dateAdd(
        name: Token,
        partToken: Token,
        partName: String,
        n: Operand,
        x: Operand,
    ): Expr {
        val part = DatePart.of(partName)
        if (part == null || part !in ADD_PARTS) throw UnsupportedConstructException.at(partToken, "datepart `$partName` in DATEADD")
        return types.resolve(listOf(n.ctx to n.expr, x.ctx to x.expr)) { e, t ->
            if (functions.waiting(t)) return@resolve types.pending(e)
            if (Kind.of(t[0]) != Kind.INTEGER) throw UnsupportedConstructException.at(n.ctx, "DATEADD with a non-integer interval")
            val count = e[0]
            val (value, kind) = functions.datetimeArgument("DATEADD", x, e[1], Kind.of(t[1]), PType.TIMESTAMP)
            when (kind) {
                Kind.DATE, Kind.TIMESTAMP -> {
                    val timestamp =
                        if (kind == Kind.DATE) {
                            builder.note(
                                x.ctx.start,
                                "Redshift `DATEADD` returns TIMESTAMP for a DATE argument; the argument was cast to TIMESTAMP",
                            )
                            exprCast(value, DataType.TIMESTAMP())
                        } else {
                            value
                        }
                    addUnits(name, partName, part, count, timestamp)
                }
                Kind.TIME -> {
                    if (part !in TIME_PARTS) {
                        throw UnsupportedConstructException.at(
                            partToken,
                            "DATEADD datepart `$partName` of a TIME value",
                        )
                    }
                    addUnits(name, partName, part, count, value)
                }
                Kind.TIMETZ -> {
                    if (part !in TIME_PARTS) {
                        throw UnsupportedConstructException.at(
                            partToken,
                            "DATEADD datepart `$partName` of a TIMETZ value",
                        )
                    }
                    builder.note(name, "Redshift `DATEADD` of a TIMETZ value returns the time in UTC; the value was converted to UTC")
                    exprCast(addUnits(name, partName, part, count, utcTime(value, kind)), DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION))
                }
                Kind.TIMESTAMPTZ -> throw UnsupportedConstructException.at(name, "DATEADD with a TIMESTAMPTZ argument")
                else -> throw UnsupportedConstructException.at(x.ctx, "DATEADD with ${kind.argument}")
            }
        }
    }

    /**
     * Adds [count] units of [part] to the TIMESTAMP or TIME value [x].
     */
    private fun addUnits(
        name: Token,
        partName: String,
        part: DatePart,
        count: Expr,
        x: Expr,
    ): Expr =
        when (part) {
            DatePart.YEAR -> {
                builder.note(
                    name,
                    "Redshift `DATEADD($partName, ...)` moves February 29 to March 1 in a non-leap year; it was replaced by " +
                        "PartiQL `DATE_ADD(YEAR, ...)` with a day correction",
                )
                val added = dateAddCall(DatePart.YEAR, count, x)
                val correction = exprOperator("-", exprExtract(DatetimeField.DAY(), x), exprExtract(DatetimeField.DAY(), added))
                dateAddCall(DatePart.DAY, correction, added)
            }
            DatePart.WEEK -> {
                builder.note(name, "Redshift `DATEADD($partName, n, ...)` was replaced by PartiQL `DATE_ADD(DAY, 7 * n, ...)`")
                dateAddCall(DatePart.DAY, exprOperator("*", int(7), count), x)
            }
            DatePart.MILLISECOND, DatePart.MICROSECOND -> {
                builder.note(name, "Redshift `DATEADD($partName, n, ...)` was replaced by PartiQL interval arithmetic")
                val unit = if (part == DatePart.MILLISECOND) secondsInterval("0.001", 3) else secondsInterval("0.000001", 6)
                exprOperator("+", x, exprOperator("*", unit, count))
            }
            else -> {
                builder.note(name, "Redshift `DATEADD` was replaced by PartiQL `DATE_ADD`")
                dateAddCall(part, count, x)
            }
        }

    /**
     * Translates `EXTRACT(part FROM x)`; see [RedshiftDatetimeFunctions.datePart].
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_EXTRACT_function.html
     */
    fun extract(ctx: P.AtomExtractContext): Expr {
        val partName = datePartName(ctx.part)
        val expr =
            functions.datePart(
                ctx.EXTRACT().symbol,
                "EXTRACT",
                ctx.part.start,
                partName,
                Operand(ctx.expr(), exprs.expr(ctx.expr())),
            )
        return builder.functions.checked(ctx.EXTRACT().symbol, "EXTRACT", expr)
    }

    /**
     * Translates a `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, or `TIMESTAMPTZ` literal. The value is written in the ISO
     * format that PartiQL reads; see [DatetimeText] for the Redshift formats that the translator reads.
     *
     *  - A TIMESTAMP literal ignores a time zone, as in Redshift.
     *  - A TIMESTAMPTZ literal is converted to UTC, as Redshift stores it. Without a time zone, the value is in the
     *    session time zone, which is assumed to be UTC.
     *  - A TIMETZ literal keeps its offset, as in Redshift. Without a time zone, the offset is the session time zone.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Date_and_time_literals.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Datetime_types.html
     */
    fun literal(ctx: P.LiteralContext): Expr {
        val (name, token) =
            when (ctx) {
                is P.LiteralDateContext -> "DATE" to ctx.STRING_LITERAL().symbol
                is P.LiteralTimeContext -> "TIME" to ctx.STRING_LITERAL().symbol
                is P.LiteralTimeTzContext -> "TIMETZ" to ctx.STRING_LITERAL().symbol
                is P.LiteralTimestampContext -> "TIMESTAMP" to ctx.STRING_LITERAL().symbol
                is P.LiteralTimestampTzContext -> "TIMESTAMPTZ" to ctx.STRING_LITERAL().symbol
                else -> builder.unexpected(ctx)
            }
        val value = exprs.stringValue(token)
        val unsupported = { UnsupportedConstructException.at(ctx, "$name literal format '$value'") }
        return when (ctx) {
            is P.LiteralDateContext -> {
                val parts = DatetimeText.readTimestamp(value, epoch = false) ?: throw unsupported()
                exprLit(Literal.typedString(DataType.DATE(), parts.date.toString()))
            }
            is P.LiteralTimeContext -> {
                val parts = DatetimeText.readTime(value)
                if (parts == null || parts.zone != null) throw unsupported()
                exprLit(Literal.typedString(DataType.TIME(), parts.time.toString()))
            }
            is P.LiteralTimeTzContext -> {
                val parts = DatetimeText.readTime(value) ?: throw unsupported()
                val zone = parts.zone ?: DatetimeText.Zone.UTC.also { noteSessionTimeZone(ctx.start, "TIMETZ") }
                val offset = zone.offset() ?: throw unsupported()
                // PartiQL compares the UTC times of TIMETZ values; how Redshift compares a value with another offset than
                // UTC is not documented (PostgreSQL also compares the offsets).
                if (zone.minutes != 0 && isComparisonOperand(ctx)) {
                    throw UnsupportedConstructException.at(ctx, "comparison with a TIMETZ literal whose offset is not UTC")
                }
                exprLit(Literal.typedString(DataType.TIME_WITH_TIME_ZONE(), "${parts.time}$offset"))
            }
            is P.LiteralTimestampContext -> {
                val parts = DatetimeText.readTimestamp(value) ?: throw unsupported()
                if (parts.zone != null) builder.note(ctx.start, "Redshift ignores the time zone of a TIMESTAMP literal; it was removed")
                exprLit(Literal.typedString(DataType.TIMESTAMP(), parts.timestamp()))
            }
            is P.LiteralTimestampTzContext -> {
                val parts = DatetimeText.readTimestamp(value) ?: throw unsupported()
                // `epoch` is 1970-01-01 00:00:00 in UTC.
                if (parts.zone == null && !value.trim().equals("epoch", ignoreCase = true)) noteSessionTimeZone(ctx.start, "TIMESTAMPTZ")
                val utc = parts.utcTimestamp() ?: throw unsupported()
                exprLit(Literal.typedString(DataType.TIMESTAMP_WITH_TIME_ZONE(), utc))
            }
            else -> builder.unexpected(ctx)
        }
    }

    private fun noteSessionTimeZone(
        token: Token,
        type: String,
    ) = builder.note(
        token,
        "Redshift reads a $type literal without a time zone in the session time zone; the translation assumes UTC, the Redshift default",
    )

    /**
     * `CURRENT_DATE` is the current date in the session time zone, which is assumed to be UTC. PartiQL `CURRENT_DATE`
     * has the time zone of the PartiQL engine, and PLK 1.6.1 cannot evaluate it.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CURRENT_DATE_function.html
     */
    fun currentDate(name: Token): Expr = functions.now(name, "CURRENT_DATE", "`CAST(UTCNOW() AS DATE)`", DataType.DATE())

    /**
     * `SYSDATE` is the start time of the current transaction as a TIMESTAMP in the session time zone, which is assumed
     * to be UTC.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    fun sysdate(name: Token): Expr = functions.now(name, "SYSDATE", "`CAST(UTCNOW() AS TIMESTAMP)`", DataType.TIMESTAMP())

    /**
     * Returns the PartiQL literal of [type] for the Redshift character string [value], or null if [type] is not a
     * datetime type or the translator does not read the format of [value]. Redshift converts a character string to the
     * datetime type of the value that it is compared or combined with, with the rules of the typed literals (see
     * [literal]). A DATE value converts only a string without a time, or with the time 00:00:00, unless
     * [dropTime] is true (`CAST('2008-02-18 02:36:48' AS DATE)` is the date 2008-02-18).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */
    fun stringToDatetime(
        value: String,
        type: PType,
        dropTime: Boolean = false,
    ): Expr? = DatetimeText.literal(value, type.code(), dropTime)

    /**
     * Translates `lhs op rhs` for `+` and `-` when an operand is a datetime or interval value, by the operand types.
     * Returns null when no operand is a datetime or interval value. Redshift semantics:
     *
     *  - `date + integer` and `date - integer` add days, and `date - date` is the number of days (INTEGER).
     *  - `date + interval` returns DATE for a YEAR TO MONTH interval and TIMESTAMP otherwise; PartiQL returns DATE and
     *    adds whole days only, so the DATE operand is cast to TIMESTAMP.
     *  - `date + time` is the TIMESTAMP of the date at that time, and `date + timetz` the TIMESTAMPTZ.
     *  - `timetz + interval` returns the time in UTC; PartiQL keeps the offset.
     *  - `timestamp - timestamp` is an interval in both languages.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_data_types.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE-CONCATENATE_function.html
     */
    fun additive(
        op: Token,
        lhs: Expr,
        lhsType: PType,
        rhs: Expr,
        rhsType: PType,
    ): Expr? {
        val l = Kind.of(lhsType)
        val r = Kind.of(rhsType)
        if (!l.isTemporal && !r.isTemporal) return null
        if (types.probing && (l == Kind.UNKNOWN || r == Kind.UNKNOWN)) return types.pending(listOf(lhs, rhs))
        val plus = op.type == P.PLUS
        val symbol = op.text
        if (op in unnormalizedIntervalOperators && !(l.isDatetime || r.isDatetime)) unsupportedArithmetic(op, l, r)
        return when {
            // date +- integer
            l == Kind.DATE && r == Kind.INTEGER -> {
                builder.note(op, "Redshift `date $symbol integer` was replaced by PartiQL `DATE_ADD(DAY, ...)`")
                dateAddCall(DatePart.DAY, if (plus) rhs else negate(rhs), lhs)
            }
            l == Kind.INTEGER && r == Kind.DATE && plus -> {
                builder.note(op, "Redshift `integer + date` was replaced by PartiQL `DATE_ADD(DAY, ...)`")
                dateAddCall(DatePart.DAY, lhs, rhs)
            }
            // date - date
            l == Kind.DATE && r == Kind.DATE && !plus -> {
                builder.note(op, "Redshift `date - date` (a number of days) was replaced by PartiQL `DATE_DIFF(DAY, ...)`")
                exprCast(dateDiffCall(DatePart.DAY, rhs, lhs), DataType.INT())
            }
            // datetime - datetime
            !plus && (l.isDateOrTimestamp && r.isDateOrTimestamp || l == Kind.TIME && r == Kind.TIME) -> {
                builder.note(
                    op,
                    "Redshift `${l.display} - ${r.display}` returns an INTERVAL; PartiQL returns an INTERVAL DAY TO SECOND with the " +
                        "same duration",
                )
                exprOperator(symbol, lhs, rhs)
            }
            !plus && (l == Kind.TIMESTAMPTZ && r.isTimestampLike || l.isTimestampLike && r == Kind.TIMESTAMPTZ) -> {
                builder.note(
                    op,
                    "Redshift `${l.display} - ${r.display}` returns an INTERVAL; PartiQL returns an INTERVAL DAY TO SECOND with the " +
                        "same duration",
                )
                exprOperator(symbol, lhs, rhs)
            }
            // date + time
            plus && (l == Kind.DATE && r.isTimeOfDay || l.isTimeOfDay && r == Kind.DATE) -> {
                val date = if (l == Kind.DATE) lhs else rhs
                val time = if (l == Kind.DATE) rhs else lhs
                val timeKind = if (l == Kind.DATE) r else l
                dateAndTime(op, date, time, timeKind)
            }
            // datetime +- interval, interval + datetime
            l.isDatetime && r.isInterval -> datetimeAndInterval(op, lhs, l, rhs, r, intervalFirst = false)
            plus && l.isInterval && r.isDatetime -> datetimeAndInterval(op, rhs, r, lhs, l, intervalFirst = true)
            // interval +- interval
            l.isInterval && r.isInterval -> {
                if (l != r) throw UnsupportedConstructException.at(op, "`$symbol` of a YEAR TO MONTH interval and a DAY TO SECOND interval")
                null
            }
            else -> unsupportedArithmetic(op, l, r)
        }
    }

    private fun unsupportedArithmetic(
        op: Token,
        l: Kind,
        r: Kind,
    ): Nothing {
        if (op in unnormalizedIntervalOperators && !(l.isDatetime || r.isDatetime)) {
            throw UnsupportedConstructException.at(
                op,
                "INTERVAL literal whose fields Redshift does not normalize, outside of datetime arithmetic",
            )
        }
        throw UnsupportedConstructException.at(op, "${l.display} ${op.text} ${r.display}")
    }

    /**
     * `date + time` and `date + timetz`. The time of day is the interval since midnight. For TIMETZ, the result is the
     * instant of the date and the local time at the offset of the value.
     */
    private fun dateAndTime(
        op: Token,
        date: Expr,
        time: Expr,
        timeKind: Kind,
    ): Expr {
        builder.note(op, "Redshift `date + ${timeKind.display.lowercase()}` was replaced by PartiQL timestamp and interval arithmetic")
        val timestamp = exprCast(date, DataType.TIMESTAMP())
        if (timeKind == Kind.TIME) return exprOperator("+", timestamp, exprOperator("-", time, MIDNIGHT))
        // The local time of the TIMETZ value, and its offset as an interval.
        val local = exprCast(time, DataType.TIME(DATETIME_PRECISION))
        val offset = exprOperator("-", exprCast(local, DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION)), time)
        val localTimestamp = exprOperator("+", timestamp, exprOperator("-", local, MIDNIGHT))
        return exprOperator("-", exprCast(localTimestamp, DataType.TIMESTAMP_WITH_TIME_ZONE(DATETIME_PRECISION)), offset)
    }

    /**
     * `datetime +- interval` and `interval + datetime`.
     */
    private fun datetimeAndInterval(
        op: Token,
        datetime: Expr,
        datetimeKind: Kind,
        interval: Expr,
        intervalKind: Kind,
        intervalFirst: Boolean,
    ): Expr? {
        val symbol = op.text

        fun build(value: Expr): Expr = if (intervalFirst) exprOperator(symbol, interval, value) else exprOperator(symbol, value, interval)
        val dayTime = intervalKind == Kind.INTERVAL_DT
        return when (datetimeKind) {
            Kind.TIMESTAMP, Kind.TIMESTAMPTZ -> null
            Kind.DATE ->
                if (dayTime || op in unqualifiedIntervalOperators) {
                    builder.note(op, "Redshift returns TIMESTAMP for `date $symbol interval`; the DATE operand was cast to TIMESTAMP")
                    build(exprCast(datetime, DataType.TIMESTAMP()))
                } else {
                    null
                }
            Kind.TIME -> if (dayTime) null else unsupportedArithmetic(op, datetimeKind, intervalKind)
            Kind.TIMETZ -> {
                if (!dayTime) unsupportedArithmetic(op, datetimeKind, intervalKind)
                builder.note(op, "Redshift returns the time in UTC for `timetz $symbol interval`; the TIMETZ value was converted to UTC")
                exprCast(build(utcTime(datetime, Kind.TIMETZ)), DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION))
            }
            else -> unsupportedArithmetic(op, datetimeKind, intervalKind)
        }
    }

    /** `-n` for a number; a literal gets the sign. */
    private fun negate(n: Expr): Expr {
        if (n is ExprLit && n.lit.code() == Literal.INT_NUM) {
            val text = n.lit.numberValue()
            return exprLit(Literal.intNum(if (text.startsWith("-")) text.removePrefix("-") else "-$text"))
        }
        return exprOperator("-", null, n)
    }

    /**
     * Translates `x AT TIME ZONE 'zone'` for the UTC time zone: a TIMESTAMP value becomes the TIMESTAMPTZ value at
     * that time in UTC, and a TIMESTAMPTZ value becomes its TIMESTAMP in UTC. PartiQL has no time zone database, so
     * other time zones are unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_AT_TIME_ZONE.html
     */
    fun atTimeZone(ctx: P.ValueAtTimeZoneContext): Expr {
        val zone =
            stringLiteral(ctx.zone)
                ?: throw UnsupportedConstructException.at(ctx.zone, "AT TIME ZONE with a time zone that is not a literal")
        val operand = Operand(ctx.valueExpr(), exprs.valueExpr(ctx.valueExpr()))
        return functions.atTimeZone(ctx.AT().symbol, "AT TIME ZONE", zone, operand, stringValues = false)
    }

    /**
     * `CURRENT_TIMESTAMP`, `CURRENT_TIME`, `LOCALTIMESTAMP`, and `LOCALTIME` are deprecated leader node-only functions:
     * Redshift raises an error when a query that uses them reads a table.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/Date_functions_header.html
     */
    fun currentDatetime(name: Token): Expr =
        throw UnsupportedConstructException.at(name, "${name.text.uppercase()} (Redshift leader node-only function)")

    /**
     * Translates an interval literal. A literal with a qualifier (`INTERVAL '1 2:03:04' DAY TO SECOND`) has the PartiQL
     * format when the string has the numeric format of the qualifier. A literal without a qualifier
     * (`INTERVAL '3 hours, 35 minutes'`) is rewritten with a qualifier. See [IntervalText].
     *
     * PartiQL intervals have a precision for the leading field, which is 2 by default; the translation writes 9, so that
     * any value that fits in the field is valid. Redshift keeps 6 fractional digits of seconds by default.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_data_types.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_literals.html
     */
    fun intervalLiteral(ctx: P.LiteralIntervalContext): Expr {
        val text = exprs.stringValue(ctx.STRING_LITERAL().symbol)
        val qualifier = ctx.intervalQualifier()
        val value =
            if (qualifier != null) {
                IntervalText.qualified(text, intervalQualifier(qualifier))
                    ?: throw UnsupportedConstructException.at(ctx, "INTERVAL literal '$text' ${textOf(qualifier).uppercase()}")
            } else {
                val unqualified =
                    IntervalText.unqualified(text) ?: throw UnsupportedConstructException.at(ctx, "INTERVAL literal '$text'")
                // DATE +- an unqualified interval returns TIMESTAMP; the rule sees the interval only as a direct operand.
                if (inIntervalArithmetic(ctx)) {
                    throw UnsupportedConstructException.at(ctx, "INTERVAL literal '$text' without a qualifier in interval arithmetic")
                }
                builder.note(ctx.start, "The Redshift interval literal was rewritten with an SQL interval qualifier")
                val operator = additiveOperator(ctx)
                if (operator != null) unqualifiedIntervalOperators.add(operator)
                if (!unqualified.normalized) {
                    // Redshift keeps fields such as 36 hours that PartiQL normalizes to 1 day 12 hours. The values are
                    // equal as durations, so the literal is valid only as an operand of datetime arithmetic.
                    if (operator == null) {
                        throw UnsupportedConstructException.at(
                            ctx,
                            "INTERVAL literal '$text' outside of datetime arithmetic (Redshift does not normalize its fields)",
                        )
                    }
                    unnormalizedIntervalOperators.add(operator)
                }
                unqualified
            }
        if (value.negative && value.lessThanOneSecond) {
            // The PartiQL printer drops the sign of such a value.
            throw UnsupportedConstructException.at(ctx, "negative INTERVAL literal '$text' shorter than one second")
        }
        return exprLit(Literal.typedString(DataType.INTERVAL(value.qualifier()), value.text()))
    }

    /**
     * Returns the `+` or `-` operator token of which the interval literal [ctx] is an operand, possibly in parentheses.
     */
    private fun additiveOperator(ctx: P.LiteralIntervalContext): Token? {
        val (child, parent) = operandOf(ctx)
        return if (parent is P.ValueAdditiveContext && (parent.lhs === child || parent.rhs === child)) parent.op else null
    }

    /**
     * True if the interval literal [ctx] is an operand of `*` or `/`, or of `+` or `-` with another interval literal.
     */
    private fun inIntervalArithmetic(ctx: P.LiteralIntervalContext): Boolean {
        val (child, parent) = operandOf(ctx)
        return when (parent) {
            is P.ValueMultiplicativeContext -> true
            is P.ValueAdditiveContext -> {
                val other = if (parent.lhs === child) parent.rhs else parent.lhs
                (atomOf(other) as? P.AtomLiteralContext)?.literal() is P.LiteralIntervalContext
            }
            else -> false
        }
    }

    /** True if the literal [ctx] is an operand of a comparison, `BETWEEN`, or `IN` predicate, possibly in parentheses. */
    private fun isComparisonOperand(ctx: P.LiteralContext): Boolean =
        when (val parent = operandOf(ctx).second) {
            is P.PredicateComparisonContext, is P.PredicateBetweenContext, is P.PredicateInListContext -> true
            // The operand of a simple CASE and its WHEN values are compared, as are the arguments of NULLIF.
            is P.AtomCaseContext -> parent.operand != null
            is P.CaseWhenContext -> (parent.parent as? P.AtomCaseContext)?.operand != null
            is P.AtomFunctionCallContext -> parent.name.text.lowercase() == "nullif"
            else -> false
        }

    /**
     * Returns the expression of which [ctx] is an operand, and the child of that expression that contains [ctx]: the
     * first ancestor that is not a parenthesis or the wrapper of a single expression.
     */
    private fun operandOf(ctx: ParserRuleContext): Pair<ParserRuleContext, ParserRuleContext?> {
        var child: ParserRuleContext = ctx
        var parent: ParserRuleContext? = ctx.getParent()
        while (
            parent is P.AtomLiteralContext || parent is P.PrimaryAtomContext || parent is P.ValuePrimaryContext ||
            parent is P.PredicateBaseContext || parent is P.NotBaseContext || parent is P.AndBaseContext ||
            parent is P.OrBaseContext || parent is P.ExprContext || parent is P.AtomParenthesizedContext
        ) {
            child = parent
            parent = parent.getParent()
        }
        return child to parent
    }

    /**
     * The fields of an interval qualifier. Redshift writes the fractional precision of seconds after `SECOND`
     * (`SECOND(3)`, `DAY TO SECOND(3)`); a precision for another field is not documented.
     */
    private fun intervalQualifier(ctx: P.IntervalQualifierContext): IntervalText.Qualifier {
        val start = IntervalText.Field.valueOf(ctx.start.text.uppercase())
        val end = ctx.end?.let { IntervalText.Field.valueOf(it.text.uppercase()) }
        val unsupported = { UnsupportedConstructException.at(ctx, "INTERVAL qualifier `${textOf(ctx).uppercase()}`") }
        val fractionToken =
            when {
                end == null && start == IntervalText.Field.SECOND -> ctx.precision
                ctx.precision != null -> throw unsupported()
                end == IntervalText.Field.SECOND -> ctx.fraction
                ctx.fraction != null -> throw unsupported()
                else -> null
            }
        val fraction = fractionToken?.let { it.text.toIntOrNull() ?: throw unsupported() }
        if (fraction != null && fraction > 6) throw unsupported()
        val qualifier = IntervalText.Qualifier(start, end ?: start, fraction)
        if (!qualifier.isValid) throw unsupported()
        return qualifier
    }

    /**
     * Redshift INTERVAL types are not translated: PartiQL casts only between interval types, and the Redshift rules for
     * casts that drop fields are not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_data_types.html
     */
    fun intervalType(ctx: P.TypeIntervalContext): DataType = throw UnsupportedConstructException.at(ctx, "data type `${textOf(ctx)}`")

    /** The source text of [ctx], with single spaces between tokens. */
    private fun textOf(ctx: ParserRuleContext): String {
        val tokens = ArrayList<String>()

        fun collect(tree: ParseTree) {
            if (tree is TerminalNode) tokens.add(tree.text) else (0 until tree.childCount).forEach { collect(tree.getChild(it)) }
        }
        collect(ctx)
        return tokens.joinToString(" ").replace("( ", "(").replace(" )", ")").replace(" (", "(")
    }

    /** The value of [ctx] if it is a character string literal. */
    private fun stringLiteral(ctx: P.PrimaryExprContext): String? {
        val literal = ((ctx as? P.PrimaryAtomContext)?.atom() as? P.AtomLiteralContext)?.literal() as? P.LiteralStringContext
        return literal?.let { exprs.stringValue(it.STRING_LITERAL().symbol) }
    }

    /**
     * Datetime and interval operand kinds.
     */
    internal enum class Kind(val display: String) {
        DATE("DATE"),
        TIME("TIME"),
        TIMETZ("TIMETZ"),
        TIMESTAMP("TIMESTAMP"),
        TIMESTAMPTZ("TIMESTAMPTZ"),
        INTERVAL_YM("INTERVAL YEAR TO MONTH"),
        INTERVAL_DT("INTERVAL DAY TO SECOND"),
        INTEGER("number"),
        NUMBER("number"),
        STRING("character string"),
        NULL("NULL"),
        UNKNOWN("value of unknown type"),
        OTHER("value"),
        ;

        /** The kind as an argument in a message: `a DATE argument`. */
        val argument: String
            get() =
                when (this) {
                    INTEGER, NUMBER -> "a numeric argument"
                    STRING -> "a character string argument"
                    NULL -> "a NULL argument"
                    UNKNOWN -> "an argument of unknown type"
                    OTHER -> "an argument of an unsupported type"
                    INTERVAL_YM, INTERVAL_DT -> "an $display argument"
                    else -> "a $display argument"
                }

        val isDateOrTimestamp: Boolean get() = this == DATE || this == TIMESTAMP
        val isTimestampLike: Boolean get() = this == DATE || this == TIMESTAMP || this == TIMESTAMPTZ
        val isTimeOfDay: Boolean get() = this == TIME || this == TIMETZ
        val isDatetime: Boolean get() = this == DATE || this == TIME || this == TIMETZ || this == TIMESTAMP || this == TIMESTAMPTZ
        val isInterval: Boolean get() = this == INTERVAL_YM || this == INTERVAL_DT
        val isTemporal: Boolean get() = isDatetime || isInterval

        companion object {
            fun of(type: PType): Kind =
                when (type.code()) {
                    PType.DATE -> DATE
                    PType.TIME -> TIME
                    PType.TIMEZ -> TIMETZ
                    PType.TIMESTAMP -> TIMESTAMP
                    PType.TIMESTAMPZ -> TIMESTAMPTZ
                    PType.INTERVAL_YM -> INTERVAL_YM
                    PType.INTERVAL_DT -> INTERVAL_DT
                    PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT -> INTEGER
                    PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> NUMBER
                    PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB -> STRING
                    PType.UNKNOWN -> NULL
                    PType.DYNAMIC -> UNKNOWN
                    else -> OTHER
                }
        }
    }

    /**
     * Redshift date part names and abbreviations.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Dateparts_for_datetime_functions.html
     */
    internal enum class DatePart {
        MILLENNIUM,
        CENTURY,
        DECADE,
        EPOCH,
        YEAR,
        QUARTER,
        MONTH,
        WEEK,
        DOW,
        DOY,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        MILLISECOND,
        MICROSECOND,
        TIMEZONE,
        TIMEZONE_HOUR,
        TIMEZONE_MINUTE,
        ;

        companion object {
            private val NAMES: Map<String, DatePart> =
                buildMap {
                    listOf("millennium", "millennia", "mil", "mils").forEach { put(it, MILLENNIUM) }
                    listOf("century", "centuries", "c", "cent", "cents").forEach { put(it, CENTURY) }
                    listOf("decade", "decades", "dec", "decs").forEach { put(it, DECADE) }
                    put("epoch", EPOCH)
                    listOf("year", "years", "y", "yr", "yrs").forEach { put(it, YEAR) }
                    listOf("quarter", "quarters", "qtr", "qtrs").forEach { put(it, QUARTER) }
                    listOf("month", "months", "mon", "mons").forEach { put(it, MONTH) }
                    listOf("week", "weeks", "w").forEach { put(it, WEEK) }
                    listOf("dayofweek", "dow", "dw", "weekday").forEach { put(it, DOW) }
                    listOf("dayofyear", "doy", "dy", "yearday").forEach { put(it, DOY) }
                    listOf("day", "days", "d").forEach { put(it, DAY) }
                    listOf("hour", "hours", "h", "hr", "hrs").forEach { put(it, HOUR) }
                    listOf("minute", "minutes", "m", "min", "mins").forEach { put(it, MINUTE) }
                    listOf("second", "seconds", "s", "sec", "secs").forEach { put(it, SECOND) }
                    listOf(
                        "millisecond", "milliseconds", "ms", "msec", "msecs", "msecond", "mseconds", "millisec", "millisecs", "millisecon",
                    ).forEach { put(it, MILLISECOND) }
                    listOf(
                        "microsecond", "microseconds", "microsec", "microsecs", "usecond", "useconds", "us", "usec", "usecs",
                    ).forEach { put(it, MICROSECOND) }
                    put("timezone", TIMEZONE)
                    put("timezone_hour", TIMEZONE_HOUR)
                    put("timezone_minute", TIMEZONE_MINUTE)
                }

            fun of(name: String): DatePart? = NAMES[name.lowercase()]
        }
    }

    internal companion object {
        /** The prefix of hidden PartiQL built-in function names; see `FunctionUtils` in partiql-spi. */
        const val SYSTEM_PREFIX_INTERNAL = "\uFDEF"

        /** Redshift stores TIME, TIMETZ, and TIMESTAMPTZ values with microsecond precision. */
        const val DATETIME_PRECISION = 6

        /** The date parts that DATEDIFF translates. */
        val DIFF_PARTS =
            setOf(
                DatePart.YEAR,
                DatePart.MONTH,
                DatePart.DAY,
                DatePart.HOUR,
                DatePart.MINUTE,
                DatePart.SECOND,
                DatePart.MILLISECOND,
                DatePart.MICROSECOND,
            )

        /** The date parts that DATEADD translates. */
        val ADD_PARTS = DIFF_PARTS + DatePart.WEEK

        /** The date parts of a time of day. */
        val TIME_PARTS = setOf(DatePart.HOUR, DatePart.MINUTE, DatePart.SECOND, DatePart.MILLISECOND, DatePart.MICROSECOND)

        /**
         * The reference timestamp of the boundary rules: the first instant of year 1, which is a Monday at the start of
         * a year, quarter, month, week, day, hour, minute, and second.
         */
        val REFERENCE: Expr get() = timestamp("0001-01-01 00:00:00")

        /** The first day of year 1, a Monday. */
        val REFERENCE_DATE: Expr get() = exprLit(Literal.typedString(DataType.DATE(), "0001-01-01"))

        /** Midnight, the reference of the boundary rules for TIME values. */
        val MIDNIGHT: Expr get() = exprLit(Literal.typedString(DataType.TIME(), "00:00:00"))

        /** The atom of the expression [ctx] if the expression is a single atom without operators, else null. */
        fun atomOf(ctx: ParserRuleContext): P.AtomContext? {
            var tree: ParserRuleContext = ctx
            while (true) {
                tree =
                    when (tree) {
                        is P.ExprContext -> tree.exprOr()
                        is P.OrBaseContext -> tree.exprAnd()
                        is P.AndBaseContext -> tree.exprNot()
                        is P.NotBaseContext -> tree.predicate()
                        is P.PredicateBaseContext -> tree.valueExpr()
                        is P.ValuePrimaryContext -> tree.primaryExpr()
                        is P.PrimaryAtomContext -> tree.atom()
                        is P.AtomContext -> return tree
                        else -> return null
                    }
            }
        }

        fun timestamp(value: String): Expr = exprLit(Literal.typedString(DataType.TIMESTAMP(), value))

        fun int(value: Int): Expr = exprLit(Literal.intNum(value))

        /** PartiQL `DATE_DIFF(part, a, b)`, the number of whole [part] units from [a] to [b]. */
        fun dateDiffCall(
            part: DatePart,
            a: Expr,
            b: Expr,
        ): Expr = exprCall(Identifier.regular("${SYSTEM_PREFIX_INTERNAL}date_diff_${part.name.lowercase()}"), listOf(a, b), null)

        /** PartiQL `DATE_ADD(part, n, x)`. */
        fun dateAddCall(
            part: DatePart,
            n: Expr,
            x: Expr,
        ): Expr = exprCall(Identifier.regular("${SYSTEM_PREFIX_INTERNAL}date_add_${part.name.lowercase()}"), listOf(n, x), null)

        /** The nanoseconds within the second of [x]: `CAST(EXTRACT(SECOND FROM x) * 1000000000 AS BIGINT) % 1000000000`. */
        fun nanosOfSecond(x: Expr): Expr {
            val nanos = exprCast(exprOperator("*", exprExtract(DatetimeField.SECOND(), x), int(1_000_000_000)), DataType.BIGINT())
            return exprOperator("%", nanos, int(1_000_000_000))
        }

        /** The interval of [value] seconds, with [fraction] fractional digits. */
        fun secondsInterval(
            value: String,
            fraction: Int,
        ): Expr = exprLit(Literal.typedString(DataType.INTERVAL(IntervalQualifier.Single(DatetimeField.SECOND(), 9, fraction)), value))

        /**
         * The UTC time of day of the TIMETZ value [x]: its local time minus its offset,
         * `CAST(x AS TIME) - (CAST(CAST(x AS TIME) AS TIMETZ) - x)`. A TIME value is returned as it is.
         */
        fun utcTime(
            x: Expr,
            kind: Kind,
        ): Expr {
            if (kind != Kind.TIMETZ) return x
            val local = exprCast(x, DataType.TIME(DATETIME_PRECISION))
            return exprOperator("-", local, exprOperator("-", exprCast(local, DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION)), x))
        }
    }
}

/**
 * Reads Redshift datetime character strings: the values of typed literals (`DATE '...'`) and the character strings
 * that Redshift converts to a datetime type. The translator reads the ISO format and the formats whose meaning does not
 * depend on session settings:
 *
 *  - Dates: `2008-02-15` (with 1 or 2 digit months and days), `20080215`, `Feb 15, 2008`, `February 15, 2008`,
 *    `2008-Feb-15`, `Feb-15-2008`, and `15-Feb-2008`. Formats that depend on the DateStyle setting (`2/15/2008`) and
 *    two-digit years are not read.
 *  - Times: `04:05:06.789`, `04:05`, `040506`, and `04:05 PM`; up to 6 fractional digits. `24:00:00` is not read.
 *  - Time zones: `+08`, `+0800`, `+08:00`, `Z`, `UTC`, and `GMT`. TIMESTAMP values also ignore the abbreviations of
 *    the US time zones.
 *  - `epoch`, the TIMESTAMP 1970-01-01 00:00:00.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_Date_and_time_literals.html
 */
internal object DatetimeText {
    class Date(
        val year: Int,
        val month: Int,
        val day: Int,
    ) {
        override fun toString(): String = "%04d-%02d-%02d".format(java.util.Locale.ROOT, year, month, day)
    }

    class Time(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val fraction: String,
    ) {
        val isMidnight: Boolean get() = hour == 0 && minute == 0 && second == 0 && fraction.all { it == '0' }

        override fun toString(): String =
            "%02d:%02d:%02d".format(java.util.Locale.ROOT, hour, minute, second) + if (fraction.isEmpty()) "" else ".$fraction"
    }

    /** A time zone: an offset from UTC in minutes, or a name whose offset the translator does not know. */
    class Zone(
        val minutes: Int?,
    ) {
        /** The offset in the PartiQL format (`+08:00`), or null for a time zone name. */
        fun offset(): String? {
            val m = minutes ?: return null
            val sign = if (m < 0) "-" else "+"
            return "%s%02d:%02d".format(java.util.Locale.ROOT, sign, Math.abs(m) / 60, Math.abs(m) % 60)
        }

        companion object {
            val UTC = Zone(0)
        }
    }

    /** A date with an optional time of day and time zone. */
    class Value(
        val date: Date,
        val time: Time?,
        val zone: Zone?,
    ) {
        /** The TIMESTAMP value in the PartiQL format; the time zone is ignored. */
        fun timestamp(): String = "$date ${time ?: MIDNIGHT}"

        /**
         * The TIMESTAMPTZ value in UTC in the PartiQL format, or null if the time zone is a name. A value without a time
         * zone is in UTC. An invalid date or time is written as it is, so that the PartiQL planner reports it.
         */
        fun utcTimestamp(): String? {
            val minutes = (zone ?: Zone.UTC).minutes ?: return null
            val d = date
            val t = time ?: MIDNIGHT
            val local =
                try {
                    java.time.LocalDateTime.of(d.year, d.month, d.day, t.hour, t.minute, t.second)
                } catch (ex: java.time.DateTimeException) {
                    return "$d $t${zone?.offset() ?: "+00:00"}"
                }
            val utc = local.minusMinutes(minutes.toLong())
            if (utc.year < 0 || utc.year > 9999) return null
            val utcDate = Date(utc.year, utc.monthValue, utc.dayOfMonth)
            val utcTime = Time(utc.hour, utc.minute, utc.second, t.fraction)
            return "$utcDate $utcTime+00:00"
        }
    }

    private val MIDNIGHT = Time(0, 0, 0, "")

    /** The special values that Redshift evaluates when the query runs. */
    private val SPECIAL = setOf("now", "today", "tomorrow", "yesterday", "infinity", "-infinity", "allballs")

    private val MONTHS: Map<String, Int> =
        listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            .withIndex()
            .associate { (i, name) -> name to i + 1 } +
            listOf(
                "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
            ).withIndex().associate { (i, name) -> name to i + 1 } + mapOf("sept" to 9)

    /** Date formats, each with a function that reads year, month, and day. */
    private val DATE_FORMATS: List<Pair<Regex, (MatchResult) -> Date?>> =
        listOf(
            Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""") to { m -> date(m.groupValues[1], m.groupValues[2], m.groupValues[3]) },
            Regex("""^(\d{4})(\d{2})(\d{2})""") to { m -> date(m.groupValues[1], m.groupValues[2], m.groupValues[3]) },
            Regex("""^([a-z]+)\s+(\d{1,2}),\s*(\d{4})""", RegexOption.IGNORE_CASE) to { m ->
                month(m.groupValues[1])?.let { date(m.groupValues[3], it.toString(), m.groupValues[2]) }
            },
            Regex("""^(\d{4})-([a-z]+)-(\d{1,2})""", RegexOption.IGNORE_CASE) to { m ->
                month(m.groupValues[2])?.let { date(m.groupValues[1], it.toString(), m.groupValues[3]) }
            },
            Regex("""^([a-z]+)-(\d{1,2})-(\d{4})""", RegexOption.IGNORE_CASE) to { m ->
                month(m.groupValues[1])?.let { date(m.groupValues[3], it.toString(), m.groupValues[2]) }
            },
            Regex("""^(\d{1,2})-([a-z]+)-(\d{4})""", RegexOption.IGNORE_CASE) to { m ->
                month(m.groupValues[2])?.let { date(m.groupValues[3], it.toString(), m.groupValues[1]) }
            },
        )

    private val TIME = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?""")
    private val COMPACT_TIME = Regex("""^(\d{2})(\d{2})(\d{2})(?:\.(\d+))?""")
    private val MERIDIEM = Regex("""^\s*(AM|PM)(?![a-z])""", RegexOption.IGNORE_CASE)
    private val OFFSET = Regex("""^\s*([+-])(\d{1,2})(?::?(\d{2}))?$""")
    private val UTC = Regex("""^\s*(Z|UTC|GMT)$""", RegexOption.IGNORE_CASE)

    /** The abbreviations of the US time zones, which a TIMESTAMP value ignores. */
    private val ZONE_NAMES = setOf("EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "PDT")

    private fun date(
        year: String,
        month: String,
        day: String,
    ): Date? {
        val y = year.toInt()
        if (y == 0) return null
        return Date(y, month.toInt(), day.toInt())
    }

    private fun month(name: String): Int? = MONTHS[name.lowercase()]

    fun isSpecial(text: String): Boolean = text.trim().lowercase() in SPECIAL

    /**
     * Reads a date with an optional time and time zone, or `epoch` if [epoch] is true; null if the format is not read.
     */
    fun readTimestamp(
        text: String,
        epoch: Boolean = true,
    ): Value? {
        val s = text.trim()
        if (s.equals("epoch", ignoreCase = true)) return if (epoch) Value(Date(1970, 1, 1), MIDNIGHT, null) else null
        for ((regex, read) in DATE_FORMATS) {
            val match = regex.find(s) ?: continue
            val rest = s.substring(match.range.last + 1)
            if (rest.isNotEmpty() && !rest[0].isWhitespace()) continue
            val date = read(match) ?: return null
            if (rest.isBlank()) return Value(date, null, null)
            val time = readTime(rest.trim()) ?: return null
            return Value(date, time.time, time.zone)
        }
        return null
    }

    /** A time of day with an optional time zone. */
    class TimeValue(
        val time: Time,
        val zone: Zone?,
    )

    /** Reads a time with an optional time zone; null if the format is not read. */
    fun readTime(text: String): TimeValue? {
        val s = text.trim()
        val match = TIME.find(s) ?: COMPACT_TIME.find(s) ?: return null
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        val second = match.groupValues[3].ifEmpty { "0" }.toInt()
        val fraction = match.groupValues[4]
        if (fraction.length > 6) return null
        var rest = s.substring(match.range.last + 1)
        MERIDIEM.find(rest)?.let { m ->
            if (hour > 12) return null
            val pm = m.groupValues[1].equals("PM", ignoreCase = true)
            hour = if (pm) (if (hour == 12) 12 else hour + 12) else (if (hour == 12) 0 else hour)
            rest = rest.substring(m.range.last + 1)
        }
        // Redshift accepts 24:00:00, which has no PartiQL value.
        if (hour == 24) return null
        val zone =
            when {
                rest.isBlank() -> null
                else -> readZone(rest) ?: return null
            }
        return TimeValue(Time(hour, minute, second, fraction), zone)
    }

    private fun readZone(text: String): Zone? {
        OFFSET.find(text)?.let { m ->
            val hours = m.groupValues[2].toInt()
            val minutes = m.groupValues[3].ifEmpty { "0" }.toInt()
            // Redshift, as PostgreSQL, accepts offsets of less than 16 hours.
            if (hours > 15 || minutes > 59) return null
            val offset = hours * 60 + minutes
            return Zone(if (m.groupValues[1] == "-") -offset else offset)
        }
        if (UTC.matches(text)) return Zone.UTC
        if (text.trim().uppercase() in ZONE_NAMES) return Zone(null)
        return null
    }

    /**
     * The PartiQL literal of the datetime type [code] (a [PType] code) for the Redshift character string [value], or
     * null; see [RedshiftDatetime.stringToDatetime].
     */
    fun literal(
        value: String,
        code: Int,
        dropTime: Boolean,
    ): Expr? =
        when (code) {
            PType.DATE -> {
                val parts = readTimestamp(value, epoch = false)
                if (parts == null) {
                    null
                } else if (!dropTime && parts.time != null && !parts.time.isMidnight) {
                    null
                } else {
                    exprLit(Literal.typedString(DataType.DATE(), parts.date.toString()))
                }
            }
            PType.TIMESTAMP -> readTimestamp(value)?.let { exprLit(Literal.typedString(DataType.TIMESTAMP(), it.timestamp())) }
            PType.TIMESTAMPZ ->
                readTimestamp(value)?.utcTimestamp()?.let { exprLit(Literal.typedString(DataType.TIMESTAMP_WITH_TIME_ZONE(), it)) }
            PType.TIME -> {
                val parts = readTime(value)
                if (parts == null || parts.zone != null) null else exprLit(Literal.typedString(DataType.TIME(), parts.time.toString()))
            }
            PType.TIMEZ -> {
                val parts = readTime(value)
                val offset = parts?.let { (it.zone ?: Zone.UTC).offset() }
                if (parts == null || offset == null) {
                    null
                } else {
                    exprLit(
                        Literal.typedString(DataType.TIME_WITH_TIME_ZONE(), "${parts.time}$offset"),
                    )
                }
            }
            else -> null
        }
}

/**
 * Reads Redshift interval literals.
 *
 * A literal with a qualifier has a number in the format of the qualifier: `'10'` for `DAY`, `'1-2'` for `YEAR TO
 * MONTH`, `'1 2:03:04.5'` for `DAY TO SECOND`. The fields after the first are in range (fewer than 12 months, 24 hours,
 * 60 minutes, and 60 seconds), and the seconds have at most the fractional digits of the qualifier. A leading sign
 * applies to the whole value; for a value with a day field and a time, each has its own sign in Redshift, so both must
 * have the same sign. Units in the string (`'5 hours' HOUR`) are not read.
 *
 * A literal without a qualifier is a list of quantities with units: `'3 hours, 35 minutes'`, `'1w, 1h, 1m, 1s'`,
 * `'0.5 days'`, `'4 months'`; each quantity has its own sign. Years and months, and weeks to seconds, cannot be mixed.
 * Redshift keeps a time of day of 24 hours or more (`'36 hours'`) and a day and a time of different signs as they are,
 * where PartiQL normalizes the fields; such a value is [Interval.normalized] false.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_data_types.html
 * https://docs.aws.amazon.com/redshift/latest/dg/r_interval_literals.html
 */
internal object IntervalText {
    enum class Field { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND }

    /** An interval qualifier from [start] to [end], with the fractional precision of seconds. */
    class Qualifier(
        val start: Field,
        val end: Field,
        val fraction: Int?,
    ) {
        val isYearMonth: Boolean get() = start == Field.YEAR || start == Field.MONTH

        val isValid: Boolean
            get() =
                when {
                    start == end -> true
                    start == Field.YEAR -> end == Field.MONTH
                    start == Field.MONTH -> false
                    else -> end > start
                }
    }

    /**
     * An interval value: [months] for the YEAR TO MONTH family, or [micros] for the DAY TO SECOND family. [fields] are
     * the first and last fields of the PartiQL qualifier, and [fraction] is the fractional precision of seconds.
     */
    class Interval(
        private val months: BigInteger?,
        private val micros: BigInteger?,
        private val fields: Pair<Field, Field>,
        private val fraction: Int,
        val normalized: Boolean,
    ) {
        val negative: Boolean get() = (months ?: micros!!).signum() < 0

        val lessThanOneSecond: Boolean get() = micros != null && micros.abs() < MICROS_PER_SECOND

        fun qualifier(): IntervalQualifier {
            val (start, end) = fields
            val fractionalPrecision = if (end == Field.SECOND) fraction else null
            return if (start == end) {
                IntervalQualifier.Single(datetimeField(start), PRECISION, fractionalPrecision)
            } else {
                IntervalQualifier.Range(datetimeField(start), PRECISION, datetimeField(end), fractionalPrecision)
            }
        }

        /** The value in the PartiQL format of [qualifier]. */
        fun text(): String {
            val sign = if (negative) "-" else ""
            val (start, end) = fields
            if (months != null) {
                val m = months.abs()
                return sign +
                    when {
                        start == Field.YEAR && end == Field.YEAR -> "${m / TWELVE}"
                        start == Field.YEAR -> "${m / TWELVE}-${m % TWELVE}"
                        else -> "$m"
                    }
            }
            val total = micros!!.abs()
            val values =
                mapOf(
                    Field.DAY to total / MICROS_PER_DAY,
                    Field.HOUR to total % MICROS_PER_DAY / MICROS_PER_HOUR,
                    Field.MINUTE to total % MICROS_PER_HOUR / MICROS_PER_MINUTE,
                    Field.SECOND to total % MICROS_PER_MINUTE / MICROS_PER_SECOND,
                )
            // The leading field holds the larger units.
            val leading =
                when (start) {
                    Field.DAY -> values.getValue(Field.DAY)
                    Field.HOUR -> total / MICROS_PER_HOUR
                    Field.MINUTE -> total / MICROS_PER_MINUTE
                    else -> total / MICROS_PER_SECOND
                }
            val text = StringBuilder(leading.toString())
            var field = start
            while (field != end) {
                field = Field.values()[field.ordinal + 1]
                text.append(if (field == Field.HOUR) " " else ":").append(values.getValue(field))
            }
            if (end == Field.SECOND) {
                val micro = total % MICROS_PER_SECOND
                if (micro.signum() != 0) text.append(".").append(micro.toString().padStart(6, '0').trimEnd('0'))
            }
            return sign + text
        }

        /** True if the leading field fits the PartiQL precision. */
        fun fits(): Boolean {
            val (start, _) = fields
            val leading =
                if (months != null) {
                    if (start == Field.YEAR) months.abs() / TWELVE else months.abs()
                } else {
                    val unit =
                        when (start) {
                            Field.DAY -> MICROS_PER_DAY
                            Field.HOUR -> MICROS_PER_HOUR
                            Field.MINUTE -> MICROS_PER_MINUTE
                            else -> MICROS_PER_SECOND
                        }
                    micros!!.abs() / unit
                }
            return leading < MAX_LEADING
        }
    }

    private const val PRECISION = 9
    private val MAX_LEADING = BigInteger.TEN.pow(PRECISION)
    private val TWELVE = BigInteger.valueOf(12)
    private val MICROS_PER_SECOND = BigInteger.valueOf(1_000_000)
    private val MICROS_PER_MINUTE = BigInteger.valueOf(60_000_000)
    private val MICROS_PER_HOUR = BigInteger.valueOf(3_600_000_000)
    private val MICROS_PER_DAY = BigInteger.valueOf(86_400_000_000)

    private fun datetimeField(field: Field): DatetimeField = DatetimeField.parse(field.name)

    /** The PartiQL value of a literal with the qualifier [qualifier], or null if the string is not read. */
    fun qualified(
        text: String,
        qualifier: Qualifier,
    ): Interval? {
        val s = text.trim()
        val fraction = qualifier.fraction ?: 6
        val (start, end) = qualifier.start to qualifier.end
        val value: Interval =
            if (qualifier.isYearMonth) {
                val months: BigInteger =
                    when {
                        start == end -> {
                            val m = Regex("""^([+-]?)(\d+)$""").find(s) ?: return null
                            signed(m.groupValues[1], BigInteger(m.groupValues[2]) * if (start == Field.YEAR) TWELVE else BigInteger.ONE)
                        }
                        else -> {
                            val m = Regex("""^([+-]?)(\d+)-(\d+)$""").find(s) ?: return null
                            val month = BigInteger(m.groupValues[3])
                            if (month >= TWELVE) return null
                            signed(m.groupValues[1], BigInteger(m.groupValues[2]) * TWELVE + month)
                        }
                    }
                // A negative value is written from its first nonzero field; see [Interval.text].
                val zeroYears = months.signum() < 0 && start == Field.YEAR && months.abs() < TWELVE
                val fields = if (zeroYears) Field.MONTH to Field.MONTH else start to end
                Interval(months, null, fields, fraction, normalized = true)
            } else {
                val micros = dayTime(s, start, end, fraction) ?: return null
                Interval(null, micros, dayTimeFields(micros, start, end), fraction, normalized = true)
            }
        return if (value.fits()) value else null
    }

    /**
     * The microseconds of a DAY TO SECOND family string with the fields [start] to [end], or null.
     */
    private fun dayTime(
        s: String,
        start: Field,
        end: Field,
        fraction: Int,
    ): BigInteger? {
        var rest = s
        var daySign = ""
        var days = BigInteger.ZERO
        if (start == Field.DAY) {
            val m = Regex("""^([+-]?)(\d+)""").find(rest) ?: return null
            daySign = m.groupValues[1]
            days = BigInteger(m.groupValues[2])
            rest = rest.substring(m.range.last + 1)
            if (end == Field.DAY) return if (rest.isEmpty()) signed(daySign, days * MICROS_PER_DAY) else null
            if (rest.isEmpty() || !rest[0].isWhitespace()) return null
            rest = rest.trimStart()
        }
        // The time fields from the first time field to [end].
        val first = if (start == Field.DAY) Field.HOUR else start
        val count = end.ordinal - first.ordinal + 1
        val numbers = (1..count).joinToString(":") { """(\d+)""" }
        val pattern = if (end == Field.SECOND) """^([+-]?)$numbers(?:\.(\d+))?$""" else """^([+-]?)$numbers$"""
        val m = Regex(pattern).find(rest) ?: return null
        val timeSign = m.groupValues[1]
        var micros = BigInteger.ZERO
        for (i in 0 until count) {
            val field = Field.values()[first.ordinal + i]
            val v = BigInteger(m.groupValues[2 + i])
            // The fields after the first are in range.
            val isFirst = i == 0 && start != Field.DAY
            val limit =
                when (field) {
                    Field.HOUR -> 24L
                    else -> 60L
                }
            if (!isFirst && v >= BigInteger.valueOf(limit)) return null
            micros += v * unitMicros(field)
        }
        if (end == Field.SECOND) {
            val digits = m.groupValues[2 + count]
            if (digits.length > fraction) return null
            if (digits.isNotEmpty()) micros += BigInteger(digits.padEnd(6, '0'))
        }
        if (start != Field.DAY) return signed(timeSign, micros)
        // Redshift reads the sign of the days and of the time separately.
        val dayNegative = daySign == "-" && days.signum() != 0
        val timeNegative = timeSign == "-" && micros.signum() != 0
        if (days.signum() != 0 && micros.signum() != 0 && dayNegative != timeNegative) return null
        val total = days * MICROS_PER_DAY + micros
        return if (dayNegative || timeNegative) total.negate() else total
    }

    private fun unitMicros(field: Field): BigInteger =
        when (field) {
            Field.DAY -> MICROS_PER_DAY
            Field.HOUR -> MICROS_PER_HOUR
            Field.MINUTE -> MICROS_PER_MINUTE
            else -> MICROS_PER_SECOND
        }

    /**
     * The fields of the PartiQL qualifier of a DAY TO SECOND family value. A positive value keeps the Redshift
     * qualifier. A negative value starts with its first nonzero field, because the PartiQL printer writes the sign on
     * the leading field.
     */
    private fun dayTimeFields(
        micros: BigInteger,
        start: Field,
        end: Field,
    ): Pair<Field, Field> {
        if (micros.signum() >= 0) return start to end
        val total = micros.abs()
        val first =
            listOf(Field.DAY, Field.HOUR, Field.MINUTE, Field.SECOND)
                .filter { it >= start && it <= end }
                .firstOrNull { total >= unitMicros(it) } ?: end
        return first to end
    }

    private fun signed(
        sign: String,
        value: BigInteger,
    ): BigInteger = if (sign == "-") value.negate() else value

    /** A quantity with an optional unit, and an optional comma: `3 hours,`. */
    private val QUANTITY = Regex("""\s*([+-]?)(\d+(?:\.\d+)?)\s*([a-z]*)\s*,?""", RegexOption.IGNORE_CASE)

    private val YEAR_UNITS = setOf("year", "years", "y", "yr", "yrs")
    private val MONTH_UNITS = setOf("month", "months", "mon", "mons")
    private val DAY_UNITS: Map<String, BigInteger> =
        listOf("week", "weeks", "w").associateWith { MICROS_PER_DAY * BigInteger.valueOf(7) } +
            listOf("day", "days", "d").associateWith { MICROS_PER_DAY }
    private val TIME_UNITS: Map<String, BigInteger> =
        listOf("hour", "hours", "h", "hr", "hrs").associateWith { MICROS_PER_HOUR } +
            listOf("minute", "minutes", "m", "min", "mins").associateWith { MICROS_PER_MINUTE } +
            listOf("second", "seconds", "s", "sec", "secs").associateWith { MICROS_PER_SECOND }

    /** The PartiQL value of a literal without a qualifier, or null if the string is not read. */
    fun unqualified(text: String): Interval? {
        val s = text.trim()
        var months: BigInteger? = null
        var monthUnit = false
        // Redshift keeps the days (from days and weeks) and the time of day (from hours, minutes, and seconds) apart.
        var days = BigDecimal.ZERO
        var time = BigDecimal.ZERO
        var fractionalDays = false
        var quantities = 0
        var bare = false
        var index = 0
        while (index < s.length) {
            val m = QUANTITY.matchAt(s, index) ?: return null
            index = m.range.last + 1
            quantities++
            val quantity = BigDecimal(m.groupValues[2]).let { if (m.groupValues[1] == "-") it.negate() else it }
            val whole = quantity.stripTrailingZeros().scale() <= 0
            when (val unit = m.groupValues[3].lowercase()) {
                "" -> {
                    bare = true
                    time += quantity * BigDecimal(MICROS_PER_SECOND)
                }
                in YEAR_UNITS, in MONTH_UNITS -> {
                    if (!whole) return null
                    val perUnit = if (unit in YEAR_UNITS) TWELVE else BigInteger.ONE
                    monthUnit = monthUnit || unit in MONTH_UNITS
                    months = (months ?: BigInteger.ZERO) + quantity.toBigIntegerExact() * perUnit
                }
                in DAY_UNITS -> {
                    if (!whole) fractionalDays = true
                    days += quantity * BigDecimal(DAY_UNITS.getValue(unit))
                }
                in TIME_UNITS -> time += quantity * BigDecimal(TIME_UNITS.getValue(unit))
                else -> return null
            }
        }
        // A number without a unit is a number of seconds only on its own.
        if (quantities == 0 || (bare && quantities > 1)) return null
        if (months != null) {
            if (days.signum() != 0 || time.signum() != 0) return null
            val field = if (!monthUnit && months.mod(TWELVE).signum() == 0) Field.YEAR else Field.MONTH
            return Interval(months, null, field to field, 6, normalized = true).takeIf { it.fits() }
        }
        val micros = runCatching { (days + time).toBigIntegerExact() }.getOrNull() ?: return null
        // PartiQL normalizes the fields of the value; Redshift has the same fields if the time of day is shorter than a
        // day and has the sign of the days. A fraction of a day becomes a time of day in Redshift.
        val normalized =
            !fractionalDays &&
                time.abs() < BigDecimal(MICROS_PER_DAY) &&
                (days.signum() == 0 || time.signum() == 0 || days.signum() == time.signum())
        // The PartiQL qualifier is from the first to the last nonzero field.
        val total = micros.abs()
        val nonzero =
            listOf(
                Field.DAY to (total >= MICROS_PER_DAY),
                Field.HOUR to (total % MICROS_PER_DAY >= MICROS_PER_HOUR),
                Field.MINUTE to (total % MICROS_PER_HOUR >= MICROS_PER_MINUTE),
                Field.SECOND to (total % MICROS_PER_MINUTE).let { it.signum() != 0 },
            ).filter { it.second }.map { it.first }
        val fields = if (nonzero.isEmpty()) Field.SECOND to Field.SECOND else nonzero.first() to nonzero.last()
        return Interval(null, micros, fields, 6, normalized).takeIf { it.fits() }
    }
}
