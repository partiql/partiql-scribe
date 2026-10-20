package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.TrimSpec
import org.partiql.spi.types.PType
import java.math.BigDecimal
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates the Redshift arithmetic operators, the sign operators, `||`, and the other operators; see
 * [RedshiftExpressions].
 *
 * The result types of PartiQL arithmetic are the Redshift result types for integer and DECIMAL operands, except for
 * DECIMAL division, whose result scale differs. Redshift computes REAL with another number in DOUBLE PRECISION, and
 * PartiQL in REAL, so the translation casts such operands to DOUBLE PRECISION.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_numeric_computations201.html
 */
internal class RedshiftOperators(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val datetime: RedshiftDatetime,
    private val types: OperandTypes,
) {
    /**
     * `+` and `-`. When an operand is a datetime or interval value, the Redshift semantics depend on the operand types;
     * see [RedshiftDatetime.additive].
     */
    fun additive(ctx: P.ValueAdditiveContext): Expr = arithmetic(ctx.op, operand(ctx.lhs), operand(ctx.rhs))

    /** `*`, `/`, and `%`. */
    fun multiplicative(ctx: P.ValueMultiplicativeContext): Expr = arithmetic(ctx.op, operand(ctx.lhs), operand(ctx.rhs))

    private fun operand(ctx: P.ValueExprContext): Operand = Operand(ctx, exprs.valueExpr(ctx))

    private fun arithmetic(
        op: Token,
        lhs: Operand,
        rhs: Operand,
    ): Expr =
        types.resolveTyped(listOf(lhs, rhs)) { (a, b) ->
            val additive = if (op.type == P.PLUS || op.type == P.MINUS) datetime.additive(op, a.expr, a.type, b.expr, b.type) else null
            additive?.let { builder.functions.checked(op, "datetime ${op.text}", it) } ?: numeric(op, a, b)
        }

    private fun numeric(
        op: Token,
        a: TypedOperand,
        b: TypedOperand,
    ): Expr {
        val fa = a.family
        val fb = b.family
        if (fa == TypeFamily.SUPER || fb == TypeFamily.SUPER) unsupported(op, "arithmetic with a SUPER operand")
        // Redshift multiplies and divides an interval by a number with the PostgreSQL interval rules (fractions of months
        // carry into days), which PartiQL does not; only an integer multiplier gives the same interval.
        val multiplicative = op.type == P.ASTERISK || op.type == P.SLASH || op.type == P.PERCENT
        if (multiplicative && (fa == TypeFamily.INTERVAL || fb == TypeFamily.INTERVAL)) {
            val other = if (fa == TypeFamily.INTERVAL) fb else fa
            if (op.type != P.ASTERISK || other != TypeFamily.INTEGER) {
                unsupported(op, "interval `${op.text}` with ${other.name.lowercase()} operand")
            }
            return exprOperator(op.text, a.expr, b.expr)
        }
        if (a.untyped || b.untyped) {
            // The type of an operand with an aggregate call is not known; see [resolveTyped]. The scale of a quotient
            // depends on it.
            if (op.type == P.SLASH || op.type == P.PERCENT) {
                unsupported(
                    op,
                    "`${op.text}` of an aggregate expression whose type is not known",
                )
            }
            return exprOperator(op.text, a.expr, b.expr)
        }
        if (fa == TypeFamily.TEXT || fb == TypeFamily.TEXT) unsupported(op, "arithmetic with a character string operand")
        if (!fa.isNumeric || !fb.isNumeric) return exprOperator(op.text, a.expr, b.expr)
        val ca = a.type.code()
        val cb = b.type.code()
        if (op.type == P.PERCENT && (fa == TypeFamily.FLOAT || fb == TypeFamily.FLOAT)) {
            // Redshift defines % for integer and DECIMAL arguments, and converts DOUBLE PRECISION to DECIMAL.
            unsupported(op, "`%` with a floating-point operand")
        }
        if (ca == PType.REAL && cb == PType.REAL) unsupported(op, "arithmetic on two REAL operands")
        if (ca == PType.REAL || cb == PType.REAL) {
            if (ca != PType.DOUBLE && cb != PType.DOUBLE) return exprOperator(op.text, toDouble(a), toDouble(b))
            return exprOperator(op.text, a.expr, b.expr)
        }
        if (fa == TypeFamily.FLOAT || fb == TypeFamily.FLOAT) return exprOperator(op.text, a.expr, b.expr)
        val decimal = fa == TypeFamily.DECIMAL || fb == TypeFamily.DECIMAL
        if (decimal && op.type == P.ASTERISK && scaleOf(a.type) + scaleOf(b.type) > MAX_DECIMAL_PRECISION) {
            unsupported(op, "DECIMAL multiplication with result scale above 38")
        }
        if (decimal && op.type == P.SLASH) decimalDivision(op, a, b)
        return exprOperator(op.text, a.expr, b.expr)
    }

    private fun toDouble(operand: TypedOperand): Expr =
        if (operand.type.code() == PType.DOUBLE) operand.expr else exprCast(operand.expr, DataType.DOUBLE_PRECISION())

    /**
     * DECIMAL division. The result scale is `max(4, s1 + p2 - s2 + 1)` in Redshift (reduced when the precision exceeds
     * 38) and `max(6, s1 + p2 + 1)` in PartiQL, and the Redshift rounding at that scale is not documented. The quotient
     * is exact, so the values are equal, when the divisor is a literal `d` whose reciprocal has `f` fraction digits and
     * `s1 + f` is at most both scales; other DECIMAL divisions are unsupported. PartiQL fails when its precision exceeds
     * 38, so such a division is unsupported too.
     */
    private fun decimalDivision(
        op: Token,
        a: TypedOperand,
        b: TypedOperand,
    ) {
        val divisor = b.literal?.takeIf { b.isNumericLiteral }?.let { BigDecimal(it.numberValue()) }
        if (divisor == null || divisor.signum() == 0) unsupported(op, "DECIMAL division")
        val reciprocal =
            try {
                BigDecimal.ONE.divide(divisor.abs())
            } catch (ex: ArithmeticException) {
                unsupported(op, "DECIMAL division")
            }
        val fraction = reciprocal.stripTrailingZeros().scale().coerceAtLeast(0)
        val s1 = scaleOf(a.type)
        val p1 = s1 + integerDigits(a.type)
        val s2 = scaleOf(b.type)
        val p2 = s2 + integerDigits(b.type)
        val partiqlScale = maxOf(PARTIQL_DIVISION_SCALE, s1 + p2 + 1)
        var redshiftScale = maxOf(REDSHIFT_DIVISION_SCALE, s1 + p2 - s2 + 1)
        val redshiftPrecision = p1 - s1 + s2 + redshiftScale
        if (redshiftPrecision > MAX_DECIMAL_PRECISION) {
            redshiftScale = maxOf(MAX_DECIMAL_PRECISION + redshiftScale - redshiftPrecision, REDSHIFT_DIVISION_SCALE)
        }
        val exact = s1 + fraction <= minOf(partiqlScale, redshiftScale)
        if (!exact || p1 - s1 + s2 + partiqlScale > MAX_DECIMAL_PRECISION) unsupported(op, "DECIMAL division")
        builder.note(
            op,
            "Redshift DECIMAL division by ${b.ctx.text} is exact; " +
                "the result scale is $redshiftScale in Redshift and $partiqlScale in PartiQL",
        )
    }

    /**
     * A sign. A sign on a numeric literal becomes part of the literal, the same as in the PartiQL parser. PartiQL types
     * `-x` for a DECIMAL(p,s) value as DECIMAL(38,19), so the translation casts it back to DECIMAL(p,s).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html
     */
    fun signed(ctx: P.ValueSignedContext): Expr {
        val sign = ctx.sign
        val value = exprs.valueExpr(ctx.valueExpr())
        if (value is ExprLit) {
            val lit = value.lit
            when (lit.code()) {
                Literal.INT_NUM, Literal.EXACT_NUM -> {
                    if (sign.type == P.PLUS) return value
                    val text = lit.numberValue()
                    val negated = if (text.startsWith("-")) text.removePrefix("-") else "-$text"
                    return exprLit(if (lit.code() == Literal.INT_NUM) Literal.intNum(negated) else Literal.exactNum(negated))
                }
                Literal.STRING -> unsupported(sign, "sign on a character string literal")
                Literal.TYPED_STRING ->
                    if (lit.dataType().code() == org.partiql.ast.DataType.INTERVAL) {
                        return negateInterval(sign, value)
                    } else {
                        return exprOperator(sign.text, null, value)
                    }
                else -> return exprOperator(sign.text, null, value)
            }
        }
        return types.resolveTyped(listOf(Operand(ctx.valueExpr(), value))) { (x) ->
            when (x.family) {
                TypeFamily.SUPER -> unsupported(sign, "arithmetic with a SUPER operand")
                TypeFamily.DECIMAL -> if (sign.type == P.PLUS) x.expr else castTo(exprOperator("-", null, x.expr), x.type)
                TypeFamily.INTERVAL -> negateInterval(sign, x.expr)
                else -> exprOperator(sign.text, null, x.expr)
            }
        }
    }

    /** PartiQL has no sign operators for intervals: `-i` is `i * -1`. */
    private fun negateInterval(
        sign: Token,
        interval: Expr,
    ): Expr {
        if (sign.type == P.PLUS) return interval
        builder.note(sign, "Redshift `-interval` was replaced by `interval * -1`")
        return exprOperator("*", interval, exprLit(Literal.intNum(-1)))
    }

    /**
     * The prefix operators: `@` is ABS; `|/` (square root), `||/` (cube root), and `~` (bitwise NOT) have no PartiQL
     * equivalent. The PartiQL planner does not find an aggregate call in the argument of ABS.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html
     */
    fun prefix(ctx: P.ValuePrefixOperatorContext): Expr {
        val op = ctx.op
        if (op.type != P.AT_SIGN) unsupported(op, "operator `${op.text}`")
        if (containsAggregate(ctx.valueExpr())) {
            unsupported(op, "`@` on an aggregate expression (PartiQL does not plan it as an argument of ABS)")
        }
        val value = Operand(ctx.valueExpr(), exprs.valueExpr(ctx.valueExpr()))
        return types.resolveTyped(listOf(value)) { (x) ->
            if (x.family == TypeFamily.SUPER) unsupported(op, "arithmetic with a SUPER operand")
            builder.note(op, "Redshift `@` was replaced by PartiQL `ABS`")
            val abs = exprCall(Identifier.regular("abs"), listOf(x.expr), null)
            // PartiQL types ABS of DECIMAL(p,s) as DECIMAL(39,19); Redshift returns DECIMAL(p,s).
            if (x.family == TypeFamily.DECIMAL) castTo(abs, x.type) else abs
        }
    }

    /**
     * `^` (exponentiation) has no PartiQL equivalent.
     */
    fun exponent(ctx: P.ValueExponentContext): Expr = unsupported(ctx.op, "operator `^`")

    /**
     * The operators `||`, `&`, `|`, `<<`, and `>>`.
     */
    fun operator(ctx: P.ValueOperatorContext): Expr {
        val op = ctx.op
        return when (op.type) {
            P.CONCAT -> concat(op, operand(ctx.lhs), operand(ctx.rhs))
            P.AMPERSAND -> bitwiseAnd(op, operand(ctx.lhs), operand(ctx.rhs))
            else -> unsupported(op, "operator `${op.text}`")
        }
    }

    /**
     * `a || b`. Redshift removes the trailing blanks of a CHAR(n) operand when it converts it to VARCHAR, and converts an
     * operand that is not a character string to VARCHAR; see [toText].
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_concat_op.html
     */
    private fun concat(
        op: Token,
        lhs: Operand,
        rhs: Operand,
    ): Expr =
        types.resolveTyped(listOf(lhs, rhs)) { (a, b) ->
            if (a.type.code() == PType.CHAR && b.type.code() == PType.CHAR) unsupported(op, "`||` of two CHAR values")
            val text = listOf(a, b).any { it.family in setOf(TypeFamily.TEXT, TypeFamily.SUPER, TypeFamily.UNKNOWN, TypeFamily.UNTYPED) }
            if (!text) unsupported(op, "`||` without a character string operand")
            exprOperator("||", toText(op, a, "`||`", trimChar = true), toText(op, b, "`||`", trimChar = true))
        }

    /**
     * `a & b` for integer operands. PartiQL also accepts DECIMAL operands, and computes with their integer part.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html
     */
    private fun bitwiseAnd(
        op: Token,
        lhs: Operand,
        rhs: Operand,
    ): Expr =
        types.resolveTyped(listOf(lhs, rhs)) { (a, b) ->
            val integers = listOf(a, b).all { it.family == TypeFamily.INTEGER || it.family == TypeFamily.UNKNOWN }
            if (!integers) unsupported(op, "`&` with a non-integer operand")
            exprOperator("&", a.expr, b.expr)
        }

    internal companion object {
        const val REDSHIFT_DIVISION_SCALE = 4
        const val PARTIQL_DIVISION_SCALE = 6

        /** The largest DECIMAL scale whose values PartiQL converts to text without an exponent. */
        private const val MAX_PLAIN_SCALE = 6

        /**
         * Converts [x] to a character string, as Redshift converts the operand of a string operator implicitly. Integers
         * and DATE values convert exactly with `CAST(x AS STRING)`. A DECIMAL value converts exactly when its scale is at
         * most 6, so that PartiQL writes no exponent, and when its scale is the Redshift scale: a column, a literal, a
         * CAST, and `+`, `-`, `*` of these. CHAR(n) values lose their padding when [trimChar]. Other types are unsupported.
         *
         * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
         */
        fun toText(
            op: Token,
            x: TypedOperand,
            construct: String,
            trimChar: Boolean,
        ): Expr =
            when (x.family) {
                TypeFamily.TEXT ->
                    if (trimChar && x.type.code() == PType.CHAR) exprTrim(x.expr, null, TrimSpec.TRAILING()) else x.expr
                TypeFamily.UNKNOWN, TypeFamily.SUPER, TypeFamily.UNTYPED -> x.expr
                TypeFamily.INTEGER -> exprCast(x.expr, DataType.STRING())
                TypeFamily.DECIMAL ->
                    if (scaleOf(x.type) <= MAX_PLAIN_SCALE && hasRedshiftScale(x.ctx)) {
                        exprCast(x.expr, DataType.STRING())
                    } else {
                        unsupported(op, "implicit conversion of a DECIMAL expression to VARCHAR in $construct")
                    }
                else ->
                    if (x.type.code() == PType.DATE) {
                        exprCast(x.expr, DataType.STRING())
                    } else {
                        unsupported(op, "implicit conversion of ${typeName(x.type)} to VARCHAR in $construct")
                    }
            }

        /**
         * True if the PartiQL scale of the DECIMAL value of [ctx] is the Redshift scale: a column or path, a literal, a
         * cast, and the sign, `+`, `-`, and `*` of these. The scale of a quotient or a function result differs.
         */
        fun hasRedshiftScale(ctx: ParserRuleContext): Boolean =
            when (val c = unwrap(ctx)) {
                is P.AtomVariableContext, is P.PathFieldContext, is P.PathElementContext -> true
                is P.LiteralIntegerContext, is P.LiteralDecimalContext -> true
                is P.ValueCastShorthandContext, is P.AtomCastContext, is P.AtomConvertContext -> true
                is P.ValueSignedContext -> hasRedshiftScale(c.valueExpr())
                is P.ValueAdditiveContext -> hasRedshiftScale(c.lhs) && hasRedshiftScale(c.rhs)
                is P.ValueMultiplicativeContext -> c.op.type == P.ASTERISK && hasRedshiftScale(c.lhs) && hasRedshiftScale(c.rhs)
                else -> false
            }
    }
}
