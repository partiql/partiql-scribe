package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isApproximateNumeric
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDecimal
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isExactNumeric
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isInteger
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isNumeric
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isUnknown
import org.partiql.scribe.frontend.redshift.internal.RedshiftFunctions.Rule
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The rules that translate Redshift mathematical functions, and the date form of TRUNC; see [RedshiftFunctions].
 *
 * PartiQL has no rounding functions, so FLOOR, CEIL, ROUND, and TRUNC are written with `%`, which gives the exact
 * remainder with the sign of the dividend, and with casts to DECIMAL. The rules depend on the static type of the
 * argument. An argument of type SUPER is unsupported: Redshift returns NULL for a value that is not a number, and
 * PartiQL returns MISSING.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/Math_functions.html
 */
internal class RedshiftMathFunctions(
    private val functions: RedshiftFunctions,
) {
    val rules: Map<String, Rule> =
        mapOf(
            "abs" to Rule(::abs),
            "mod" to Rule(::mod),
            "round" to Rule(::round),
            "floor" to Rule { name, args, setq -> floorCeil(floor = true, name, args, setq) },
            "ceil" to Rule { name, args, setq -> floorCeil(floor = false, name, args, setq) },
            "ceiling" to Rule { name, args, setq -> floorCeil(floor = false, name, args, setq) },
            "trunc" to Rule(::trunc),
            "sign" to Rule(::sign),
            "pi" to Rule(::pi),
            "power" to Rule(::power),
            "pow" to Rule(::power),
            "sqrt" to functions.unsupported(NO_FUNCTION),
            "cbrt" to functions.unsupported(NO_FUNCTION),
            "exp" to functions.unsupported(NO_FUNCTION),
            "dexp" to functions.unsupported(NO_FUNCTION),
            "ln" to functions.unsupported(NO_FUNCTION),
            "dlog1" to functions.unsupported(NO_FUNCTION),
            "log" to functions.unsupported(NO_FUNCTION),
            "dlog10" to functions.unsupported(NO_FUNCTION),
            "sin" to functions.unsupported(NO_FUNCTION),
            "cos" to functions.unsupported(NO_FUNCTION),
            "tan" to functions.unsupported(NO_FUNCTION),
            "cot" to functions.unsupported(NO_FUNCTION),
            "asin" to functions.unsupported(NO_FUNCTION),
            "acos" to functions.unsupported(NO_FUNCTION),
            "atan" to functions.unsupported(NO_FUNCTION),
            "atan2" to functions.unsupported(NO_FUNCTION),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_DEGREES.html: the formula, and so the last bit, is not documented.
            "degrees" to functions.unsupported("the Redshift formula is not documented"),
            "radians" to functions.unsupported("the Redshift formula is not documented"),
            "random" to functions.unsupported("PartiQL has no random numbers"),
            "checksum" to functions.unsupported("the Redshift hash function is not documented"),
        )

    /**
     * `ABS(x)` returns a value of the type of the argument. PLK types `ABS` of a DECIMAL(p, s) argument as DECIMAL(39,
     * 19), so arithmetic on the result can overflow; the translation casts the result to DECIMAL(p, s).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ABS.html
     */
    private fun abs(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val type = types[0]
            val abs = functions.fn("abs", exprs[0])
            when {
                isDecimal(type) -> exprCast(abs, DataType.DECIMAL(type.precision, type.scale))
                isNumeric(type) || isUnknown(type) -> abs
                else -> unsupported(name, type)
            }
        }
    }

    /**
     * `MOD(a, b)` is `a % b` for integer and DECIMAL arguments: the remainder has the sign of `a` in both languages.
     * Redshift returns INTEGER when either argument is INTEGER, while PartiQL returns the wider type; the value always
     * fits. Redshift defines MOD only for integer and DECIMAL arguments.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_MOD.html
     */
    private fun mod(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `MOD(a, b)` was replaced by PartiQL `a % b`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            for (type in types) {
                if (isApproximateNumeric(type)) throw UnsupportedConstructException.at(name, "`MOD` with a floating-point argument")
                if (!isExactNumeric(type) && !isUnknown(type)) unsupported(name, type)
            }
            exprOperator("%", exprs[0], exprs[1])
        }
    }

    /**
     * `ROUND(x [, n])` rounds half away from zero to n digits after the decimal point (0 by default). n must be an
     * integer literal.
     *  - DECIMAL(p, s): `x` when n >= s, and `CAST(x AS DECIMAL(38, n))` when 0 <= n < s: a single rounding, half away
     *    from zero, which cannot overflow. For n < 0, with m = 10^-n:
     *
     *    ```
     *    CASE WHEN x % m >= m / 2 THEN CAST(x - x % m AS DECIMAL(38, 0)) + m
     *         WHEN x % m <= -m / 2 THEN CAST(x - x % m AS DECIMAL(38, 0)) - m
     *         ELSE CAST(x - x % m AS DECIMAL(38, 0)) END
     *    ```
     *  - SMALLINT, INTEGER: `x` when n >= 0.
     *  - BIGINT, and integers with n < 0: unsupported. Redshift defines ROUND for DECIMAL and DOUBLE PRECISION, and does
     *    not document the conversion of an integer argument; a conversion to DOUBLE PRECISION would change a BIGINT.
     *  - REAL, DOUBLE PRECISION: unsupported; Redshift does not document how it rounds ties.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ROUND.html
     */
    private fun round(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1..2)
        functions.scalar(name, setq)
        val n = digits(name, args)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val x = exprs[0]
            val type = types[0]
            when {
                isUnknown(type) -> x
                type.code() == PType.BIGINT -> unsupported(name, "`ROUND` of a BIGINT value (Redshift does not document the conversion)")
                isInteger(type) -> {
                    if (n < 0) unsupported(name, "`ROUND` of an integer value to a negative number of digits")
                    functions.note(name, "Redshift `ROUND` of an integer value was replaced by the value")
                    x
                }
                isDecimal(type) && n >= type.scale -> {
                    functions.note(name, "Redshift `ROUND` to at least the scale of the value was replaced by the value")
                    x
                }
                isDecimal(type) && n >= 0 -> {
                    functions.note(
                        name,
                        "Redshift `ROUND(x, $n)` was replaced by PartiQL `CAST(x AS DECIMAL(38, $n))`, which rounds half away " +
                            "from zero; the result type is DECIMAL(38, $n)",
                    )
                    exprCast(x, DataType.DECIMAL(MAX_PRECISION, n.toInt()))
                }
                isDecimal(type) -> {
                    if (n <= -MAX_PRECISION) unsupported(name, "`ROUND` to $n digits")
                    functions.note(name, "Redshift `ROUND(x, $n)` was rewritten with PartiQL `%`; the result type is DECIMAL(38, 0)")
                    val m = BigInteger.TEN.pow((-n).toInt())
                    val half = m.divide(BigInteger.TWO)
                    val remainder = exprOperator("%", x, int(m))
                    val truncated = exprCast(exprOperator("-", x, remainder), DataType.DECIMAL(MAX_PRECISION, 0))
                    exprCase(
                        null,
                        listOf(
                            exprCaseBranch(exprOperator(">=", remainder, int(half)), exprOperator("+", truncated, int(m))),
                            exprCaseBranch(exprOperator("<=", remainder, int(half.negate())), exprOperator("-", truncated, int(m))),
                        ),
                        truncated,
                    )
                }
                isApproximateNumeric(
                    type,
                ) -> unsupported(name, "`ROUND` of a floating-point value (Redshift does not document how it rounds ties)")
                else -> unsupported(name, type)
            }
        }
    }

    /**
     * `FLOOR(x)` and `CEIL(x)` (or `CEILING(x)`), with `x % 1`, which is the exact fraction with the sign of `x`:
     *  - SMALLINT, INTEGER, BIGINT: `x`.
     *  - DECIMAL: `CASE WHEN x % 1 < 0 THEN CAST(x - x % 1 AS DECIMAL(38, 0)) - 1 ELSE CAST(x - x % 1 AS DECIMAL(38, 0))
     *    END` for FLOOR, with `> 0` and `+ 1` for CEIL. The cast is exact because the value is an integer, and it removes
     *    the scale, as Redshift does.
     *  - REAL, DOUBLE PRECISION: `CASE WHEN x - x = 0 THEN CASE WHEN x % 1 < 0 THEN x - x % 1 - 1 ELSE x - x % 1 END ELSE x
     *    END`; the first test returns infinite values unchanged.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_FLOOR.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CEILING_FLOOR.html
     */
    private fun floorCeil(
        floor: Boolean,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val x = exprs[0]
            val type = types[0]
            val test = if (floor) "<" else ">"
            val step = if (floor) "-" else "+"
            when {
                isUnknown(type) -> x
                isInteger(type) || (isDecimal(type) && type.scale == 0) -> {
                    functions.note(name, "Redshift `$fn` of an integer value was replaced by the value")
                    x
                }
                isDecimal(type) -> {
                    functions.note(name, "Redshift `$fn(x)` was rewritten with PartiQL `%`; the result type is DECIMAL(38, 0)")
                    val fraction = exprOperator("%", x, functions.int(1))
                    val truncated = exprCast(exprOperator("-", x, fraction), DataType.DECIMAL(MAX_PRECISION, 0))
                    exprCase(
                        null,
                        listOf(
                            exprCaseBranch(exprOperator(test, fraction, functions.int(0)), exprOperator(step, truncated, functions.int(1))),
                        ),
                        truncated,
                    )
                }
                isApproximateNumeric(type) -> {
                    functions.note(name, "Redshift `$fn(x)` was rewritten with PartiQL `%`")
                    val fraction = exprOperator("%", x, functions.int(1))
                    val truncated = exprOperator("-", x, fraction)
                    val rounded =
                        exprCase(
                            null,
                            listOf(
                                exprCaseBranch(
                                    exprOperator(test, fraction, functions.int(0)),
                                    exprOperator(step, truncated, functions.int(1)),
                                ),
                            ),
                            truncated,
                        )
                    exprCase(null, listOf(exprCaseBranch(finite(x), rounded)), x)
                }
                else -> unsupported(name, type)
            }
        }
    }

    /**
     * `TRUNC(x [, n])` truncates a number toward zero to n digits after the decimal point (0 by default), and
     * `TRUNC(timestamp)` returns the date of a timestamp. n must be an integer literal.
     *  - TIMESTAMP, DATE: `CAST(x AS DATE)`.
     *  - TIMESTAMPTZ: the date in UTC, the Redshift session time zone: `DATE_ADD(DAY, DATE_DIFF(DAY, R, x), DATE
     *    '0001-01-01')`, with R the first instant of that date in UTC.
     *  - DECIMAL(p, s): `x` when n >= s, else `CAST(x - x % 10^-n AS DECIMAL(38, max(n, 0)))`; `x % 10^-n` is the exact
     *    part to remove, with the sign of `x`.
     *  - SMALLINT, INTEGER: `x` when n >= 0, else `x - x % 10^-n`.
     *  - REAL, DOUBLE PRECISION, without n: `CASE WHEN x - x = 0 THEN x - x % 1 ELSE x END`.
     *  - BIGINT: unsupported. Redshift defines TRUNC for DECIMAL and DOUBLE PRECISION and does not document the
     *    conversion of an integer argument; a conversion to DOUBLE PRECISION would change a BIGINT.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRUNC.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRUNC_date.html
     */
    private fun trunc(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1..2)
        functions.scalar(name, setq)
        val n = digits(name, args)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val x = exprs[0]
            val type = types[0]
            when {
                isUnknown(type) -> x
                type.code() == PType.TIMESTAMP || type.code() == PType.DATE -> {
                    if (args.size == 2) unsupported(name, "`TRUNC` of a ${FunctionTypes.name(type)} value with a number of digits")
                    functions.note(name, "Redshift `TRUNC(timestamp)` was replaced by PartiQL `CAST(... AS DATE)`")
                    exprCast(x, DataType.DATE())
                }
                type.code() == PType.TIMESTAMPZ -> {
                    if (args.size == 2) unsupported(name, "`TRUNC` of a TIMESTAMPTZ value with a number of digits")
                    functions.note(name, "Redshift `TRUNC(timestamptz)` was replaced by the date in UTC, the Redshift session time zone")
                    utcDate(x)
                }
                type.code() == PType.BIGINT -> unsupported(name, "`TRUNC` of a BIGINT value (Redshift does not document the conversion)")
                isInteger(type) && n >= 0 -> {
                    functions.note(name, "Redshift `TRUNC` of an integer value was replaced by the value")
                    x
                }
                isInteger(type) -> {
                    if (n <= -MAX_PRECISION) unsupported(name, "`TRUNC` to $n digits")
                    val unit = BigInteger.TEN.pow((-n).toInt())
                    functions.note(name, "Redshift `TRUNC(x, $n)` was rewritten as `x - x % $unit`")
                    exprOperator("-", x, exprOperator("%", x, int(unit)))
                }
                isDecimal(type) && n >= type.scale -> {
                    functions.note(name, "Redshift `TRUNC` to at least the scale of the value was replaced by the value")
                    x
                }
                isDecimal(type) -> {
                    if (n <= -MAX_PRECISION) unsupported(name, "`TRUNC` to $n digits")
                    val unit = BigDecimal.ONE.movePointLeft(n.toInt())
                    functions.note(name, "Redshift `TRUNC(x, $n)` was rewritten as `x - x % ${unit.toPlainString()}`")
                    val remainder = exprOperator("%", x, exactLiteral(unit))
                    exprCast(exprOperator("-", x, remainder), DataType.DECIMAL(MAX_PRECISION, maxOf(n, 0L).toInt()))
                }
                isApproximateNumeric(type) -> {
                    if (n != 0L) unsupported(name, "`TRUNC` of a floating-point value with digits")
                    functions.note(name, "Redshift `TRUNC(x)` was rewritten as `x - x % 1`")
                    exprCase(null, listOf(exprCaseBranch(finite(x), exprOperator("-", x, exprOperator("%", x, functions.int(1))))), x)
                }
                else -> unsupported(name, type)
            }
        }
    }

    /**
     * The date of the TIMESTAMPTZ value [x] in UTC: `DATE_ADD(DAY, DATE_DIFF(DAY, R, x), DATE '0001-01-01')`, where R is
     * `TIMESTAMP '0001-01-01 00:00:00'` in UTC. PartiQL `CAST(x AS DATE)` would take the date at the offset of the value.
     */
    private fun utcDate(x: Expr): Expr {
        val epoch =
            exprCast(
                exprLit(Literal.typedString(DataType.TIMESTAMP(), "0001-01-01 00:00:00")),
                DataType.TIMESTAMP_WITH_TIME_ZONE(),
            )
        val days = exprCall(Identifier.regular("${HIDDEN}date_diff_day"), listOf(epoch, x), null)
        return exprCall(
            Identifier.regular("${HIDDEN}date_add_day"),
            listOf(days, exprLit(Literal.typedString(DataType.DATE(), "0001-01-01"))),
            null,
        )
    }

    /**
     * `SIGN(x)` is `CASE WHEN x > 0 THEN 1 WHEN x < 0 THEN -1 WHEN x = 0 THEN 0 END`, which is NULL for NULL. Redshift
     * returns the type of the argument for DOUBLE PRECISION and DECIMAL(1, 0) for DECIMAL; the PartiQL result is an
     * INTEGER of the same value. REAL and DOUBLE PRECISION values are assumed not to be NaN.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SIGN.html
     */
    private fun sign(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val x = exprs[0]
            if (!isNumeric(types[0]) && !isUnknown(types[0])) unsupported(name, types[0])
            functions.note(name, "Redshift `SIGN(x)` was rewritten as a CASE expression; the result type is INTEGER")
            exprCase(
                null,
                listOf(
                    exprCaseBranch(exprOperator(">", x, functions.int(0)), functions.int(1)),
                    exprCaseBranch(exprOperator("<", x, functions.int(0)), functions.int(-1)),
                    exprCaseBranch(exprOperator("=", x, functions.int(0)), functions.int(0)),
                ),
                null,
            )
        }
    }

    /**
     * `PI()` is the DOUBLE PRECISION value nearest to pi, `CAST(3.141592653589793 AS DOUBLE PRECISION)`. A bare
     * 3.141592653589793 is a DECIMAL in PartiQL.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_PI.html
     */
    private fun pi(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 0)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `PI()` was replaced by the DOUBLE PRECISION value 3.141592653589793")
        return exprCast(exprLit(Literal.exactNum(Math.PI.toBigDecimal().toPlainString())), DataType.DOUBLE_PRECISION())
    }

    /**
     * `POWER(a, b)` and `POW(a, b)` return a DOUBLE PRECISION value. PartiQL has no power function, so the translation
     * requires numeric literals whose exact power is a DOUBLE PRECISION value: `a` is a DOUBLE PRECISION value, `b` is an
     * integer, and the power is exact (for a negative `b`, `a` is a power of 2). Then every implementation of the power
     * with an error of less than one unit in the last place returns that value, and the translation is `CAST(<power> AS
     * DOUBLE PRECISION)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_POWER.html
     */
    private fun power(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        val base = numericLiteral(args[0].expr)
        val exponent = numericLiteral(args[1].expr)
        if (base == null || exponent == null) throw UnsupportedConstructException.at(name, "`$fn` with a non-literal argument")
        val result =
            exactPower(base, exponent)
                ?: throw UnsupportedConstructException.at(name, "`$fn` whose result is not an exact DOUBLE PRECISION value")
        functions.note(name, "Redshift `$fn(${args[0].ctx.text}, ${args[1].ctx.text})` was replaced by its exact DOUBLE PRECISION value")
        return exprCast(exactLiteral(result), DataType.DOUBLE_PRECISION())
    }

    /**
     * Returns base^exponent if [exponent] is an integer and the power is exact and a finite DOUBLE PRECISION value, or
     * null.
     */
    private fun exactPower(
        base: BigDecimal,
        exponent: BigDecimal,
    ): BigDecimal? {
        val e =
            try {
                exponent.toBigIntegerExact().intValueExact()
            } catch (ex: ArithmeticException) {
                return null
            }
        if (!isDouble(base) || e > MAX_EXPONENT || e < -MAX_EXPONENT) return null
        val result =
            when {
                e >= 0 -> base.pow(e)
                base.signum() == 0 -> return null
                else -> {
                    // 1 / base^|e| is a finite decimal only when |base| is a power of 2.
                    val power = base.pow(-e)
                    try {
                        BigDecimal.ONE.divide(power)
                    } catch (ex: ArithmeticException) {
                        return null
                    }
                }
            }
        return if (isDouble(result)) result.stripTrailingZeros() else null
    }

    /** True if [value] is a finite DOUBLE PRECISION value. */
    private fun isDouble(value: BigDecimal): Boolean {
        val d = value.toDouble()
        return d.isFinite() && BigDecimal(d).compareTo(value) == 0
    }

    private fun numericLiteral(expr: Expr): BigDecimal? {
        val lit = (expr as? ExprLit)?.lit ?: return null
        return when (lit.code()) {
            Literal.INT_NUM, Literal.EXACT_NUM -> BigDecimal(lit.numberValue())
            else -> null
        }
    }

    /**
     * Returns n, the number of digits of `ROUND(x, n)` or `TRUNC(x, n)` (0 when it is absent), which must be an integer
     * literal.
     */
    private fun digits(
        name: Token,
        args: List<Operand>,
    ): Long {
        if (args.size < 2) return 0
        return functions.intLiteral(args[1].expr)
            ?: throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` with a non-literal number of digits")
    }

    /** `x - x = 0`, which is false for infinite values. */
    private fun finite(x: Expr): Expr = exprOperator("=", exprOperator("-", x, x), functions.int(0))

    private fun int(value: BigInteger): Expr = exprLit(Literal.intNum(value))

    /** An exact numeric literal for [value]: an integer literal for an integer value, else a DECIMAL literal. */
    private fun exactLiteral(value: BigDecimal): Expr {
        val plain = value.stripTrailingZeros()
        return if (plain.scale() <= 0) int(plain.toBigIntegerExact()) else exprLit(Literal.exactNum(plain.toPlainString()))
    }

    private fun unsupported(
        name: Token,
        type: PType,
    ): Nothing = throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` of ${FunctionTypes.describe(type, "value")}")

    private fun unsupported(
        name: Token,
        construct: String,
    ): Nothing = throw UnsupportedConstructException.at(name, construct)

    private companion object {
        const val NO_FUNCTION = "PartiQL has no equivalent function"
        const val MAX_PRECISION = 38

        /** Larger exponents of exact powers overflow DOUBLE PRECISION. */
        const val MAX_EXPONENT = 1100

        /** The prefix of hidden PartiQL built-in function names; see `FunctionUtils` in partiql-spi. */
        const val HIDDEN = "\uFDEF"
    }
}
