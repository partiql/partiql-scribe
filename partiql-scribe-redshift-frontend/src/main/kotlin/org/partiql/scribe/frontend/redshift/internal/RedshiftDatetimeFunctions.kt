package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprExtract
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.REFERENCE
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.REFERENCE_DATE
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.dateAddCall
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.dateDiffCall
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.int
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.nanosOfSecond
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.secondsInterval
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Companion.timestamp
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.DatePart
import org.partiql.scribe.frontend.redshift.internal.RedshiftDatetime.Kind
import org.partiql.spi.types.PType
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The rules that translate Redshift date and time functions with the function call syntax; see [RedshiftFunctions].
 * `DATEADD`, `DATEDIFF`, `EXTRACT`, and the datetime keywords and operators are in [RedshiftDatetime], which shares the
 * rules for date parts, time zones, and character string arguments in this class.
 */
internal class RedshiftDatetimeFunctions(
    private val functions: RedshiftFunctions,
) {
    private val types = functions.types

    val rules: Map<String, RedshiftFunctions.Rule> =
        mapOf(
            // https://docs.aws.amazon.com/redshift/latest/dg/r_GETDATE.html
            "getdate" to
                RedshiftFunctions.Rule { name, args, setq ->
                    scalar(name, args, setq, 0)
                    getdate(name)
                },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_PART_function.html
            "date_part" to RedshiftFunctions.Rule { name, args, setq -> datePartFunction(name, args, setq) },
            "pgdate_part" to RedshiftFunctions.Rule { name, args, setq -> datePartFunction(name, args, setq) },
            "date_part_year" to RedshiftFunctions.Rule { name, args, setq -> datePartYear(name, args, setq) },
            "date_trunc" to RedshiftFunctions.Rule { name, args, setq -> dateTrunc(name, args, setq) },
            "last_day" to RedshiftFunctions.Rule { name, args, setq -> lastDay(name, args, setq) },
            "add_months" to RedshiftFunctions.Rule { name, args, setq -> addMonths(name, args, setq) },
            "next_day" to RedshiftFunctions.Rule { name, args, setq -> nextDay(name, args, setq) },
            "convert_timezone" to RedshiftFunctions.Rule { name, args, setq -> convertTimezone(name, args, setq) },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_TIMEZONE.html
            "timezone" to
                RedshiftFunctions.Rule { name, args, setq ->
                    scalar(name, args, setq, 2)
                    val zone =
                        stringLiteral(args[0].expr)
                            ?: throw UnsupportedConstructException.at(args[0].ctx, "TIMEZONE with a time zone that is not a literal")
                    atTimeZone(name, "TIMEZONE", zone, args[1], stringValues = true)
                },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_CMP.html and the related pages.
            "date_cmp" to compare("DATE_CMP", Kind.DATE, Kind.DATE),
            "date_cmp_timestamp" to compare("DATE_CMP_TIMESTAMP", Kind.DATE, Kind.TIMESTAMP),
            "date_cmp_timestamptz" to compare("DATE_CMP_TIMESTAMPTZ", Kind.DATE, Kind.TIMESTAMPTZ),
            "timestamp_cmp" to compare("TIMESTAMP_CMP", Kind.TIMESTAMP, Kind.TIMESTAMP),
            "timestamp_cmp_date" to compare("TIMESTAMP_CMP_DATE", Kind.TIMESTAMP, Kind.DATE),
            "timestamp_cmp_timestamptz" to compare("TIMESTAMP_CMP_TIMESTAMPTZ", Kind.TIMESTAMP, Kind.TIMESTAMPTZ),
            "timestamptz_cmp" to compare("TIMESTAMPTZ_CMP", Kind.TIMESTAMPTZ, Kind.TIMESTAMPTZ),
            "timestamptz_cmp_date" to compare("TIMESTAMPTZ_CMP_DATE", Kind.TIMESTAMPTZ, Kind.DATE),
            "timestamptz_cmp_timestamp" to compare("TIMESTAMPTZ_CMP_TIMESTAMP", Kind.TIMESTAMPTZ, Kind.TIMESTAMP),
        )

    /*
     * Current date and time
     */

    /**
     * `GETDATE()` is the start time of the current statement as a TIMESTAMP in the session time zone, which is assumed
     * to be UTC, without fractional seconds: `DATE_ADD(SECOND, DATE_DIFF(SECOND, R, UTCNOW()), R)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_GETDATE.html
     */
    private fun getdate(name: Token): Expr {
        functions.note(
            name,
            "Redshift `GETDATE()` was replaced by PartiQL `UTCNOW()` truncated to the second as a TIMESTAMP; this assumes the " +
                "Redshift session time zone is UTC (the default). PartiQL evaluates UTCNOW() for each call and row; Redshift " +
                "returns one value per statement",
        )
        return dateAddCall(DatePart.SECOND, dateDiffCall(DatePart.SECOND, REFERENCE, utcnow()), REFERENCE)
    }

    /**
     * `SYSDATE` and `CURRENT_DATE`: `UTCNOW()` cast to [type]. PartiQL `UTCNOW()` is a TIMESTAMP WITH TIME ZONE in
     * UTC, and the cast keeps the UTC date and time.
     */
    internal fun now(
        name: Token,
        source: String,
        replacement: String,
        type: DataType,
    ): Expr {
        functions.note(
            name,
            "Redshift `$source` was replaced by PartiQL $replacement; this assumes the Redshift session time zone is UTC (the " +
                "default). PartiQL evaluates UTCNOW() for each call and row; Redshift returns the start time of the transaction",
        )
        return exprCast(utcnow(), type)
    }

    private fun utcnow(): Expr = exprCall(Identifier.regular("utcnow"), emptyList(), null)

    /*
     * Date parts
     */

    /**
     * `DATE_PART(part, x)` and `PGDATE_PART(part, x)`: the date part as DOUBLE PRECISION. The date part is a name or a
     * character string. See [datePart].
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_PART_function.html
     */
    private fun datePartFunction(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 2)
        val partName =
            datePartName(args[0])
                ?: throw UnsupportedConstructException.at(args[0].ctx, "${name.text.uppercase()} datepart `${args[0].ctx.text}`")
        return datePart(name, name.text.uppercase(), args[0].ctx.start, partName, args[1])
    }

    /** The date part name of [arg]: a name or a character string literal. */
    private fun datePartName(arg: Operand): String? =
        when (val atom = RedshiftDatetime.atomOf(arg.ctx)) {
            is P.AtomVariableContext -> atom.identifier().text.removeSurrounding("\"")
            is P.AtomLiteralContext -> stringLiteral(arg.expr)
            else -> null
        }

    /**
     * `EXTRACT(part FROM x)` ([function] `EXTRACT`) and `DATE_PART(part, x)`.
     *
     * Redshift `EXTRACT` returns INTEGER, and DOUBLE PRECISION for a TIMESTAMPTZ value; `DATE_PART` returns DOUBLE
     * PRECISION. Redshift and PartiQL have the same year, month, day, hour, and minute fields. The other fields are
     * computed from them:
     *  - `second`: Redshift `EXTRACT` returns whole seconds of a datetime value; PartiQL `EXTRACT(SECOND ...)` returns a
     *    DECIMAL with the fraction. For an interval, Redshift keeps the fraction.
     *  - `millisecond`: the milliseconds within the second.
     *  - `quarter`, `dow` (0 is Sunday), `doy`, `week` (ISO 8601), `decade` (decade 1 is the years 1 to 9),
     *    `century` and `millennium` (which start with the year 1).
     *  - `epoch` of a DATE value (`DATE_PART` only): the seconds since 1970-01-01.
     *
     * The fields of a TIMESTAMPTZ value are its fields in UTC, the session time zone. `microsecond`, `timezone`, the
     * `epoch` of a timestamp, and the DOUBLE PRECISION `second` of `DATE_PART` are not translated: the Redshift
     * documentation does not specify their values. A character string literal argument of `DATE_PART` is a TIMESTAMP
     * value; `EXTRACT` from a character string is not translated, because its result type depends on how Redshift
     * reads the string.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_EXTRACT_function.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_PART_function.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Dateparts_for_datetime_functions.html
     */
    internal fun datePart(
        location: Token,
        function: String,
        partToken: Token,
        partName: String,
        source: Operand,
    ): Expr {
        val isExtract = function == "EXTRACT"
        val unsupportedPart = { UnsupportedConstructException.at(partToken, "$function datepart `$partName`") }
        val part = DatePart.of(partName) ?: throw unsupportedPart()
        when (part) {
            DatePart.MICROSECOND, DatePart.TIMEZONE, DatePart.TIMEZONE_HOUR, DatePart.TIMEZONE_MINUTE -> throw unsupportedPart()
            DatePart.EPOCH -> if (isExtract) throw unsupportedPart()
            DatePart.SECOND, DatePart.MILLISECOND -> if (!isExtract) throw unsupportedPart()
            else -> {}
        }
        return functions.resolveOperands(listOf(source.ctx to source.expr), typing = "$function:$partName") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            if (isExtract && Kind.of(t[0]) == Kind.STRING) {
                // Redshift can read the string as a TIMESTAMPTZ value, for which EXTRACT returns DOUBLE PRECISION.
                throw UnsupportedConstructException.at(source.ctx, "EXTRACT from a character string")
            }
            val (x, kind) = datetimeArgument(function, source, e[0], Kind.of(t[0]), PType.TIMESTAMP)
            when (kind) {
                Kind.DATE, Kind.TIMESTAMP, Kind.TIMESTAMPTZ -> {
                    if (part == DatePart.EPOCH) {
                        if (kind != Kind.DATE) {
                            throw UnsupportedConstructException.at(
                                location,
                                "$function(epoch, ...) of a ${kind.display} value",
                            )
                        }
                        functions.note(
                            location,
                            "Redshift `$function($partName, ...)` was replaced by PartiQL " +
                                "`DATE_DIFF(SECOND, TIMESTAMP '1970-01-01 00:00:00', ...)` cast to DOUBLE PRECISION",
                        )
                        return@resolveOperands exprCast(dateDiffCall(DatePart.SECOND, EPOCH, x), DataType.DOUBLE_PRECISION())
                    }
                    if (kind == Kind.TIMESTAMPTZ && isExtract && (part == DatePart.SECOND || part == DatePart.MILLISECOND)) {
                        throw UnsupportedConstructException.at(location, "EXTRACT(${part.name} ...) of a TIMESTAMPTZ value")
                    }
                    // The fields of a TIMESTAMPTZ value in UTC.
                    val value = if (kind == Kind.TIMESTAMPTZ) exprCast(x, DataType.TIMESTAMP()) else x
                    val (field, description) = field(part, value)
                    if (isExtract && kind != Kind.TIMESTAMPTZ) {
                        if (description != null) functions.note(location, "Redshift `EXTRACT($partName ...)` was replaced by $description")
                        field
                    } else {
                        val replacement = description ?: "PartiQL `EXTRACT(${part.name} ...)`"
                        val message =
                            if (isExtract) {
                                "Redshift `EXTRACT($partName ...)` returns DOUBLE PRECISION for a TIMESTAMPTZ value; it was replaced by " +
                                    "$replacement cast to DOUBLE PRECISION"
                            } else {
                                "Redshift `$function($partName, ...)` was replaced by $replacement cast to DOUBLE PRECISION"
                            }
                        functions.note(location, message)
                        exprCast(field, DataType.DOUBLE_PRECISION())
                    }
                }
                Kind.TIME, Kind.TIMETZ -> {
                    if (!isExtract) throw UnsupportedConstructException.at(location, "$function with ${kind.argument}")
                    if (part !in TIME_FIELDS) {
                        throw UnsupportedConstructException.at(
                            partToken,
                            "EXTRACT datepart `$partName` of a ${kind.display} value",
                        )
                    }
                    val (field, description) = field(part, x)
                    if (description != null) functions.note(location, "Redshift `EXTRACT($partName ...)` was replaced by $description")
                    field
                }
                Kind.INTERVAL_YM, Kind.INTERVAL_DT -> {
                    val fields = if (kind == Kind.INTERVAL_YM) YEAR_MONTH_FIELDS else DAY_TIME_FIELDS
                    if (!isExtract || part !in fields) {
                        throw UnsupportedConstructException.at(partToken, "$function datepart `$partName` of an ${kind.display} value")
                    }
                    if (part == DatePart.SECOND) {
                        functions.note(
                            location,
                            "Redshift `EXTRACT($partName ...)` of an interval returns the seconds with the fraction; PartiQL " +
                                "returns them as a DECIMAL",
                        )
                    }
                    exprExtract(DatetimeField.parse(part.name), x)
                }
                else -> throw UnsupportedConstructException.at(source.ctx, "$function with ${kind.argument}")
            }
        }
    }

    /**
     * The INTEGER value of the field [part] of the DATE, TIMESTAMP, TIME, or TIMETZ value [x], and a description of the
     * computation if it is not the PartiQL `EXTRACT` of the same field.
     */
    private fun field(
        part: DatePart,
        x: Expr,
    ): Pair<Expr, String?> {
        fun extract(field: DatetimeField) = exprExtract(field, x)

        fun days(): Expr = dateDiffCall(DatePart.DAY, REFERENCE, x)
        val year = { extract(DatetimeField.YEAR()) }
        return when (part) {
            DatePart.YEAR, DatePart.MONTH, DatePart.DAY, DatePart.HOUR, DatePart.MINUTE -> extract(DatetimeField.parse(part.name)) to null
            DatePart.SECOND -> {
                val nanos = exprCast(exprOperator("*", extract(DatetimeField.SECOND()), int(NANOS_PER_SECOND)), DataType.BIGINT())
                exprCast(exprOperator("/", nanos, int(NANOS_PER_SECOND)), DataType.INT()) to
                    "PartiQL `EXTRACT(SECOND ...)` without the fraction"
            }
            DatePart.MILLISECOND ->
                exprCast(exprOperator("/", nanosOfSecond(x), int(1_000_000)), DataType.INT()) to
                    "arithmetic on PartiQL `EXTRACT(SECOND ...)`"
            DatePart.QUARTER ->
                exprOperator("+", exprOperator("/", exprOperator("-", extract(DatetimeField.MONTH()), int(1)), int(3)), int(1)) to
                    "arithmetic on PartiQL `EXTRACT(MONTH ...)`"
            // The reference is a Monday, so the day of the week with 0 for Sunday is (days + 1) % 7.
            DatePart.DOW ->
                exprCast(exprOperator("%", exprOperator("+", days(), int(1)), int(7)), DataType.INT()) to "PartiQL date arithmetic"
            DatePart.DOY -> {
                val newYear = dateAddCall(DatePart.YEAR, dateDiffCall(DatePart.YEAR, REFERENCE, x), REFERENCE)
                exprCast(exprOperator("+", dateDiffCall(DatePart.DAY, newYear, x), int(1)), DataType.INT()) to "PartiQL date arithmetic"
            }
            // The ISO 8601 week of a day is the week of the Thursday of its week (Monday to Sunday) in the year of that
            // Thursday.
            DatePart.WEEK -> {
                val monday = exprOperator("-", days(), exprOperator("%", days(), int(7)))
                val thursday = dateAddCall(DatePart.DAY, exprOperator("+", monday, int(3)), REFERENCE_DATE)
                val newYear = dateAddCall(DatePart.YEAR, dateDiffCall(DatePart.YEAR, REFERENCE, thursday), REFERENCE_DATE)
                val week = exprOperator("+", exprOperator("/", dateDiffCall(DatePart.DAY, newYear, thursday), int(7)), int(1))
                exprCast(week, DataType.INT()) to "PartiQL date arithmetic for the ISO 8601 week"
            }
            DatePart.DECADE -> exprOperator("+", exprOperator("/", year(), int(10)), int(1)) to "arithmetic on PartiQL `EXTRACT(YEAR ...)`"
            DatePart.CENTURY ->
                exprOperator("+", exprOperator("/", exprOperator("-", year(), int(1)), int(100)), int(1)) to
                    "arithmetic on PartiQL `EXTRACT(YEAR ...)`"
            DatePart.MILLENNIUM ->
                exprOperator("+", exprOperator("/", exprOperator("-", year(), int(1)), int(1000)), int(1)) to
                    "arithmetic on PartiQL `EXTRACT(YEAR ...)`"
            else -> error("No field rule for $part")
        }
    }

    /**
     * `DATE_PART_YEAR(x)`: the year of a date as an INTEGER.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_PART_YEAR.html
     */
    private fun datePartYear(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 1)
        return functions.resolveOperands(listOf(args[0].ctx to args[0].expr), typing = "date_part_year") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("DATE_PART_YEAR", args[0], e[0], Kind.of(t[0]), PType.TIMESTAMP)
            if (!kind.isTimestampLike) throw UnsupportedConstructException.at(args[0].ctx, "DATE_PART_YEAR with ${kind.argument}")
            functions.note(name, "Redshift `DATE_PART_YEAR` was replaced by PartiQL `EXTRACT(YEAR ...)`")
            exprExtract(DatetimeField.YEAR(), if (kind == Kind.TIMESTAMPTZ) exprCast(x, DataType.TIMESTAMP()) else x)
        }
    }

    /**
     * `DATE_TRUNC('part', x)` truncates a timestamp to the start of the date part, and returns a TIMESTAMP. The start of
     * the unit is the reference plus the whole units from the reference to `x`:
     * `DATE_ADD(MONTH, DATE_DIFF(MONTH, R, x), R)`. Weeks start on Monday, quarters are 3 months, decades start with a
     * year divisible by 10, and centuries and millennia start with the year 1.
     *
     * The result type for a TIMESTAMPTZ value is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_TRUNC.html
     */
    private fun dateTrunc(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 2)
        // Read the datepart from the parse tree first: an unquoted datepart (`month`) is not a column.
        val partName =
            (RedshiftDatetime.atomOf(args[0].ctx) as? P.AtomLiteralContext)?.let { stringLiteral(args[0].expr) }
                ?: throw UnsupportedConstructException.at(args[0].ctx, "DATE_TRUNC with a datepart that is not a character string literal")
        val part =
            DatePart.of(partName)?.takeIf {
                it in TRUNC_PARTS
            } ?: throw UnsupportedConstructException.at(args[0].ctx, "DATE_TRUNC datepart '$partName'")
        return functions.resolveOperands(listOf(args[1].ctx to args[1].expr), typing = "date_trunc:$partName") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("DATE_TRUNC", args[1], e[0], Kind.of(t[0]), PType.TIMESTAMP)
            when (kind) {
                Kind.DATE, Kind.TIMESTAMP -> {}
                Kind.TIMESTAMPTZ -> throw UnsupportedConstructException.at(name, "DATE_TRUNC with a TIMESTAMPTZ argument")
                else -> throw UnsupportedConstructException.at(args[1].ctx, "DATE_TRUNC with ${kind.argument}")
            }
            functions.note(
                name,
                "Redshift `DATE_TRUNC` was replaced by PartiQL `DATE_ADD` and `DATE_DIFF` arithmetic from a fixed reference timestamp",
            )
            truncate(part, x)
        }
    }

    private fun truncate(
        part: DatePart,
        x: Expr,
    ): Expr {
        fun start(
            unit: DatePart,
            per: Int,
            reference: Expr = REFERENCE,
        ): Expr {
            val units = dateDiffCall(unit, reference, x)
            val whole = if (per == 1) units else exprOperator("*", exprOperator("/", units, int(per)), int(per))
            return dateAddCall(unit, whole, reference)
        }
        return when (part) {
            DatePart.SECOND, DatePart.MINUTE, DatePart.HOUR, DatePart.DAY, DatePart.MONTH, DatePart.YEAR -> start(part, 1)
            DatePart.WEEK -> start(DatePart.DAY, 7)
            DatePart.QUARTER -> start(DatePart.MONTH, 3)
            DatePart.DECADE -> start(DatePart.YEAR, 10, timestamp("0000-01-01 00:00:00"))
            DatePart.CENTURY -> start(DatePart.YEAR, 100)
            DatePart.MILLENNIUM -> start(DatePart.YEAR, 1000)
            DatePart.MILLISECOND, DatePart.MICROSECOND -> {
                val (unit, nanos) =
                    if (part == DatePart.MILLISECOND) secondsInterval("0.001", 3) to 1_000_000 else secondsInterval("0.000001", 6) to 1_000
                exprOperator("+", start(DatePart.SECOND, 1), exprOperator("*", unit, exprOperator("/", nanosOfSecond(x), int(nanos))))
            }
            else -> error("No truncation rule for $part")
        }
    }

    /**
     * `LAST_DAY(x)` is the DATE of the last day of the month of `x`: the day before the first day of the next month.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LAST_DAY.html
     */
    private fun lastDay(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 1)
        return functions.resolveOperands(listOf(args[0].ctx to args[0].expr), typing = "last_day") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("LAST_DAY", args[0], e[0], Kind.of(t[0]), PType.TIMESTAMP)
            if (!kind.isTimestampLike) throw UnsupportedConstructException.at(args[0].ctx, "LAST_DAY with ${kind.argument}")
            functions.note(name, "Redshift `LAST_DAY` was replaced by PartiQL date arithmetic")
            val value = if (kind == Kind.TIMESTAMPTZ) exprCast(x, DataType.TIMESTAMP()) else x
            val months = exprOperator("+", dateDiffCall(DatePart.MONTH, REFERENCE, value), int(1))
            dateAddCall(DatePart.DAY, int(-1), dateAddCall(DatePart.MONTH, months, REFERENCE_DATE))
        }
    }

    /**
     * `ADD_MONTHS(x, n)` adds `n` months and returns a TIMESTAMP. If `x` is the last day of its month, the result is the
     * last day of the result month; otherwise the day is the same, or the last day of a shorter month. PartiQL
     * `DATE_ADD(MONTH, ...)` keeps the day where it can, so the last day of a month is computed from the next day.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ADD_MONTHS.html
     */
    private fun addMonths(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 2)
        return functions.resolveOperands(args.map { it.ctx to it.expr }, typing = "add_months") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("ADD_MONTHS", args[0], e[0], Kind.of(t[0]), PType.TIMESTAMP)
            if (Kind.of(t[1]) != Kind.INTEGER) {
                throw UnsupportedConstructException.at(
                    args[1].ctx,
                    "ADD_MONTHS with a non-integer number of months",
                )
            }
            val n = e[1]
            val value =
                when (kind) {
                    Kind.DATE -> exprCast(x, DataType.TIMESTAMP())
                    Kind.TIMESTAMP -> x
                    Kind.TIMESTAMPTZ -> throw UnsupportedConstructException.at(name, "ADD_MONTHS with a TIMESTAMPTZ argument")
                    else -> throw UnsupportedConstructException.at(args[0].ctx, "ADD_MONTHS with ${kind.argument}")
                }
            functions.note(name, "Redshift `ADD_MONTHS` was replaced by PartiQL `DATE_ADD(MONTH, ...)` with an end-of-month rule")
            val next = dateAddCall(DatePart.DAY, int(1), value)
            val isLastDay = exprOperator("=", exprExtract(DatetimeField.DAY(), next), int(1))
            val lastDay = dateAddCall(DatePart.DAY, int(-1), dateAddCall(DatePart.MONTH, n, next))
            exprCase(null, listOf(exprCaseBranch(isLastDay, lastDay)), dateAddCall(DatePart.MONTH, n, value))
        }
    }

    /**
     * `NEXT_DAY(x, 'day')` is the DATE of the first day after `x` with the day of the week `day`. The reference is a
     * Monday, so `n % 7` is the day of the week of the day `n` days after it, with 0 for Monday.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NEXT_DAY.html
     */
    private fun nextDay(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        scalar(name, args, setq, 2)
        val dayName =
            stringLiteral(args[1].expr)
                ?: throw UnsupportedConstructException.at(args[1].ctx, "NEXT_DAY with a day name that is not a literal")
        val weekday = WEEKDAYS[dayName.lowercase()] ?: throw UnsupportedConstructException.at(args[1].ctx, "NEXT_DAY day name '$dayName'")
        return functions.resolveOperands(listOf(args[0].ctx to args[0].expr), typing = "next_day:$weekday") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("NEXT_DAY", args[0], e[0], Kind.of(t[0]), PType.TIMESTAMP)
            if (!kind.isTimestampLike) throw UnsupportedConstructException.at(args[0].ctx, "NEXT_DAY with ${kind.argument}")
            functions.note(name, "Redshift `NEXT_DAY` was replaced by PartiQL date arithmetic")
            val value = if (kind == Kind.TIMESTAMPTZ) exprCast(x, DataType.TIMESTAMP()) else x

            fun days(): Expr = dateDiffCall(DatePart.DAY, REFERENCE, value)
            // The days to the next `weekday`: ((weekday - today - 1) mod 7) + 1, which is 7 for the same day.
            val ahead = exprOperator("%", exprOperator("-", int(weekday + 6), exprOperator("%", days(), int(7))), int(7))
            dateAddCall(DatePart.DAY, exprOperator("+", exprOperator("+", days(), ahead), int(1)), REFERENCE_DATE)
        }
    }

    /**
     * The comparison functions `DATE_CMP`, `TIMESTAMP_CMP`, ...: -1, 0, or 1, and NULL for a NULL argument. The
     * arguments must have the types of the function, or a type that converts to them exactly (DATE to TIMESTAMP).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATE_CMP.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TIMESTAMP_CMP.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TIMESTAMPTZ_CMP.html
     */
    private fun compare(
        redshift: String,
        first: Kind,
        second: Kind,
    ): RedshiftFunctions.Rule =
        RedshiftFunctions.Rule { name, args, setq ->
            scalar(name, args, setq, 2)
            functions.resolveOperands(args.map { it.ctx to it.expr }, typing = redshift.lowercase()) { e, t ->
                if (waiting(t)) return@resolveOperands types.pending(e)
                val a = compareArgument(redshift, args[0], e[0], Kind.of(t[0]), first)
                val b = compareArgument(redshift, args[1], e[1], Kind.of(t[1]), second)
                functions.note(name, "Redshift `$redshift` was replaced by a CASE expression")
                exprCase(
                    null,
                    listOf(
                        exprCaseBranch(exprOperator("<", a, b), int(-1)),
                        exprCaseBranch(exprOperator(">", a, b), int(1)),
                        exprCaseBranch(exprOperator("=", a, b), int(0)),
                    ),
                    null,
                )
            }
        }

    private fun compareArgument(
        function: String,
        operand: Operand,
        expr: Expr,
        kind: Kind,
        declared: Kind,
    ): Expr {
        if (kind == Kind.STRING) {
            val value =
                stringLiteral(operand.expr)
                    ?: throw notLiteral(function, operand)
            val code =
                when (declared) {
                    Kind.DATE -> PType.DATE
                    Kind.TIMESTAMP -> PType.TIMESTAMP
                    else -> PType.TIMESTAMPZ
                }
            return stringArgument(operand, value, code, declared.display)
        }
        val exact =
            kind == declared ||
                (kind == Kind.DATE && (declared == Kind.TIMESTAMP || declared == Kind.TIMESTAMPTZ)) ||
                (kind == Kind.TIMESTAMP && declared == Kind.TIMESTAMPTZ)
        if (!exact) throw UnsupportedConstructException.at(operand.ctx, "$function with ${kind.argument}")
        return expr
    }

    /**
     * `CONVERT_TIMEZONE(['source',] 'target', x)` converts a timestamp from the source time zone (UTC by default) to the
     * target time zone, and returns a TIMESTAMP. PartiQL has no time zone database, so only the UTC time zone is
     * translated, and the conversion does not change the timestamp.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/CONVERT_TIMEZONE.html
     */
    private fun convertTimezone(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        if (args.size != 2 && args.size != 3) {
            throw UnsupportedConstructException.at(name, "`${name.text}` with ${args.size} argument(s); expected 2 or 3")
        }
        if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
        for (zone in args.dropLast(1)) {
            val value =
                stringLiteral(zone.expr)
                    ?: throw UnsupportedConstructException.at(zone.ctx, "CONVERT_TIMEZONE with a time zone that is not a literal")
            if (!isUtc(value)) throw UnsupportedConstructException.at(zone.ctx, "CONVERT_TIMEZONE with time zone '$value'")
        }
        val operand = args.last()
        return functions.resolveOperands(listOf(operand.ctx to operand.expr), typing = "convert_timezone:${args.size}") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            val (x, kind) = datetimeArgument("CONVERT_TIMEZONE", operand, e[0], Kind.of(t[0]), PType.TIMESTAMP)
            if (!kind.isTimestampLike) throw UnsupportedConstructException.at(operand.ctx, "CONVERT_TIMEZONE with ${kind.argument}")
            if (kind == Kind.TIMESTAMP) {
                functions.note(name, "Redshift `CONVERT_TIMEZONE` between UTC time zones was removed")
                x
            } else {
                functions.note(name, "Redshift `CONVERT_TIMEZONE` between UTC time zones was replaced by a cast to TIMESTAMP")
                exprCast(x, DataType.TIMESTAMP())
            }
        }
    }

    /**
     * `x AT TIME ZONE 'zone'` ([construct] `AT TIME ZONE`) and `TIMEZONE('zone', x)`, for the UTC time zone: a TIMESTAMP
     * value becomes the TIMESTAMPTZ value at that time in UTC, and a TIMESTAMPTZ value becomes its TIMESTAMP in UTC. A
     * character string argument of `TIMEZONE` is a TIMESTAMP value ([stringValues]).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_AT_TIME_ZONE.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TIMEZONE.html
     */
    internal fun atTimeZone(
        location: Token,
        construct: String,
        zone: String,
        operand: Operand,
        stringValues: Boolean,
    ): Expr {
        val source = if (construct == "TIMEZONE") "TIMEZONE('$zone', ...)" else "AT TIME ZONE '$zone'"
        if (!isUtc(zone)) throw UnsupportedConstructException.at(location, source)
        return functions.resolveOperands(listOf(operand.ctx to operand.expr), typing = "at_time_zone:$construct:$stringValues") { e, t ->
            if (waiting(t)) return@resolveOperands types.pending(e)
            var x = e[0]
            var kind = Kind.of(t[0])
            if (kind == Kind.STRING && stringValues) {
                val converted = datetimeArgument(construct, operand, x, kind, PType.TIMESTAMP)
                x = converted.first
                kind = converted.second
            }
            when (kind) {
                Kind.TIMESTAMP -> {
                    functions.note(location, "Redshift `$source` of a TIMESTAMP value was replaced by a cast to TIMESTAMP WITH TIME ZONE")
                    exprCast(x, DataType.TIMESTAMP_WITH_TIME_ZONE(RedshiftDatetime.DATETIME_PRECISION))
                }
                Kind.TIMESTAMPTZ -> {
                    functions.note(location, "Redshift `$source` of a TIMESTAMPTZ value was replaced by a cast to TIMESTAMP")
                    exprCast(x, DataType.TIMESTAMP())
                }
                else -> throw UnsupportedConstructException.at(location, "$construct with ${kind.argument}")
            }
        }
    }

    private fun isUtc(zone: String): Boolean = zone.uppercase() in UTC_NAMES

    /*
     * Arguments
     */

    /**
     * True if a rule must wait for another probe pass: in a probe pass, the type of an operand can be the dynamic type
     * of a placeholder from the previous pass.
     */
    internal fun waiting(operandTypes: List<PType>): Boolean = types.probing && operandTypes.any { it.code() == PType.DYNAMIC }

    /**
     * A datetime argument of [function] and its kind. Redshift converts a character string literal to the datetime
     * type of the argument; the translation converts it to a literal of [target] (TIMESTAMP or TIME). If another
     * argument is a DATE value ([dateOperand]), Redshift can read the string as a DATE, so a string with a time other
     * than 00:00:00 is unsupported. A character string that is not a literal is unsupported: PartiQL cannot convert it
     * when the query runs.
     */
    internal fun datetimeArgument(
        function: String,
        operand: Operand,
        expr: Expr,
        kind: Kind,
        target: Int,
        dateOperand: Boolean = false,
    ): Pair<Expr, Kind> {
        if (kind != Kind.STRING) return expr to kind
        val value =
            stringLiteral(operand.expr)
                ?: throw notLiteral(function, operand)
        if (dateOperand && target == PType.TIMESTAMP) {
            val time = DatetimeText.readTimestamp(value)?.time
            if (time != null && !time.isMidnight) {
                throw UnsupportedConstructException.at(operand.ctx, "character string '$value' with a time of day next to a DATE argument")
            }
        }
        val typeName = if (target == PType.TIME) "TIME" else "TIMESTAMP"
        return stringArgument(operand, value, target, typeName) to (if (target == PType.TIME) Kind.TIME else Kind.TIMESTAMP)
    }

    /** The literal of the type [code] for the character string literal [value] of [operand]. */
    private fun stringArgument(
        operand: Operand,
        value: String,
        code: Int,
        typeName: String,
    ): Expr {
        val token = operand.ctx.start
        if (DatetimeText.isSpecial(value)) throw UnsupportedConstructException.at(token, "special datetime value '${value.trim()}'")
        // Redshift may read a string with a time zone as a TIMESTAMPTZ value and convert it in the session time zone.
        if ((code == PType.DATE || code == PType.TIMESTAMP) && DatetimeText.readTimestamp(value)?.zone != null) {
            throw UnsupportedConstructException.at(token, "character string '$value' with a time zone as a $typeName")
        }
        val literal =
            DatetimeText.literal(value, code, dropTime = false)
                ?: throw UnsupportedConstructException.at(token, "character string '$value' as a $typeName")
        functions.note(token, "The character string literal was converted to a $typeName literal, as Redshift does implicitly")
        return literal
    }

    private fun notLiteral(
        function: String,
        operand: Operand,
    ) = UnsupportedConstructException.at(operand.ctx, "$function with a character string argument that is not a literal")

    /** The value of [expr] if it is a character string literal. */
    internal fun stringLiteral(expr: Expr): String? = (expr as? ExprLit)?.lit?.takeIf { it.code() == Literal.STRING }?.stringValue()

    private fun scalar(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        arity: Int,
    ) {
        functions.arity(name, args, arity)
        if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000

        /** The UNIX epoch, 1970-01-01 00:00:00. */
        val EPOCH: Expr get() = timestamp("1970-01-01 00:00:00")

        val UTC_NAMES = setOf("UTC", "GMT")

        val TIME_FIELDS = setOf(DatePart.HOUR, DatePart.MINUTE, DatePart.SECOND, DatePart.MILLISECOND)
        val YEAR_MONTH_FIELDS = setOf(DatePart.YEAR, DatePart.MONTH)
        val DAY_TIME_FIELDS = setOf(DatePart.DAY, DatePart.HOUR, DatePart.MINUTE, DatePart.SECOND)

        val TRUNC_PARTS =
            setOf(
                DatePart.MICROSECOND, DatePart.MILLISECOND, DatePart.SECOND, DatePart.MINUTE, DatePart.HOUR, DatePart.DAY,
                DatePart.WEEK, DatePart.MONTH, DatePart.QUARTER, DatePart.YEAR, DatePart.DECADE, DatePart.CENTURY,
                DatePart.MILLENNIUM,
            )

        /** The day names of NEXT_DAY, with the day of the week, 0 for Monday. */
        val WEEKDAYS: Map<String, Int> =
            buildMap {
                listOf("m", "mo", "mon", "monday").forEach { put(it, 0) }
                listOf("tu", "tue", "tues", "tuesday").forEach { put(it, 1) }
                listOf("w", "we", "wed", "wednesday").forEach { put(it, 2) }
                listOf("th", "thu", "thurs", "thursday").forEach { put(it, 3) }
                listOf("f", "fr", "fri", "friday").forEach { put(it, 4) }
                listOf("sa", "sat", "saturday").forEach { put(it, 5) }
                listOf("su", "sun", "sunday").forEach { put(it, 6) }
            }
    }
}
