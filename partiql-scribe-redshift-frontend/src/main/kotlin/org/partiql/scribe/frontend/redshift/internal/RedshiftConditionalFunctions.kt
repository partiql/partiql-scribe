package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.DataType
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDecimal
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDynamic
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isInteger
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isNumeric
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isText
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isUnknown
import org.partiql.scribe.frontend.redshift.internal.RedshiftFunctions.Rule
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The rules that translate Redshift conditional functions; see [RedshiftFunctions].
 *
 * The values that a conditional expression can return (the arguments of COALESCE, the results of DECODE, ...) are
 * unified to one result type with [unify]. PLK 1.6.1 types such an expression with the common type of the values, but
 * does not convert the values at run time, so a value of another type makes an expression that uses the result
 * MISSING.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_conditional_expressions.html
 */
internal class RedshiftConditionalFunctions(
    private val functions: RedshiftFunctions,
) {
    val rules: Map<String, Rule> =
        mapOf(
            "coalesce" to Rule { name, args, setq -> coalesce(name, args, setq, nvl = false) },
            "nvl" to Rule { name, args, setq -> coalesce(name, args, setq, nvl = true) },
            "nvl2" to Rule(::nvl2),
            "nullif" to Rule(::nullif),
            "decode" to Rule(::decode),
            "greatest" to Rule { name, args, setq -> extremum(greatest = true, name, args, setq) },
            "least" to Rule { name, args, setq -> extremum(greatest = false, name, args, setq) },
        )

    /**
     * `COALESCE(e1, ..., en)` and its synonym `NVL(e1, ..., en)` return the first argument that is not NULL. The
     * arguments are unified to the result type; see [unify].
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NVL_function.html
     */
    private fun coalesce(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        nvl: Boolean,
    ): Expr {
        functions.scalar(name, setq)
        if (args.isEmpty()) throw UnsupportedConstructException.at(name, "`${name.text}` with no arguments")
        if (nvl) functions.note(name, "Redshift `NVL` was replaced by PartiQL `COALESCE`")
        return functions.resolve(args) { exprs, types -> exprCoalesce(unify(name, Values(args, exprs, types))) }
    }

    /**
     * `NVL2(e, a, b)` is `CASE WHEN e IS NOT NULL THEN a ELSE b END`. Redshift converts `b` to the type of `a`, so the
     * translation requires that the conversion does not change the value: both have the same type, or both are numbers,
     * or one is the NULL literal or a string literal that converts exactly. Character values must have the same type,
     * unless `a` has no length or `b` is a string literal that fits in `a`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NVL2.html
     */
    private fun nvl2(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 3)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `NVL2(e, a, b)` was replaced by `CASE WHEN e IS NOT NULL THEN a ELSE b END`")
        return functions.resolve(args) { exprs, types ->
            val values = Values(args.drop(1), exprs.drop(1), types.drop(1))
            if (!nvl2Compatible(values)) {
                val (ta, tb) = values.types
                throw UnsupportedConstructException.at(
                    name,
                    "`NVL2` with result values of different types (${FunctionTypes.name(ta)} and ${FunctionTypes.name(tb)})",
                )
            }
            val (a, b) = unify(name, values)
            exprCase(null, listOf(exprCaseBranch(exprNullPredicate(exprs[0], true), a)), b)
        }
    }

    private fun nvl2Compatible(values: Values): Boolean {
        val (ta, tb) = values.types
        val stringA = values.string(0)
        val stringB = values.string(1)
        return when {
            values.isNull(0) || values.isNull(1) || ta == tb -> true
            isNumeric(ta) && isNumeric(tb) -> true
            setOf(ta.code(), tb.code()) == setOf(PType.DATE, PType.TIMESTAMP) -> true
            isText(ta) && isText(tb) ->
                stringA != null || ta.code() == PType.STRING ||
                    (stringB != null && stringB.toByteArray(Charsets.UTF_8).size <= ta.length)
            // A string literal `b` converts to the type of `a`; see [unify]. Redshift converts a non-character `b` to the
            // type of a string literal `a`, which the translation does not do.
            stringB != null -> !isText(ta)
            else -> false
        }
    }

    /**
     * `NULLIF(a, b)` returns NULL when `a` equals `b`, and `a` otherwise. Redshift disregards the padding of a CHAR(n)
     * argument, so for such an argument the translation is `CASE WHEN TRIM(TRAILING FROM a) = b THEN NULL ELSE a END`.
     * A string literal `b` with a non-character `a` converts to the type of `a`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NULLIF_function.html
     */
    private fun nullif(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        return functions.resolve(args) { exprs, types ->
            val values = Values(args, exprs, types)
            if (values.string(0) != null && !isText(types[1]) && !isDynamic(types[1]) && !isUnknown(types[1])) {
                throw UnsupportedConstructException.at(
                    name,
                    "`NULLIF` of a character string literal and ${FunctionTypes.describe(types[1], "value")}",
                )
            }
            val (a, b) = comparands(name, values, 0, 1)
            if (types[0].code() == PType.CHAR || types[1].code() == PType.CHAR) {
                exprCase(null, listOf(exprCaseBranch(exprOperator("=", a, b), exprLit(Literal.nul()))), exprs[0])
            } else {
                exprNullIf(exprs[0], b)
            }
        }
    }

    /**
     * `DECODE(e, s1, r1, ..., sk, rk [, default])` returns the result of the first search value that equals `e`, else
     * the default, else NULL. A NULL search value matches a NULL `e`:
     *
     * ```
     * CASE WHEN m1 THEN r1 ... WHEN mk THEN rk ELSE default END
     * ```
     *
     * where `mi` is `e IS NULL` for the NULL literal, `e = si` for another literal, and `e = si OR (e IS NULL AND si IS
     * NULL)` otherwise. The results are unified like the arguments of COALESCE.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DECODE_expression.html
     */
    private fun decode(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.scalar(name, setq)
        if (args.size < 3) throw UnsupportedConstructException.at(name, "`DECODE` with fewer than 3 arguments")
        functions.note(name, "Redshift `DECODE` was replaced by a CASE expression; a NULL search value matches a NULL expression")
        return functions.resolve(args) { exprs, types ->
            val all = Values(args, exprs, types)
            val pairs = (args.size - 1) / 2
            val searches = (0 until pairs).map { 1 + 2 * it }
            val results = searches.map { it + 1 } + listOfNotNull(if (args.size % 2 == 0) args.size - 1 else null)
            val unified = unify(name, all.select(results))
            val e = exprs[0]
            val branches =
                searches.mapIndexed { k, i ->
                    val match =
                        if (all.isNull(i)) {
                            exprNullPredicate(e, false)
                        } else {
                            val (x, y) = comparands(name, all, 0, i)
                            val equal = exprOperator("=", x, y)
                            if (args[i].expr is ExprLit) {
                                equal
                            } else {
                                exprOr(equal, exprAnd(exprNullPredicate(e, false), exprNullPredicate(exprs[i], false)))
                            }
                        }
                    exprCaseBranch(match, unified[k])
                }
            exprCase(null, branches, unified.getOrNull(pairs))
        }
    }

    /**
     * `GREATEST(v1, ..., vn)` and `LEAST(v1, ..., vn)` return the largest or smallest argument, ignoring NULL values,
     * or NULL when all are NULL:
     *
     * ```
     * CASE WHEN c1 THEN v1 ... WHEN c(n-1) THEN v(n-1) ELSE vn END
     * ```
     *
     * where `ci` is the conjunction of `vi >= COALESCE(vj, vi)` (`<=` for LEAST) for j > i; a literal `vj` is used
     * directly. The arguments must be numbers, character strings, or datetime values, and are unified like the arguments
     * of COALESCE.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_GREATEST_LEAST.html
     */
    private fun extremum(
        greatest: Boolean,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        if (args.isEmpty()) throw UnsupportedConstructException.at(name, "`$fn` with no arguments")
        functions.note(name, "Redshift `$fn` was replaced by a CASE expression that ignores NULL arguments")
        return functions.resolve(args) { exprs, types ->
            val all = Values(args, exprs, types)
            types.forEachIndexed { i, type ->
                val comparable = isNumeric(type) || isText(type) || type.code() in DATETIMES || all.isNull(i)
                if (!comparable) throw UnsupportedConstructException.at(name, "`$fn` with ${FunctionTypes.describe(type, "argument")}")
            }
            val values = unify(name, all)
            val hasChar = types.any { it.code() == PType.CHAR }
            // The comparison keys: CHAR(n) values without the padding, and string literals compared with them without
            // trailing blanks, because Redshift disregards the padding.
            val keys =
                values.indices.map { i ->
                    val string = all.string(i)
                    when {
                        types[i].code() == PType.CHAR && values[i] === exprs[i] -> functions.trimTrailing(values[i])
                        string != null && hasChar -> exprLit(Literal.string(string.trimEnd(' ')))
                        else -> values[i]
                    }
                }
            val op = if (greatest) ">=" else "<="
            val branches =
                (0 until values.size - 1).map { i ->
                    val conditions =
                        (i + 1 until values.size).map { j ->
                            val other = if (args[j].expr is ExprLit && !all.isNull(j)) keys[j] else exprCoalesce(listOf(keys[j], keys[i]))
                            exprOperator(op, keys[i], other)
                        }
                    exprCaseBranch(conditions.reduce<Expr, Expr> { a, b -> exprAnd(a, b) }, values[i])
                }
            if (branches.isEmpty()) values[0] else exprCase(null, branches, values.last())
        }
    }

    /*
     * Comparisons
     */

    /**
     * Returns the operands of the equality of the values [i] and [j] that Redshift evaluates for `NULLIF(a, b)` and the
     * matches of `DECODE`:
     *  - A CHAR(n) value is compared without its padding, and a string literal compared with it without its trailing
     *    blanks.
     *  - A string literal compared with a number, a datetime, or a BOOLEAN value converts to that type; see [literal].
     *  - Values of other different type families are unsupported: Redshift converts one of them implicitly, while the
     *    PartiQL equality of values of different types is false.
     */
    private fun comparands(
        name: Token,
        values: Values,
        i: Int,
        j: Int,
    ): Pair<Expr, Expr> {
        val a = values.exprs[i]
        val b = values.exprs[j]
        val ta = values.types[i]
        val tb = values.types[j]
        if (ta === FunctionTypes.UNRESOLVED || tb === FunctionTypes.UNRESOLVED) {
            val type = if (ta === FunctionTypes.UNRESOLVED) ta else tb
            throw UnsupportedConstructException.at(
                name,
                "`${name.text.uppercase()}` comparison of ${FunctionTypes.describe(type, "value")}",
            )
        }
        return when {
            isDynamic(ta) || isDynamic(tb) || isUnknown(ta) || isUnknown(tb) -> a to b
            isText(ta) && isText(tb) -> {
                if (ta.code() != PType.CHAR && tb.code() != PType.CHAR) return a to b

                fun side(k: Int): Expr {
                    val string = values.string(k)
                    return when {
                        values.types[k].code() == PType.CHAR -> functions.trimTrailing(values.exprs[k])
                        string != null -> exprLit(Literal.string(string.trimEnd(' ')))
                        else -> values.exprs[k]
                    }
                }
                side(i) to side(j)
            }
            values.string(j) != null && !isText(ta) -> a to convert(name, values.string(j)!!, ta).first
            values.string(i) != null && !isText(tb) -> convert(name, values.string(i)!!, tb).first to b
            family(ta) != family(tb) ->
                throw UnsupportedConstructException.at(
                    name,
                    "`${name.text.uppercase()}` comparison of ${FunctionTypes.name(ta)} and ${FunctionTypes.name(tb)} values",
                )
            else -> a to b
        }
    }

    /*
     * Result types
     */

    /**
     * The values of a conditional expression: the operands, their probed translations, and their static types.
     */
    private class Values(
        val operands: List<Operand>,
        val exprs: List<Expr>,
        val types: List<PType>,
    ) {
        /** The value of the operand [i] if it is a string literal. */
        fun string(i: Int): String? {
            val lit = (operands[i].expr as? ExprLit)?.lit ?: return null
            return if (lit.code() == Literal.STRING) lit.stringValue() else null
        }

        /** True if the operand [i] is the NULL literal. */
        fun isNull(i: Int): Boolean = (operands[i].expr as? ExprLit)?.lit?.code() == Literal.NULL

        fun select(indices: List<Int>): Values = Values(indices.map { operands[it] }, indices.map { exprs[it] }, indices.map { types[it] })
    }

    /**
     * Unifies [values], the values that the conditional expression [name] can return, as Redshift unifies them to one
     * result type:
     *  - A string literal next to values of a non-character type converts to that type (`NVL(int_col, '0')` is
     *    `NVL(int_col, 0)`); see [literal].
     *  - When the common PartiQL type is numeric or a datetime type, a value of another type is cast to it, for example
     *    `COALESCE(decimal_col, CAST(0 AS DECIMAL(38, 0)))`. A cast to a DECIMAL type that cannot hold every value of the
     *    source type is unsupported.
     *  - When CHAR(n) values are unified with other character strings, they are trimmed: Redshift removes the padding
     *    when it converts a CHAR(n) value to another string type, and disregards it otherwise.
     *  - Values of different type families (other than SUPER) are unsupported.
     */
    private fun unify(
        name: Token,
        values: Values,
    ): List<Expr> {
        val exprs = values.exprs.toMutableList()
        val types = values.types.toMutableList()
        val strings = exprs.indices.filter { values.string(it) != null }
        val typed = exprs.indices.filter { it !in strings && !values.isNull(it) }
        if (typed.isEmpty()) return exprs
        typed.firstOrNull { types[it] === FunctionTypes.UNRESOLVED }?.let {
            throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` of ${FunctionTypes.describe(types[it], "value")}")
        }
        val first = supertype(typed.map { types[it] })
        when {
            // A value that the planner could not type; planning the translation reports the problem.
            isUnknown(first) -> return exprs
            isDynamic(first) -> {
                if (typed.any { isDynamic(types[it]) || types[it].code() in COLLECTIONS }) return exprs
                val names = typed.map { FunctionTypes.name(types[it]) }.distinct().joinToString(" and ")
                throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` of $names values")
            }
            first.code() in COLLECTIONS -> return exprs
            isText(first) -> {
                val chars = exprs.indices.filter { types[it].code() == PType.CHAR }
                val allChar = first.code() == PType.CHAR && strings.isEmpty()
                // CHAR(n) values of one length keep their padding, as the CHAR(n) result does. Otherwise the padding of
                // the Redshift result is not documented; see [RedshiftFunctions.charBranches].
                if (chars.isNotEmpty() && !(allChar && chars.map { types[it].length }.distinct().size == 1)) {
                    values.operands.first().ctx.parent?.let { functions.charBranches.add(it as ParserRuleContext) }
                }
                if (!allChar) chars.forEach { exprs[it] = functions.trimTrailing(exprs[it]) }
                return exprs
            }
        }
        for (i in strings) {
            val (value, type) = convert(name, values.string(i)!!, first)
            exprs[i] = value
            types[i] = type
        }
        val target = supertype((typed + strings).map { types[it] })
        if (!isNumeric(target) && target.code() !in DATETIMES) return exprs
        for (i in typed + strings) {
            val type = types[i]
            if (type.code() == target.code()) continue
            val targetDigits = FunctionTypes.integerDigits(target)
            val sourceDigits = FunctionTypes.integerDigits(type)
            if (isDecimal(target) && targetDigits != null && sourceDigits != null && targetDigits < sourceDigits) {
                val pair = "${FunctionTypes.name(type)} and ${FunctionTypes.name(target)}"
                throw UnsupportedConstructException.at(
                    name,
                    "`${name.text.uppercase()}` of $pair values: the common type cannot hold all values",
                )
            }
            exprs[i] = exprCast(exprs[i], dataType(target))
        }
        return exprs
    }

    /**
     * Returns the literal and the type for the character string literal [value] that Redshift converts to [type], or
     * reports the conversion as unsupported; see [literal].
     */
    private fun convert(
        name: Token,
        value: String,
        type: PType,
    ): Pair<Expr, PType> =
        literal(value, type) ?: throw UnsupportedConstructException.at(
            name,
            "`${name.text.uppercase()}` with the character string '$value' and a value of type ${FunctionTypes.name(type)}; " +
                "Redshift converts the string implicitly",
        )

    /**
     * Returns the literal of [type] for the character string literal [value], with its static type, or null if the
     * translator does not convert [value]: it converts canonical integers (not `-0`, `05`, or `+5`, because Redshift
     * compares a number with a character string as text), plain decimal numbers that fit the DECIMAL type, dates and
     * timestamps in the ISO format, and the BOOLEAN strings.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Boolean_type.html
     */
    private fun literal(
        value: String,
        type: PType,
    ): Pair<Expr, PType>? {
        val trimmed = value.trim(' ')
        return when {
            isInteger(type) -> {
                if (!CANONICAL_INTEGER.matches(trimmed)) return null
                val n = BigInteger(trimmed)
                val (min, max) = INTEGER_RANGES.getValue(type.code())
                if (n < min || n > max) return null
                val lit = exprLit(Literal.intNum(n))
                if (type.code() == PType.INTEGER) lit to type else exprCast(lit, dataType(type)) to type
            }
            isDecimal(type) -> {
                if (!PLAIN_DECIMAL.matches(trimmed)) return null
                val d = BigDecimal(trimmed)
                if (d.scale() > type.scale || d.precision() - d.scale() > type.precision - type.scale) return null
                exprLit(Literal.exactNum(trimmed)) to PType.decimal(maxOf(d.precision(), d.scale()), d.scale())
            }
            type.code() == PType.DATE ->
                if (DATE.matches(trimmed)) exprLit(Literal.typedString(DataType.DATE(), trimmed)) to PType.date() else null
            type.code() == PType.TIMESTAMP ->
                when {
                    DATE.matches(trimmed) -> exprLit(Literal.typedString(DataType.TIMESTAMP(), "$trimmed 00:00:00")) to type
                    TIMESTAMP.matches(trimmed) -> exprLit(Literal.typedString(DataType.TIMESTAMP(), trimmed)) to type
                    else -> null
                }
            type.code() == PType.BOOL ->
                when (trimmed.lowercase()) {
                    in TRUE_STRINGS -> exprLit(Literal.bool(true)) to type
                    in FALSE_STRINGS -> exprLit(Literal.bool(false)) to type
                    else -> null
                }
            else -> null
        }
    }

    /**
     * The common type of [types], as the PLK planner computes it for CASE and COALESCE (`DynamicTyper`): the types are
     * combined from left to right, and two types of the same family give the wider one.
     */
    private fun supertype(types: List<PType>): PType {
        var s: PType? = null
        for (type in types) {
            val current = s
            s =
                when {
                    current == null -> type
                    isDynamic(current) -> current
                    isDynamic(type) -> type
                    current == type -> current
                    else -> common(current, type) ?: PType.dynamic()
                }
        }
        val result = s ?: return PType.dynamic()
        // When all the types are DECIMAL, the planner takes the largest precision and the largest scale.
        if (isDecimal(result) && types.all { it.code() == PType.DECIMAL }) {
            return PType.decimal(types.maxOf { it.precision }, types.maxOf { it.scale })
        }
        return result
    }

    private fun common(
        a: PType,
        b: PType,
    ): PType? {
        if (a.code() == b.code()) return merge(a, b)
        if (isUnknown(a)) return b
        if (isUnknown(b)) return a
        coerce(a, b.code())?.let { return common(it, b) }
        coerce(b, a.code())?.let { return common(a, it) }
        return null
    }

    private fun merge(
        a: PType,
        b: PType,
    ): PType =
        when (a.code()) {
            PType.DECIMAL, PType.NUMERIC -> {
                val scale = maxOf(a.scale, b.scale)
                val precision = minOf(MAX_PRECISION, maxOf(a.precision - a.scale, b.precision - b.scale) + scale)
                if (a.code() == PType.DECIMAL) PType.decimal(precision, scale) else PType.numeric(precision, scale)
            }
            PType.CHAR -> PType.character(maxOf(a.length, b.length))
            PType.VARCHAR -> PType.varchar(maxOf(a.length, b.length))
            PType.TIME -> PType.time(maxOf(a.precision, b.precision))
            PType.TIMEZ -> PType.timez(maxOf(a.precision, b.precision))
            PType.TIMESTAMP -> PType.timestamp(maxOf(a.precision, b.precision))
            PType.TIMESTAMPZ -> PType.timestampz(maxOf(a.precision, b.precision))
            else -> a
        }

    /** The type of a value of [source] coerced to the type family [target], or null if the planner does not coerce it. */
    private fun coerce(
        source: PType,
        target: Int,
    ): PType? =
        when (source.code()) {
            PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT ->
                when (target) {
                    PType.SMALLINT -> if (source.code() == PType.TINYINT) PType.smallint() else null
                    PType.INTEGER -> if (source.code() == PType.TINYINT || source.code() == PType.SMALLINT) PType.integer() else null
                    PType.BIGINT -> if (source.code() != PType.BIGINT) PType.bigint() else null
                    PType.NUMERIC -> PType.numeric(FunctionTypes.integerDigits(source)!!, 0)
                    PType.DECIMAL -> PType.decimal(FunctionTypes.integerDigits(source)!!, 0)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
            PType.NUMERIC ->
                when (target) {
                    PType.DECIMAL -> PType.decimal(source.precision, source.scale)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
            PType.DECIMAL ->
                when (target) {
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
            PType.REAL -> if (target == PType.DOUBLE) PType.doublePrecision() else null
            PType.CHAR ->
                when (target) {
                    PType.VARCHAR -> PType.varchar(source.length)
                    PType.STRING -> PType.string()
                    else -> null
                }
            PType.VARCHAR -> if (target == PType.STRING) PType.string() else null
            PType.DATE ->
                when (target) {
                    PType.TIMESTAMP -> PType.timestamp(0)
                    PType.TIMESTAMPZ -> PType.timestampz(0)
                    else -> null
                }
            PType.TIME -> if (target == PType.TIMEZ) PType.timez(source.precision) else null
            PType.TIMESTAMP -> if (target == PType.TIMESTAMPZ) PType.timestampz(source.precision) else null
            else -> null
        }

    /** The data type of a CAST to the numeric or datetime type [type]. */
    private fun dataType(type: PType): DataType =
        when (type.code()) {
            PType.TINYINT, PType.SMALLINT -> DataType.SMALLINT()
            PType.INTEGER -> DataType.INTEGER()
            PType.BIGINT -> DataType.BIGINT()
            PType.DECIMAL, PType.NUMERIC -> DataType.DECIMAL(type.precision, type.scale)
            PType.REAL -> DataType.REAL()
            PType.DOUBLE -> DataType.DOUBLE_PRECISION()
            PType.DATE -> DataType.DATE()
            PType.TIME -> DataType.TIME(type.precision)
            PType.TIMEZ -> DataType.TIME_WITH_TIME_ZONE(type.precision)
            PType.TIMESTAMP -> DataType.TIMESTAMP(type.precision)
            PType.TIMESTAMPZ -> DataType.TIMESTAMP_WITH_TIME_ZONE(type.precision)
            else -> error("Not a numeric or datetime type: $type")
        }

    /** The type family of [type], for comparisons. */
    private fun family(type: PType): String =
        when {
            isNumeric(type) -> "number"
            isText(type) -> "text"
            type.code() == PType.DATE || type.code() == PType.TIMESTAMP || type.code() == PType.TIMESTAMPZ -> "timestamp"
            type.code() == PType.TIME || type.code() == PType.TIMEZ -> "time"
            else -> FunctionTypes.name(type)
        }

    private companion object {
        const val MAX_PRECISION = 38

        val DATETIMES = setOf(PType.DATE, PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ)
        val COLLECTIONS = setOf(PType.ARRAY, PType.BAG, PType.ROW, PType.STRUCT)

        val INTEGER_RANGES: Map<Int, Pair<BigInteger, BigInteger>> =
            mapOf(
                PType.TINYINT to range(Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong()),
                PType.SMALLINT to range(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()),
                PType.INTEGER to range(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()),
                PType.BIGINT to range(Long.MIN_VALUE, Long.MAX_VALUE),
            )

        fun range(
            min: Long,
            max: Long,
        ): Pair<BigInteger, BigInteger> = BigInteger.valueOf(min) to BigInteger.valueOf(max)

        val CANONICAL_INTEGER = Regex("0|-?[1-9][0-9]*")
        val PLAIN_DECIMAL = Regex("-?[0-9]+(\\.[0-9]+)?")
        val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        val TIMESTAMP = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d{1,6})?""")

        val TRUE_STRINGS = setOf("t", "true", "y", "yes", "1")
        val FALSE_STRINGS = setOf("f", "false", "n", "no", "0")
    }
}
