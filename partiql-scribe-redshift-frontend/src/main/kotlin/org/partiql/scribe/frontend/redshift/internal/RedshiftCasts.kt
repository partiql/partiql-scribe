package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.DataType
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.TrimSpec
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.math.BigInteger
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates Redshift data types, CAST, `::`, and CONVERT; see [RedshiftExpressions].
 *
 * A PartiQL CAST is not always the Redshift conversion. The rules here use the static type of the operand (see
 * [OperandTypes]):
 *  - Redshift truncates a DECIMAL value from a table when it reduces its scale (`enable_numeric_rounding` is off by
 *    default), and rounds a constant; PartiQL rounds. The translation truncates column values, keeps a constant that
 *    needs no rounding, and reports other constants as unsupported.
 *  - PartiQL 1.6.1 cannot convert a character string to a datetime value at run time, and converts some strings to
 *    numbers differently. A character string literal is converted at translation time.
 *  - PartiQL writes some numbers and datetime values as text in other formats; those conversions to a character string
 *    are unsupported.
 *  - Redshift casts SUPER values laxly (NULL on failure), so a CAST of a SUPER value is unsupported.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_CAST_function.html
 */
internal class RedshiftCasts(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val datetime: RedshiftDatetime,
    private val types: OperandTypes,
) {
    /** A Redshift data type. [name] is the Redshift name, for messages. */
    internal sealed class Target(val name: String) {
        /** A type with a direct PartiQL equivalent: the integer types, REAL, DOUBLE PRECISION, BOOLEAN, and the datetime types. */
        class Simple(name: String, val type: DataType, val pType: PType) : Target(name)

        class Decimal(val precision: Int, val scale: Int) : Target("DECIMAL($precision,$scale)")

        /** VARCHAR(n); [length] is null for VARCHAR without a length, and [max] is true for VARCHAR(MAX). */
        class Varchar(
            val length: Int?,
            val max: Boolean = false,
        ) : Target(
                if (max) {
                    "VARCHAR(MAX)"
                } else if (length == null) {
                    "VARCHAR"
                } else {
                    "VARCHAR($length)"
                },
            )

        class Char(val length: Int) : Target("CHAR($length)")

        /** A type that the translator passes to PartiQL as it is, such as an INTERVAL type. */
        class Other(name: String, val type: DataType) : Target(name)

        /** The PartiQL data type. */
        val dataType: DataType
            get() =
                when (this) {
                    is Simple -> type
                    is Decimal -> DataType.DECIMAL(precision, scale)
                    is Varchar -> if (length == null || max) DataType.STRING() else DataType.VARCHAR(length)
                    is Char -> DataType.CHAR(length)
                    is Other -> type
                }
    }

    /*
     * Data types
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */

    /** The PartiQL data type of a Redshift data type. */
    fun dataType(ctx: P.DataTypeContext): DataType = target(ctx).dataType

    fun target(ctx: P.DataTypeContext): Target =
        when (ctx) {
            is P.TypeDoublePrecisionContext -> DOUBLE
            is P.TypeCharacterVaryingContext -> varchar(ctx, "CHARACTER VARYING", ctx.length)
            is P.TypeNationalCharacterContext -> char(ctx, "NATIONAL CHARACTER", ctx.length, ctx.length == null)
            is P.TypeBinaryContext ->
                unsupported(
                    ctx.start,
                    "data type `${ctx.children.filter { it !is P.TypeParametersContext }.joinToString(" ") { it.text.lowercase() }}`",
                )
            is P.TypeTimeContext -> TIME
            is P.TypeTimeTzContext -> TIMETZ
            is P.TypeTimestampContext -> TIMESTAMP
            is P.TypeTimestampTzContext -> TIMESTAMPTZ
            is P.TypeDateContext -> DATE
            is P.TypeIntervalContext -> Target.Other("INTERVAL", datetime.intervalType(ctx))
            is P.TypeNamedContext -> named(ctx)
            else -> builder.unexpected(ctx)
        }

    private fun named(ctx: P.TypeNamedContext): Target {
        val name = ctx.name.text.lowercase()
        val params = ctx.typeParameters()

        fun noArgs(type: Target): Target {
            if (params != null) unsupported(ctx.start, "type parameters for `${ctx.name.text}`")
            return type
        }
        return when (name) {
            "smallint", "int2" -> noArgs(SMALLINT)
            "integer", "int", "int4" -> noArgs(INTEGER)
            "bigint", "int8" -> noArgs(BIGINT)
            "real", "float4" -> noArgs(REAL)
            "float8" -> noArgs(DOUBLE)
            "float" -> float(ctx, params)
            "boolean", "bool" -> noArgs(BOOLEAN)
            "decimal", "numeric" -> decimal(ctx, params)
            "char", "character", "nchar" -> char(ctx, ctx.name.text.uppercase(), params, false)
            "bpchar" -> char(ctx, "BPCHAR", params, params == null)
            "varchar", "nvarchar" -> varchar(ctx, ctx.name.text.uppercase(), params)
            "text" -> noArgs(Target.Varchar(null))
            else -> unsupported(ctx.start, "data type `${ctx.name.text}`")
        }
    }

    /** FLOAT is DOUBLE PRECISION; FLOAT(p) is REAL for p from 1 to 24 and DOUBLE PRECISION for p from 25 to 53. */
    private fun float(
        ctx: P.TypeNamedContext,
        params: P.TypeParametersContext?,
    ): Target {
        if (params == null) return DOUBLE
        if (params.arg1 != null) invalid(params, "FLOAT takes one parameter")
        val p = integerParameter(params.arg0, "FLOAT precision", 1, MAX_FLOAT_PRECISION)
        return if (p <= REAL_PRECISION) REAL else DOUBLE
    }

    /** DECIMAL is DECIMAL(18, 0), DECIMAL(p) is DECIMAL(p, 0). The precision is 1 to 38, and the scale 0 to 37. */
    private fun decimal(
        ctx: P.TypeNamedContext,
        params: P.TypeParametersContext?,
    ): Target {
        if (params == null) return Target.Decimal(DEFAULT_DECIMAL_PRECISION, 0)
        val p = integerParameter(params.arg0, "DECIMAL precision", 1, MAX_DECIMAL_PRECISION)
        val s = params.arg1?.let { integerParameter(it, "DECIMAL scale", 0, MAX_DECIMAL_SCALE) } ?: 0
        if (s > p) invalid(params, "DECIMAL scale $s is greater than the precision $p")
        return Target.Decimal(p, s)
    }

    /**
     * CHAR without a length is CHAR(1); the length is 1 to 4096, or MAX (4096). Redshift documents BPCHAR as CHAR(256)
     * for columns; the meaning of BPCHAR without a length in a CAST is not documented.
     */
    private fun char(
        ctx: P.DataTypeContext,
        name: String,
        params: P.TypeParametersContext?,
        unbounded: Boolean,
    ): Target {
        if (unbounded && name == "BPCHAR") unsupported(ctx.start, "data type `bpchar` without a length")
        if (params == null) return Target.Char(1)
        if (params.arg1 != null) invalid(params, "$name takes one parameter")
        if (params.arg0.MAX() != null) return Target.Char(MAX_CHAR_LENGTH)
        return Target.Char(integerParameter(params.arg0, "$name length", 1, MAX_CHAR_LENGTH))
    }

    /** VARCHAR(n): the length is 1 to 65535, or MAX. */
    private fun varchar(
        ctx: P.DataTypeContext,
        name: String,
        params: P.TypeParametersContext?,
    ): Target {
        if (params == null) return Target.Varchar(null)
        if (params.arg1 != null) invalid(params, "$name takes one parameter")
        if (params.arg0.MAX() != null) return Target.Varchar(MAX_VARCHAR_LENGTH, max = true)
        return Target.Varchar(integerParameter(params.arg0, "$name length", 1, MAX_VARCHAR_LENGTH))
    }

    private fun integerParameter(
        ctx: P.TypeParameterContext,
        what: String,
        min: Int,
        max: Int,
    ): Int {
        val value = ctx.INTEGER_LITERAL()?.text?.toBigInteger()
        if (value == null || value < BigInteger.valueOf(min.toLong()) || value > BigInteger.valueOf(max.toLong())) {
            invalid(ctx, "$what ${ctx.text} is not between $min and $max")
        }
        return value.toInt()
    }

    private fun invalid(
        ctx: ParserRuleContext,
        message: String,
    ): Nothing = throw RedshiftSyntaxException(message, ctx.start.line, ctx.start.charPositionInLine + 1)

    /*
     * Casts
     */

    /**
     * `CAST(x AS type)` and `x::type`; [ctx] is the cast, and [token] its location.
     */
    fun cast(
        token: Token,
        ctx: ParserRuleContext,
        value: Operand,
        type: P.DataTypeContext,
    ): Expr {
        val target = target(type)
        return types.resolveTyped(listOf(value)) { (x) -> cast(token, ctx, x, target) }
    }

    /**
     * `CONVERT(type, x)` is `CAST(x AS type)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CONVERT_function.html
     */
    fun convert(ctx: P.AtomConvertContext): Expr {
        builder.note(ctx.CONVERT().symbol, "Redshift `CONVERT` was replaced by PartiQL `CAST`")
        return cast(ctx.CONVERT().symbol, ctx, Operand(ctx.expr(), exprs.expr(ctx.expr())), ctx.dataType())
    }

    /**
     * TRY_CAST returns NULL when the conversion fails; a PartiQL CAST returns MISSING or fails.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRY_CAST.html
     */
    fun tryCast(ctx: P.AtomTryCastContext): Expr = unsupported(ctx.TRY_CAST().symbol, "TRY_CAST")

    private fun cast(
        token: Token,
        ctx: ParserRuleContext,
        x: TypedOperand,
        target: Target,
    ): Expr {
        if (x.family == TypeFamily.UNKNOWN) return exprCast(x.expr, target.dataType)
        if (x.family == TypeFamily.SUPER) unsupported(token, "CAST of a SUPER value: Redshift casts SUPER values laxly (NULL on failure)")
        if (x.untyped) unsupported(token, "CAST of an aggregate expression whose type is not known")
        return when (target) {
            is Target.Simple ->
                when (TypeFamily.of(target.pType)) {
                    TypeFamily.INTEGER -> toInteger(token, x, target)
                    TypeFamily.FLOAT -> toFloat(token, x, target)
                    TypeFamily.BOOLEAN -> toBoolean(token, x, target)
                    else -> toDatetime(token, x, target)
                }
            is Target.Decimal -> toDecimal(token, x, target)
            is Target.Varchar -> toText(token, ctx, x, target)
            is Target.Char -> toText(token, ctx, x, target)
            is Target.Other -> exprCast(x.expr, target.dataType)
        }
    }

    private fun disallowed(
        token: Token,
        x: TypedOperand,
        target: Target,
    ): Nothing = unsupported(token, "CAST of ${typeName(x.type)} to ${target.name}")

    /**
     * To SMALLINT, INTEGER, or BIGINT. A DECIMAL value from a table is truncated toward zero:
     * `CAST(x - x % 1 AS INT)`. A constant that needs rounding is unsupported, because Redshift rounds constants.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_enable_numeric_rounding.html
     */
    private fun toInteger(
        token: Token,
        x: TypedOperand,
        target: Target.Simple,
    ): Expr =
        when (x.family) {
            TypeFamily.INTEGER, TypeFamily.BOOLEAN -> exprCast(x.expr, target.type)
            TypeFamily.DECIMAL -> {
                val scale = scaleOf(x.type)
                when {
                    scale == 0 -> exprCast(x.expr, target.type)
                    referencesColumn(x.ctx) -> exprCast(truncate(x.expr, 0), target.type)
                    x.literal != null && x.literal.bigDecimalValue().stripTrailingZeros().scale() <= 0 -> exprCast(x.expr, target.type)
                    else -> roundedConstant(token)
                }
            }
            TypeFamily.FLOAT ->
                unsupported(
                    token,
                    "CAST of ${typeName(x.type)} to an integer type: Redshift rounding is not documented; PartiQL truncates",
                )
            TypeFamily.TEXT -> {
                if (x.isStringLiteral) {
                    val text = x.literal!!.stringValue().trim(*RedshiftConditions.WHITESPACE)
                    val value = if (RedshiftConditions.INTEGER_TEXT.matches(text)) text.removePrefix("+").toBigInteger() else null
                    if (value == null || value.bitLength() >= integerBits(target.pType)) {
                        unsupported(token, "CAST of the character string '${x.literal.stringValue()}' to ${target.name}")
                    }
                    literalCast(token, x, exprLit(Literal.intNum(value)), target.type)
                } else {
                    // Redshift ignores white space around the number; PartiQL fails on some of it, as in '0 ' and
                    // ' 012'. PartiQL casts only STRING values to numbers.
                    exprCast(
                        exprTrim(stringValue(x), exprLit(Literal.string(String(RedshiftConditions.WHITESPACE))), TrimSpec.BOTH()),
                        target.type,
                    )
                }
            }
            else -> disallowed(token, x, target)
        }

    private fun integerBits(type: PType): Int =
        when (type.code()) {
            PType.SMALLINT -> Short.SIZE_BITS
            PType.INTEGER -> Int.SIZE_BITS
            else -> Long.SIZE_BITS
        }

    /** A non-literal character string operand as a STRING value, without the padding of a CHAR(n) value. */
    private fun stringValue(x: TypedOperand): Expr =
        when (x.type.code()) {
            PType.STRING -> x.expr
            PType.CHAR -> exprCast(exprTrim(x.expr, null, TrimSpec.TRAILING()), DataType.STRING())
            else -> exprCast(x.expr, DataType.STRING())
        }

    /**
     * To DECIMAL(p,s). A reduction of the scale truncates a value from a table, and is unsupported for a constant that
     * needs rounding.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_enable_numeric_rounding.html
     */
    private fun toDecimal(
        token: Token,
        x: TypedOperand,
        target: Target.Decimal,
    ): Expr =
        when (x.family) {
            TypeFamily.INTEGER -> exprCast(x.expr, target.dataType)
            TypeFamily.DECIMAL -> {
                val scale = scaleOf(x.type)
                when {
                    target.scale >= scale -> exprCast(x.expr, target.dataType)
                    referencesColumn(x.ctx) -> exprCast(truncate(x.expr, target.scale), target.dataType)
                    x.literal != null && x.literal.bigDecimalValue().stripTrailingZeros().scale() <= target.scale ->
                        exprCast(x.expr, target.dataType)
                    else -> roundedConstant(token)
                }
            }
            TypeFamily.FLOAT -> unsupported(token, "CAST of REAL or DOUBLE PRECISION to DECIMAL: Redshift rounding is not documented")
            TypeFamily.TEXT -> {
                if (!x.isStringLiteral) unsupported(token, "CAST of a character string to DECIMAL (PartiQL rejects strings such as '0.5')")
                val number = parseNumber(x.literal!!.stringValue())
                val fits =
                    number != null &&
                        number.stripTrailingZeros().scale() <= target.scale &&
                        number.precision() - number.scale() <= target.precision - target.scale
                if (!fits) unsupported(token, "CAST of the character string '${x.literal.stringValue()}' to ${target.name}")
                literalCast(token, x, exprLit(Literal.exactNum(number.toPlainString())), target.dataType)
            }
            else -> disallowed(token, x, target)
        }

    /** `x - x % 10^-s`, the value of [x] truncated to scale [scale]; the divisor is DECIMAL(38,s), so the remainder keeps the scale of [x]. */
    private fun truncate(
        x: Expr,
        scale: Int,
    ): Expr {
        val unit: Expr =
            if (scale == 0) {
                exprLit(Literal.intNum(1))
            } else {
                exprCast(
                    exprLit(Literal.exactNum(BigDecimal.ONE.movePointLeft(scale).toPlainString())),
                    DataType.DECIMAL(MAX_DECIMAL_PRECISION, scale),
                )
            }
        return exprOperator("-", x, exprOperator("%", x, unit))
    }

    private fun roundedConstant(token: Token): Nothing =
        unsupported(token, "CAST of a DECIMAL constant that needs rounding: Redshift rounds constants and truncates column values")

    /**
     * True if the expression of [ctx] refers to a column. Redshift truncates the values of a column when a cast
     * reduces their scale, and rounds constants.
     */
    private fun referencesColumn(ctx: ParserRuleContext): Boolean =
        ctx is P.AtomVariableContext ||
            ctx is P.AtomSubqueryContext ||
            (0 until ctx.childCount).any { (ctx.getChild(it) as? ParserRuleContext)?.let(::referencesColumn) == true }

    /** To REAL or DOUBLE PRECISION. */
    private fun toFloat(
        token: Token,
        x: TypedOperand,
        target: Target.Simple,
    ): Expr =
        when (x.family) {
            TypeFamily.INTEGER, TypeFamily.DECIMAL -> exprCast(x.expr, target.type)
            TypeFamily.FLOAT ->
                if (x.type.code() == PType.DOUBLE && target.pType.code() == PType.REAL) {
                    // PartiQL 1.6.1 returns MISSING for a DOUBLE PRECISION value that is zero or negative.
                    unsupported(token, "CAST of DOUBLE PRECISION to REAL")
                } else {
                    exprCast(x.expr, target.type)
                }
            TypeFamily.TEXT -> {
                if (!x.isStringLiteral) {
                    unsupported(
                        token,
                        "CAST of a character string to ${target.name} (PartiQL rejects strings such as '0.5')",
                    )
                }
                val text = x.literal!!.stringValue()
                val number = parseNumber(text) ?: unsupported(token, "CAST of the character string '$text' to ${target.name}")
                // A string converts to REAL through DOUBLE PRECISION in PostgreSQL, which can round twice; translate
                // only the values for which both conversions agree.
                if (target.pType.code() == PType.REAL && number.toFloat() != number.toDouble().toFloat()) {
                    unsupported(token, "CAST of the character string '$text' to REAL")
                }
                literalCast(token, x, exprLit(Literal.exactNum(number.toPlainString())), target.type)
            }
            else -> disallowed(token, x, target)
        }

    /**
     * To BOOLEAN. A character string literal is one of the documented spellings of TRUE and FALSE.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Boolean_type.html
     */
    private fun toBoolean(
        token: Token,
        x: TypedOperand,
        target: Target.Simple,
    ): Expr =
        when (x.family) {
            TypeFamily.BOOLEAN, TypeFamily.INTEGER -> exprCast(x.expr, target.type)
            TypeFamily.TEXT -> {
                if (!x.isStringLiteral) unsupported(token, "CAST of a character string to BOOLEAN")
                val text = x.literal!!.stringValue()
                val value =
                    when (text.trim(*RedshiftConditions.WHITESPACE).lowercase()) {
                        in RedshiftConditions.TRUE_TEXT -> true
                        in RedshiftConditions.FALSE_TEXT -> false
                        else -> unsupported(token, "CAST of the character string '$text' to BOOLEAN")
                    }
                builder.note(token, "Redshift CAST of the character string '$text' to BOOLEAN was replaced by the literal `$value`")
                exprLit(Literal.bool(value))
            }
            else -> disallowed(token, x, target)
        }

    /**
     * To VARCHAR, VARCHAR(n), or CHAR(n). Redshift VARCHAR(n) holds n bytes, and PartiQL VARCHAR(n) n characters, so a
     * cast that can shorten a character string is unsupported. VARCHAR without a length is PartiQL STRING when the value
     * has at most 256 bytes, the Redshift default length; whether Redshift truncates longer values in a CAST is not
     * documented. PartiQL 1.6.1 casts only STRING values to VARCHAR(n) and CHAR(n) at run time, so other character
     * values are cast to STRING first.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Character_types.html
     */
    private fun toText(
        token: Token,
        ctx: ParserRuleContext,
        x: TypedOperand,
        target: Target,
    ): Expr {
        if (target is Target.Char && !isTopLevelSelectItem(ctx)) unsupported(token, "CAST to CHAR(n) outside the SELECT list")
        val type = target.dataType
        val unbounded = target is Target.Varchar && (target.length == null || target.max)
        return when (x.family) {
            TypeFamily.INTEGER -> exprCast(x.expr, type)
            TypeFamily.DECIMAL -> {
                if (scaleOf(x.type) > MAX_PLAIN_SCALE || !RedshiftOperators.hasRedshiftScale(x.ctx)) {
                    unsupported(token, "CAST of a DECIMAL value to a character string: PartiQL writes a different scale or an exponent")
                }
                exprCast(x.expr, type)
            }
            TypeFamily.DATETIME -> {
                if (x.type.code() != PType.DATE) {
                    unsupported(token, "CAST of ${typeName(x.type)} to a character string: the text formats differ")
                }
                val text = exprCast(x.expr, DataType.STRING())
                if (unbounded) text else exprCast(text, type)
            }
            TypeFamily.FLOAT -> unsupported(token, "CAST of ${typeName(x.type)} to a character string: the text formats differ")
            TypeFamily.TEXT -> if (x.isStringLiteral) literalToText(token, x, target) else textToText(token, x, target)
            else -> disallowed(token, x, target)
        }
    }

    /** The cast of a character string literal to VARCHAR or CHAR, whose length in bytes is known. */
    private fun literalToText(
        token: Token,
        x: TypedOperand,
        target: Target,
    ): Expr {
        val value = x.literal!!.stringValue()
        val bytes = value.toByteArray(Charsets.UTF_8).size
        val ascii = value.all { it.code <= ASCII_MAX }
        when {
            target is Target.Char -> if (!ascii) unsupported(token, "CAST of a multibyte character string to ${target.name}")
            target !is Target.Varchar || target.max -> {}
            target.length == null -> if (bytes > DEFAULT_VARCHAR_LENGTH) unsupported(token, VARCHAR_NO_LENGTH)
            // A value that fits, or ASCII text, whose characters are bytes.
            bytes > target.length && !ascii -> unsupported(token, truncation(target))
        }
        return exprCast(x.expr, target.dataType)
    }

    /**
     * The cast of a character string value. A CHAR(n) value loses its padding, as when Redshift converts it to another
     * string type; its characters are single bytes.
     */
    private fun textToText(
        token: Token,
        x: TypedOperand,
        target: Target,
    ): Expr {
        val text = stringValue(x)
        // The largest length of the value in Redshift bytes, or null if it is not bounded.
        val length =
            when (x.type.code()) {
                PType.CHAR, PType.VARCHAR -> x.type.length
                else -> null
            }
        return when (target) {
            is Target.Char -> exprCast(text, target.dataType)
            is Target.Varchar ->
                when {
                    target.max -> text
                    target.length == null ->
                        if (length != null && length <= DEFAULT_VARCHAR_LENGTH) {
                            text
                        } else {
                            unsupported(
                                token,
                                VARCHAR_NO_LENGTH,
                            )
                        }
                    x.type.code() == PType.CHAR -> exprCast(text, target.dataType)
                    length != null && length <= target.length -> exprCast(text, target.dataType)
                    else -> unsupported(token, truncation(target))
                }
            else -> disallowed(token, x, target)
        }
    }

    private fun truncation(target: Target): String =
        "CAST to ${target.name} that can truncate a character string: Redshift counts bytes, PartiQL counts characters"

    /**
     * To DATE, TIME, TIMETZ, TIMESTAMP, or TIMESTAMPTZ. A character string literal is converted at translation time;
     * PartiQL 1.6.1 cannot convert other character strings. TIMESTAMPTZ values are UTC instants, as Redshift stores them.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */
    private fun toDatetime(
        token: Token,
        x: TypedOperand,
        target: Target.Simple,
    ): Expr {
        val to = target.pType.code()
        return when (x.family) {
            TypeFamily.TEXT -> {
                if (!x.isStringLiteral) {
                    unsupported(
                        token,
                        "CAST of a character string to ${target.name} (PartiQL cannot convert a string value to ${target.name})",
                    )
                }
                val text = x.literal!!.stringValue()
                val trimmed = text.trim(' ')
                val converted =
                    RedshiftConditions.datetimeLiteral(datetime, trimmed, target.pType)
                        ?: unsupported(token, "CAST of the character string '$text' to ${target.name}")
                builder.note(
                    token,
                    "Redshift CAST of the character string '$text' to ${target.name} was replaced by a ${target.name} literal",
                )
                converted
            }
            TypeFamily.DATETIME -> {
                val from = x.type.code()
                val allowed =
                    when (from) {
                        PType.DATE -> to == PType.DATE || to == PType.TIMESTAMP || to == PType.TIMESTAMPZ
                        PType.TIMESTAMP -> to != PType.TIMEZ
                        PType.TIMESTAMPZ -> true
                        PType.TIME -> to == PType.TIME || to == PType.TIMEZ
                        PType.TIMEZ -> to == PType.TIME || to == PType.TIMEZ
                        else -> false
                    }
                if (!allowed) disallowed(token, x, target)
                if (from == PType.TIMEZ && to == PType.TIME && hasOffset(x)) {
                    unsupported(token, "CAST of a TIMETZ literal with an offset to TIME: Redshift converts it in the session time zone")
                }
                if (from == PType.TIMESTAMPZ && to == PType.TIMEZ) {
                    // The time of day of the UTC instant, with the UTC offset.
                    exprCast(exprCast(x.expr, DataType.TIME(DATETIME_PRECISION)), target.type)
                } else {
                    exprCast(x.expr, target.type)
                }
            }
            else -> disallowed(token, x, target)
        }
    }

    private fun hasOffset(x: TypedOperand): Boolean {
        val text = x.literal?.takeIf { it.code() == Literal.TYPED_STRING }?.stringValue() ?: return false
        return !text.endsWith("+00:00") && !text.endsWith("-00:00")
    }

    /** The cast of a character string literal, converted at translation time to [value] of [type]. */
    private fun literalCast(
        token: Token,
        x: TypedOperand,
        value: Expr,
        type: DataType,
    ): Expr {
        builder.note(
            token,
            "Redshift CAST of the character string '${x.literal!!.stringValue()}' was replaced by a CAST of a numeric literal",
        )
        return exprCast(value, type)
    }

    /** A number in the text of a character string, as Redshift reads it, or null. */
    private fun parseNumber(text: String): BigDecimal? {
        val trimmed = text.trim(*RedshiftConditions.WHITESPACE)
        if (!NUMBER_TEXT.matches(trimmed)) return null
        return BigDecimal(trimmed.removePrefix("+"))
    }

    /**
     * True if [ctx], a cast, is a SELECT item of the outermost query, where its value is only returned. Elsewhere a
     * CHAR(n) value takes part in comparisons and functions, where Redshift ignores its padding.
     */
    private fun isTopLevelSelectItem(ctx: ParserRuleContext): Boolean {
        var c = ctx.parent as? ParserRuleContext
        while (c != null && c !is P.SelectItemExprContext) {
            val transparent =
                c is P.PrimaryAtomContext || c is P.ValuePrimaryContext || c is P.PredicateBaseContext || c is P.NotBaseContext ||
                    c is P.AndBaseContext || c is P.OrBaseContext || c is P.ExprContext || c is P.AtomParenthesizedContext
            if (!transparent) return false
            c = c.parent as? ParserRuleContext
        }
        var block = c?.parent as? ParserRuleContext
        while (block != null && block !is P.RootContext) {
            // A query block that is not part of a set operation, a WITH query, or a subquery.
            val outermost =
                block is P.SelectListContext || block is P.QuerySpecificationContext ||
                    block is P.QueryPrimarySpecificationContext || block is P.QueryTermPrimaryContext || block is P.QueryContext
            if (!outermost) return false
            block = block.parent as? ParserRuleContext
        }
        return block != null
    }

    private companion object {
        val SMALLINT = Target.Simple("SMALLINT", DataType.SMALLINT(), PType.smallint())
        val INTEGER = Target.Simple("INTEGER", DataType.INT(), PType.integer())
        val BIGINT = Target.Simple("BIGINT", DataType.BIGINT(), PType.bigint())
        val REAL = Target.Simple("REAL", DataType.REAL(), PType.real())
        val DOUBLE = Target.Simple("DOUBLE PRECISION", DataType.DOUBLE_PRECISION(), PType.doublePrecision())
        val BOOLEAN = Target.Simple("BOOLEAN", DataType.BOOL(), PType.bool())
        val DATE = Target.Simple("DATE", DataType.DATE(), PType.date())
        val TIME = Target.Simple("TIME", DataType.TIME(DATETIME_PRECISION), PType.time(DATETIME_PRECISION))
        val TIMETZ = Target.Simple("TIMETZ", DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION), PType.timez(DATETIME_PRECISION))

        // TIMESTAMP without a precision is TIMESTAMP(6) in PartiQL.
        val TIMESTAMP = Target.Simple("TIMESTAMP", DataType.TIMESTAMP(), PType.timestamp(DATETIME_PRECISION))
        val TIMESTAMPTZ =
            Target.Simple("TIMESTAMPTZ", DataType.TIMESTAMP_WITH_TIME_ZONE(DATETIME_PRECISION), PType.timestampz(DATETIME_PRECISION))

        const val DEFAULT_DECIMAL_PRECISION = 18
        const val MAX_DECIMAL_SCALE = 37
        const val REAL_PRECISION = 24
        const val MAX_FLOAT_PRECISION = 53
        const val MAX_CHAR_LENGTH = 4096
        const val MAX_VARCHAR_LENGTH = 65535
        const val DEFAULT_VARCHAR_LENGTH = 256
        const val MAX_PLAIN_SCALE = 6
        const val ASCII_MAX = 0x7F

        const val VARCHAR_NO_LENGTH = "CAST to VARCHAR without a length of a string that can exceed 256 bytes"

        val NUMBER_TEXT = Regex("[+-]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][+-]?[0-9]+)?")
    }
}
