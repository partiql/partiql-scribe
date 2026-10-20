package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprExtract
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprSessionAttribute
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.SessionAttribute
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates Redshift date and time constructs: datetime literals, `DATEADD`, `DATEDIFF`, `EXTRACT`, and the current
 * date and time. The datetime functions with the function call syntax are in [RedshiftDatetimeFunctions].
 */
internal class RedshiftDatetime(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val types: OperandTypes,
) {
    /**
     * Translates `DATEADD(part, n, x)` and `DATEDIFF(part, a, b)`.
     *
     * The PartiQL parser translates `DATE_ADD(part, ...)` and `DATE_DIFF(part, ...)` to calls of the hidden built-in
     * functions `date_add_<part>` and `date_diff_<part>`; this method makes the same calls. The argument order is the
     * same in both languages.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    fun dateFunction(ctx: P.AtomDateFunctionContext): Expr {
        val partText = ctx.part.text.lowercase()
        val part = DATE_PARTS[partText] ?: throw UnsupportedConstructException.at(ctx.part, "datepart `${ctx.part.text}`")
        val isAdd = ctx.name.type == P.DATEADD
        val function = if (isAdd) "date_add" else "date_diff"
        builder.note(ctx.name, "Redshift `${ctx.name.text.uppercase()}` was replaced by PartiQL `${function.uppercase()}`")
        val args = ctx.expr().map(exprs::expr)
        return exprCall(Identifier.regular("$SYSTEM_PREFIX_INTERNAL${function}_$part"), args, null)
    }

    /**
     * Translates `EXTRACT(part FROM x)`.
     *
     * For SECOND the result types are different: Redshift returns INTEGER (the fraction is truncated) for TIMESTAMP
     * and TIME, and DOUBLE PRECISION for TIMESTAMPTZ; PartiQL returns a DECIMAL that keeps the fraction. PartiQL has
     * no truncation function to make the result equal, so the translator adds a note.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_EXTRACT_function.html
     */
    fun extract(ctx: P.AtomExtractContext): Expr {
        val partToken = ctx.part.start
        val partText =
            if (ctx.part.STRING_LITERAL() != null) exprs.stringValue(partToken).lowercase() else ctx.part.text.lowercase()
        // TIMEZONE_HOUR and TIMEZONE_MINUTE are not supported: the Scribe PartiQL target cannot print them.
        val part = DATE_PARTS[partText] ?: throw UnsupportedConstructException.at(partToken, "EXTRACT datepart `${ctx.part.text}`")
        if (part == "second") {
            builder.note(
                partToken,
                "Redshift `EXTRACT(SECOND ...)` returns INTEGER for TIMESTAMP and TIME values, without the fraction; " +
                    "PartiQL returns a DECIMAL with the fraction",
            )
        }
        return exprExtract(DatetimeField.parse(part.uppercase()), exprs.expr(ctx.expr()))
    }

    /**
     * Translates a `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, or `TIMESTAMPTZ` literal.
     */
    fun literal(ctx: P.LiteralContext): Expr =
        when (ctx) {
            is P.LiteralDateContext -> {
                val value = exprs.stringValue(ctx.STRING_LITERAL().symbol)
                if (!DATE_PATTERN.matches(value)) throw UnsupportedConstructException.at(ctx, "DATE literal format '$value'")
                exprLit(Literal.typedString(DataType.DATE(), value))
            }
            is P.LiteralTimeContext -> typedLiteral(ctx, ctx.STRING_LITERAL().symbol, "TIME", TIME_PATTERN, DataType.TIME())
            is P.LiteralTimeTzContext ->
                typedLiteral(ctx, ctx.STRING_LITERAL().symbol, "TIMETZ", TIME_TZ_PATTERN, DataType.TIME_WITH_TIME_ZONE())
            is P.LiteralTimestampContext ->
                typedLiteral(ctx, ctx.STRING_LITERAL().symbol, "TIMESTAMP", TIMESTAMP_PATTERN, DataType.TIMESTAMP())
            is P.LiteralTimestampTzContext ->
                typedLiteral(
                    ctx,
                    ctx.STRING_LITERAL().symbol,
                    "TIMESTAMPTZ",
                    TIMESTAMP_TZ_PATTERN,
                    DataType.TIMESTAMP_WITH_TIME_ZONE(),
                )
            else -> builder.unexpected(ctx)
        }

    /**
     * Translates a typed datetime literal. A value that does not match [pattern] is unsupported: Redshift accepts more
     * formats (for example time zone names), and a TIMESTAMP or TIME value with an offset, which Redshift ignores but
     * PartiQL does not.
     */
    private fun typedLiteral(
        ctx: P.LiteralContext,
        string: Token,
        name: String,
        pattern: Regex,
        type: DataType,
    ): Expr {
        val value = exprs.stringValue(string)
        if (!pattern.matches(value)) throw UnsupportedConstructException.at(ctx, "$name literal format '$value'")
        return exprLit(Literal.typedString(type, value))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CURRENT_DATE_function.html
     */
    fun currentDate(name: Token): Expr = exprSessionAttribute(SessionAttribute.CURRENT_DATE())

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    fun sysdate(name: Token): Expr = builder.functions.datetime.utcnow(name, "SYSDATE")

    internal companion object {
        /** The prefix of hidden PartiQL built-in function names; see `FunctionUtils` in partiql-spi. */
        const val SYSTEM_PREFIX_INTERNAL = "\uFDEF"

        val DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")
        val TIME_PATTERN = Regex("""\d{2}:\d{2}:\d{2}(\.\d{1,6})?""")
        val TIME_TZ_PATTERN = Regex("""\d{2}:\d{2}:\d{2}(\.\d{1,6})?[+-]\d{2}:\d{2}""")
        val TIMESTAMP_PATTERN = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d{1,6})?""")
        val TIMESTAMP_TZ_PATTERN = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d{1,6})?[+-]\d{2}:\d{2}""")

        /**
         * Redshift datepart names and abbreviations that have a PartiQL equivalent.
         * https://docs.aws.amazon.com/redshift/latest/dg/r_Dateparts_for_datetime_functions.html
         */
        val DATE_PARTS: Map<String, String> =
            buildMap {
                listOf("year", "years", "y", "yr", "yrs").forEach { put(it, "year") }
                listOf("month", "months", "mon", "mons").forEach { put(it, "month") }
                listOf("day", "days", "d").forEach { put(it, "day") }
                listOf("hour", "hours", "h", "hr", "hrs").forEach { put(it, "hour") }
                listOf("minute", "minutes", "m", "min", "mins").forEach { put(it, "minute") }
                listOf("second", "seconds", "s", "sec", "secs").forEach { put(it, "second") }
            }
    }
}
