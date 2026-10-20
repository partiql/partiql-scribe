package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprBoolTest
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNot
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.expr.TruthValue
import org.partiql.spi.types.PType
import java.math.BigDecimal
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates Redshift comparison conditions, IN, BETWEEN, the IS tests, and CASE; see [RedshiftExpressions].
 *
 * Redshift converts the operands of a comparison implicitly, and PartiQL does not: in PartiQL, a DATE is never equal
 * to a character string, and a DOUBLE PRECISION value is compared with a DECIMAL value by its exact binary value. The
 * rules here use the static operand types (see [OperandTypes]) to write the conversions that Redshift makes:
 *  - A character string literal that is compared with a value of another type is converted to that type at
 *    translation time (`d = '2020-01-01'` becomes `d = DATE '2020-01-01'`), when Redshift's conversion is known to be
 *    exact; otherwise the comparison is unsupported.
 *  - CHAR(n) operands are trimmed, because Redshift ignores their padding. VARCHAR values are compared as they are
 *    (a documented deviation: Redshift also ignores their trailing blanks).
 *  - REAL and DOUBLE PRECISION values are compared with DECIMAL values, and REAL values with integers, in DOUBLE
 *    PRECISION, as in Redshift.
 *  - Other comparisons of values of different type families are unsupported. SUPER values are compared as they are.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
 */
internal class RedshiftConditions(
    private val builder: RedshiftAstBuilder,
    private val exprs: RedshiftExpressions,
    private val datetime: RedshiftDatetime,
    private val types: OperandTypes,
) {
    /** A comparison operator. */
    internal enum class Cmp(val partiql: String) {
        EQ("="),
        NE("<>"),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
        ;

        val isEquality: Boolean get() = this == EQ || this == NE

        companion object {
            fun of(ctx: P.ComparisonOperatorContext): Cmp =
                when {
                    ctx.EQ() != null -> EQ
                    ctx.NEQ() != null -> NE
                    ctx.LT() != null -> LT
                    ctx.LT_EQ() != null -> LE
                    ctx.GT() != null -> GT
                    else -> GE
                }
        }
    }

    /** Where a character string literal is converted to the type of another operand. */
    internal enum class Use {
        /** `=`, `<>`, IN, and the WHEN values of a simple CASE. */
        EQUALITY,

        /** `<`, `<=`, `>`, `>=`, and BETWEEN. */
        ORDERING,

        /** A THEN or ELSE result of CASE next to results of another type. */
        RESULT,
    }

    /*
     * Comparisons
     */

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_comparison_condition.html
     */
    fun comparison(ctx: P.PredicateComparisonContext): Expr =
        compare(ctx.op.start, Cmp.of(ctx.op), Operand(ctx.lhs, exprs.valueExpr(ctx.lhs)), Operand(ctx.rhs, exprs.valueExpr(ctx.rhs)))

    /** Translates `lhs op rhs` with the implicit conversions of Redshift. */
    fun compare(
        token: Token,
        op: Cmp,
        lhs: Operand,
        rhs: Operand,
    ): Expr =
        types.resolveTyped(listOf(lhs, rhs)) { (a, b) ->
            val (x, y) = convertPair(token, op, a, b)
            exprOperator(op.partiql, x, y)
        }

    /**
     * Returns the operands of the comparison `a op b` with the conversions that Redshift makes implicitly, or throws
     * [UnsupportedConstructException] when the translator does not know the conversion.
     */
    private fun convertPair(
        token: Token,
        op: Cmp,
        a: TypedOperand,
        b: TypedOperand,
    ): Pair<Expr, Expr> {
        if (a.isNullLiteral || b.isNullLiteral) return a.expr to b.expr
        if (a.untyped || b.untyped) {
            // The type of an operand with an aggregate call is not known; see [resolveTyped].
            if (a.isStringLiteral || b.isStringLiteral) unsupported(token, "comparison of an aggregate expression with a character string")
            return a.expr to b.expr
        }
        if (a.family == TypeFamily.SUPER || b.family == TypeFamily.SUPER) return a.expr to b.expr
        if (a.isStringLiteral && b.isStringLiteral) return a.expr to b.expr
        if (a.isStringLiteral) return convertWithStringLiteral(token, op, b, a).let { (y, x) -> x to y }
        if (b.isStringLiteral) return convertWithStringLiteral(token, op, a, b)
        val fa = a.family
        val fb = b.family
        if (fa == TypeFamily.UNKNOWN || fb == TypeFamily.UNKNOWN) return a.expr to b.expr
        if (fa.isNumeric && fb.isNumeric) return promoteNumeric(a, b)
        if (fa == TypeFamily.BOOLEAN && b.isNumericLiteral) return a.expr to booleanOfInteger(token, op, b)
        if (fb == TypeFamily.BOOLEAN && a.isNumericLiteral) return booleanOfInteger(token, op, a) to b.expr
        if (fa != fb) throw UnsupportedConstructException.at(token, crossFamilyMessage(a.type, b.type))
        return when (fa) {
            TypeFamily.TEXT -> trimChar(a) to trimChar(b)
            TypeFamily.INTERVAL -> {
                if (!op.isEquality) unsupported(token, "comparison `${op.partiql}` of INTERVAL values")
                if (a.type.code() != b.type.code()) unsupported(token, "comparison of year-month and day-time INTERVAL values")
                a.expr to b.expr
            }
            else -> a.expr to b.expr
        }
    }

    private fun crossFamilyMessage(
        a: PType,
        b: PType,
    ): String {
        val fa = TypeFamily.of(a)
        val fb = TypeFamily.of(b)
        return if ((fa.isNumeric && fb == TypeFamily.TEXT) || (fb.isNumeric && fa == TypeFamily.TEXT)) {
            "comparison of a number with a character string"
        } else {
            "comparison of ${typeName(a)} with ${typeName(b)}"
        }
    }

    /**
     * The typed operand [t] and the character string literal [s] of a comparison. Redshift converts the literal to the
     * type of [t] ("if the string is an appropriate literal value; any leading or trailing spaces are ignored"), and
     * converts a number other than DECIMAL to a character string. The translation converts the literal when both
     * readings give the same result.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */
    private fun convertWithStringLiteral(
        token: Token,
        op: Cmp,
        t: TypedOperand,
        s: TypedOperand,
    ): Pair<Expr, Expr> {
        val value = s.literal!!.stringValue()
        return when (t.family) {
            TypeFamily.TEXT ->
                if (t.type.code() == PType.CHAR) trimChar(t) to exprLit(Literal.string(value.trimEnd(' '))) else t.expr to s.expr
            TypeFamily.UNKNOWN -> t.expr to s.expr
            else -> t.expr to convertStringLiteral(s.ctx.start, value, t.type, if (op.isEquality) Use.EQUALITY else Use.ORDERING)
        }
    }

    /**
     * Converts the character string literal [value] to the PartiQL literal of [type], as Redshift converts it next to
     * a value of [type], or throws [UnsupportedConstructException].
     */
    fun convertStringLiteral(
        token: Token,
        value: String,
        type: PType,
        use: Use,
    ): Expr {
        val name = typeName(type)
        val failure =
            "comparison of $name with the character string '$value': Redshift converts the string implicitly; write a typed literal"
        val converted: Expr =
            when (TypeFamily.of(type)) {
                TypeFamily.DATETIME -> stringToDatetime(token, value, type) ?: unsupported(token, failure)
                TypeFamily.DECIMAL -> {
                    val text = value.trim(' ')
                    val number = if (DECIMAL_TEXT.matches(text)) BigDecimal(text) else null
                    val fits =
                        number != null &&
                            number.scale() <= type.scale &&
                            number.precision() - number.scale() <= type.precision - type.scale
                    if (!fits) unsupported(token, failure)
                    exprLit(Literal.exactNum(number.toPlainString()))
                }
                TypeFamily.INTEGER ->
                    when (use) {
                        Use.ORDERING -> unsupported(token, "ordering comparison of an integer with a character string '$value'")
                        // Redshift converts the integer to a character string, or the string to an integer: the
                        // results are the same only for the canonical text of an integer.
                        Use.EQUALITY ->
                            if (CANONICAL_INTEGER.matches(value)) exprLit(Literal.intNum(value)) else unsupported(token, failure)
                        // A CASE result: the string is converted to the integer type.
                        Use.RESULT -> {
                            val text = value.trim(*WHITESPACE)
                            if (!INTEGER_TEXT.matches(text)) unsupported(token, failure)
                            exprLit(Literal.intNum(text.removePrefix("+").toBigInteger().toString()))
                        }
                    }
                TypeFamily.BOOLEAN ->
                    when (value.trim(*WHITESPACE).lowercase()) {
                        in TRUE_TEXT -> exprLit(Literal.bool(true))
                        in FALSE_TEXT -> exprLit(Literal.bool(false))
                        else -> unsupported(token, failure)
                    }
                else -> unsupported(token, failure)
            }
        builder.note(token, "Redshift converts the character string '$value' to $name; it was replaced by a $name literal")
        return converted
    }

    /**
     * Converts a character string literal to a datetime literal with [datetimeLiteral]. A string with a time of day
     * other than midnight is not converted to a DATE: the Redshift result is not documented.
     */
    private fun stringToDatetime(
        token: Token,
        value: String,
        type: PType,
    ): Expr? {
        val text = value.trim(' ')
        if (type.code() == PType.DATE) {
            val timestamp = (datetime.stringToDatetime(text, PType.timestamp(DATETIME_PRECISION)) as? ExprLit)?.lit?.stringValue()
            if (timestamp != null && !MIDNIGHT.matches(timestamp)) {
                unsupported(token, "comparison of DATE with the character string '$value', which has a time of day")
            }
        }
        return datetimeLiteral(datetime, text, type)
    }

    /**
     * Redshift converts an integer to BOOLEAN in a comparison with a BOOLEAN value: 0 is false and 1 is true.
     */
    private fun booleanOfInteger(
        token: Token,
        op: Cmp,
        n: TypedOperand,
    ): Expr {
        val value = n.literal!!.takeIf { it.code() == Literal.INT_NUM }?.numberValue()
        if (!op.isEquality || (value != "0" && value != "1")) unsupported(token, "comparison of BOOLEAN with ${n.ctx.text}")
        return exprLit(Literal.bool(value == "1"))
    }

    /**
     * Redshift compares a REAL or DOUBLE PRECISION value with a DECIMAL value, and a REAL value with an integer, in
     * DOUBLE PRECISION. PartiQL compares a floating-point value with a DECIMAL value by its exact binary value, and a REAL
     * value with an integer in REAL, so the translation casts the operands that are not DOUBLE PRECISION. A numeric
     * literal whose value the floating-point type of the other operand holds exactly needs no cast: both comparisons are
     * exact then.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_numeric_computations201.html
     */
    private fun promoteNumeric(
        a: TypedOperand,
        b: TypedOperand,
    ): Pair<Expr, Expr> {
        if (!needsDouble(a.type, b.type) || isExactIn(a, b.type) || isExactIn(b, a.type)) return a.expr to b.expr
        return toDouble(a) to toDouble(b)
    }

    private fun toDouble(operand: TypedOperand): Expr =
        if (operand.type.code() == PType.DOUBLE) operand.expr else exprCast(operand.expr, DataType.DOUBLE_PRECISION())

    /** True if [operand] is a numeric literal whose value the REAL or DOUBLE PRECISION [type] holds exactly. */
    private fun isExactIn(
        operand: TypedOperand,
        type: PType,
    ): Boolean {
        val lit = operand.literal ?: return false
        val value =
            when (lit.code()) {
                Literal.INT_NUM -> BigDecimal(lit.numberValue())
                Literal.EXACT_NUM -> lit.bigDecimalValue()
                else -> return false
            }
        val binary =
            when (type.code()) {
                PType.REAL -> value.toFloat().toDouble()
                PType.DOUBLE -> value.toDouble()
                else -> return false
            }
        return binary.isFinite() && BigDecimal(binary).compareTo(value) == 0
    }

    /** Trims the padding of a CHAR(n) operand, which Redshift ignores in comparisons. */
    private fun trimChar(operand: TypedOperand): Expr =
        if (operand.type.code() == PType.CHAR) exprTrim(operand.expr, null, TrimSpec.TRAILING()) else operand.expr

    /*
     * IN
     */

    /**
     * `x [NOT] IN (e1, ..., en)` is `x = e1 OR ... OR x = en`; the values get the conversions of `=`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_in_condition.html
     */
    fun inList(ctx: P.PredicateInListContext): Expr {
        val not = ctx.NOT() != null
        val lhs = Operand(ctx.lhs, exprs.valueExpr(ctx.lhs))
        val values = ctx.expr().map { Operand(it, exprs.expr(it)) }
        values.firstOrNull { it.expr is ExprQuerySet }?.let {
            throw UnsupportedConstructException.at(it.ctx, "subquery as a value of an IN list")
        }
        val token = ctx.IN().symbol
        return types.resolveTyped(listOf(lhs) + values) { typed ->
            val x = typed[0]
            val pairs = typed.drop(1).map { convertPair(token, Cmp.EQ, x, it) }
            if (pairs.all { it.first == pairs[0].first }) {
                exprInCollection(pairs[0].first, exprArray(pairs.map { it.second }), not)
            } else {
                builder.note(
                    token,
                    "Redshift `IN` list was replaced by comparisons joined with OR, because its values need different conversions",
                )
                val any = pairs.map<Pair<Expr, Expr>, Expr> { exprOperator("=", it.first, it.second) }.reduce { l, r -> exprOr(l, r) }
                if (not) exprNot(any) else any
            }
        }
    }

    /**
     * `x [NOT] IN (subquery)`. The comparison conversions apply to `x`; the translation does not change the subquery, so
     * a conversion of the subquery values is unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_in_condition.html
     */
    fun inQuery(
        token: Token,
        lhs: P.ValueExprContext,
        query: P.QueryContext,
        not: Boolean,
    ): Expr {
        val x = Operand(lhs, exprs.valueExpr(lhs))
        val subquery = builder.exprQuerySet(query)
        // The probe of a subquery argument has the type of the subquery column.
        return types.resolveTyped(listOf(x, Operand(query, subquery))) { (a, q) ->
            // In a probe pass the subquery is a function argument, so the IN predicate is not built.
            if (types.probing) return@resolveTyped types.pending(listOf(a.expr, q.expr))
            val columnType = columnType(q.type)
            val column = TypedOperand(q.ctx, COLUMN, columnType)
            val (converted, columnExpr) = convertPair(token, Cmp.EQ, a, column)
            if (columnExpr !== COLUMN) {
                unsupported(token, "IN subquery whose values Redshift converts (${typeName(a.type)} with ${typeName(columnType)})")
            }
            exprInCollection(converted, subquery, not)
        }
    }

    /** The type of the column of a subquery: the planner types a subquery argument as its column, or as its rows. */
    private fun columnType(type: PType): PType {
        if (type.code() != PType.BAG && type.code() != PType.ARRAY) return type
        val row = type.typeParameter
        if (row.code() != PType.ROW) return row
        return row.fields.singleOrNull()?.type ?: PType.dynamic()
    }

    /**
     * `x = ANY (subquery)` and `x = SOME (subquery)` are `x IN (subquery)`; `x <> ALL (subquery)` is
     * `x NOT IN (subquery)`. Redshift supports no other quantified comparison.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_comparison_condition.html
     */
    fun quantified(ctx: P.PredicateQuantifiedContext): Expr {
        val op = Cmp.of(ctx.op)
        val quantifier = ctx.quantifier
        val construct = "${ctx.op.text} ${quantifier.text.uppercase()}"
        val not =
            when {
                op == Cmp.EQ && quantifier.type != P.ALL -> false
                op == Cmp.NE && quantifier.type == P.ALL -> true
                else ->
                    unsupported(
                        ctx.op.start,
                        "quantified comparison `$construct`; Redshift supports only `= ANY`, `= SOME`, and `<> ALL`",
                    )
            }
        builder.note(ctx.op.start, "Redshift `$construct (subquery)` was replaced by PartiQL `${if (not) "NOT IN" else "IN"} (subquery)`")
        return inQuery(ctx.op.start, ctx.lhs, ctx.query(), not)
    }

    /**
     * `(a, b, ...) [NOT] IN ((v1, w1, ...), ...)` is `(a = v1 AND b = w1 AND ...) OR ...`. PartiQL has a row-value IN,
     * but it reads a NULL component as unequal instead of unknown.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_in_condition.html
     */
    fun rowInList(ctx: P.PredicateRowInListContext): Expr {
        val lhs = ctx.row.expr()
        val rows = ctx.rowValue().drop(1)
        rows.firstOrNull { it.expr().size != lhs.size }?.let {
            throw RedshiftSyntaxException(
                "row value with ${it.expr().size} values in IN; expected ${lhs.size}",
                it.start.line,
                it.start.charPositionInLine + 1,
            )
        }
        val token = ctx.IN().symbol
        builder.note(ctx.row.start, "Redshift row-value `IN` was replaced by comparisons joined with AND and OR")
        val any =
            rows.map<P.RowValueContext, Expr> { row ->
                lhs.indices.map { i ->
                    compare(token, Cmp.EQ, Operand(lhs[i], exprs.expr(lhs[i])), Operand(row.expr(i), exprs.expr(row.expr(i))))
                }.reduce { l, r -> exprAnd(l, r) }
            }.reduce { l, r -> exprOr(l, r) }
        return if (ctx.NOT() != null) exprNot(any) else any
    }

    /*
     * BETWEEN
     */

    /**
     * `x [NOT] BETWEEN a AND b`. PartiQL BETWEEN returns NULL when a bound is NULL, while Redshift evaluates
     * `x >= a AND x <= b`, which is FALSE for `5 BETWEEN NULL AND 3`. The translation keeps BETWEEN when both bounds are
     * literals other than NULL, and otherwise writes the comparisons.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_range_condition.html
     */
    fun between(ctx: P.PredicateBetweenContext): Expr {
        ctx.symmetry?.let { unsupported(it, "BETWEEN ${it.text.uppercase()}") }
        val not = ctx.NOT() != null
        val token = ctx.BETWEEN().symbol
        val operands =
            listOf(ctx.lhs, ctx.lower, ctx.upper).map { Operand(it, exprs.valueExpr(it)) }
        return types.resolveTyped(operands) { (x, lower, upper) ->
            val (x1, lo) = convertPair(token, Cmp.GE, x, lower)
            val (x2, hi) = convertPair(token, Cmp.LE, x, upper)
            val literalBounds = listOf(lower, upper).all { it.literal != null && !it.isNullLiteral }
            if (literalBounds && x1 == x2) {
                exprBetween(x1, lo, hi, not)
            } else {
                builder.note(
                    token,
                    "Redshift `BETWEEN` was replaced by `x >= a AND x <= b`, because PartiQL BETWEEN is NULL when a bound is NULL",
                )
                val range = exprAnd(exprOperator(">=", x1, lo), exprOperator("<=", x2, hi))
                if (not) exprNot(range) else range
            }
        }
    }

    /*
     * IS tests
     */

    /**
     * `x IS [NOT] TRUE | FALSE | UNKNOWN`. UNKNOWN is the NULL test for any type. Redshift also accepts a number for
     * TRUE and FALSE (non-zero is TRUE); the translation supports BOOLEAN values only.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_comparison_condition.html
     */
    fun truth(ctx: P.PredicateTruthContext): Expr {
        val not = ctx.NOT() != null
        val value = Operand(ctx.lhs, exprs.valueExpr(ctx.lhs))
        if (ctx.truth.type == P.UNKNOWN) {
            builder.note(
                ctx.truth,
                "Redshift `IS ${if (not) "NOT " else ""}UNKNOWN` was replaced by PartiQL `IS ${if (not) "NOT " else ""}NULL`",
            )
            return exprNullPredicate(value.expr, not)
        }
        val truth = if (ctx.truth.type == P.TRUE) TruthValue.TRUE() else TruthValue.FALSE()
        if (isBooleanForm(ctx.lhs)) return exprBoolTest(value.expr, not, truth)
        return types.resolveTyped(listOf(value)) { (x) ->
            if (x.family != TypeFamily.BOOLEAN && x.family != TypeFamily.UNKNOWN) {
                unsupported(ctx.IS().symbol, "`IS ${ctx.truth.text.uppercase()}` on a non-boolean value")
            }
            exprBoolTest(x.expr, not, truth)
        }
    }

    /**
     * `IS [NOT] DISTINCT FROM` is not supported by Redshift.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_unsupported-postgresql-functions.html
     */
    fun distinct(ctx: P.PredicateDistinctContext): Nothing = unsupported(ctx.IS().symbol, "IS DISTINCT FROM (not supported by Redshift)")

    /*
     * CASE
     */

    /**
     * `CASE [x] WHEN ... THEN ... [ELSE ...] END`.
     *  - A simple CASE becomes a searched CASE whose conditions are `x = value` with the comparison conversions; the
     *    PartiQL planner does the same.
     *  - A branch whose condition is the NULL literal is removed: it is never taken, and the PartiQL planner fails on a
     *    CASE without a branch.
     *  - The results are cast to the type of the CASE when their numeric or datetime types differ, because PartiQL
     *    computes that type but does not convert the results.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CASE_function.html
     */
    fun case(ctx: P.AtomCaseContext): Expr {
        val operand = ctx.operand?.let { Operand(it, exprs.expr(it)) }
        val whens = ctx.caseWhen()
        val conditions =
            whens.map { w ->
                if (operand != null) {
                    compare(w.WHEN().symbol, Cmp.EQ, operand, Operand(w.condition, exprs.expr(w.condition)))
                } else {
                    exprs.condition(w.condition)
                }
            }
        val keep = whens.indices.filter { operand != null || !isNullLiteral(conditions[it]) }
        whens.indices.firstOrNull { it !in keep }?.let {
            builder.note(whens[it].WHEN().symbol, "Redshift `WHEN NULL` branch was removed; its condition is never true")
        }
        val elseResult = ctx.otherwise?.let { Operand(it, exprs.expr(it)) }
        val results = whens.map { Operand(it.result, exprs.expr(it.result)) } + listOfNotNull(elseResult)
        return types.resolveTyped(results) { typed ->
            val target = resultType(ctx.CASE().symbol, typed)
            val converted = convertResults(typed, target)
            val branches = keep.map { exprCaseBranch(conditions[it], converted[it]) }
            val otherwise = if (ctx.otherwise != null) converted.last() else null
            // The type of the CASE, for a CASE whose branches are all removed.
            val type = target ?: typed.filter { !it.isNullLiteral && !it.isStringLiteral }.map { it.type }.distinct().singleOrNull()
            when {
                branches.isNotEmpty() -> exprCase(null, branches, otherwise)
                otherwise != null -> otherwise
                type != null && dataTypeOf(type) != null -> castTo(exprLit(Literal.nul()), type)
                else -> exprLit(Literal.nul())
            }
        }
    }

    private fun isNullLiteral(expr: Expr): Boolean = expr is ExprLit && expr.lit.code() == Literal.NULL

    /**
     * The type of a CASE, when its results need a conversion, or null. Redshift converts the results to one type: the
     * widest integer type, a DECIMAL that holds the integer digits and the scale of every result, DOUBLE PRECISION,
     * TIMESTAMP, TIMESTAMPTZ, or TIMETZ. A character string literal result is converted to the type of the other
     * results.
     */
    private fun resultType(
        token: Token,
        results: List<TypedOperand>,
    ): PType? {
        val typed = results.filter { !it.isNullLiteral && !it.isStringLiteral && it.family != TypeFamily.UNKNOWN }
        if (typed.isEmpty() || typed.any { it.family == TypeFamily.SUPER || it.untyped }) return null
        val codes = typed.map { it.type.code() }.toSet()
        val families = typed.map { it.family }.toSet()
        val common =
            when {
                families.all { it.isNumeric } -> numericResultType(token, typed.map { it.type })
                families == setOf(TypeFamily.DATETIME) ->
                    when {
                        codes.size == 1 -> null
                        codes.all { it == PType.DATE || it == PType.TIMESTAMP } -> PType.timestamp(DATETIME_PRECISION)
                        codes.all { it == PType.DATE || it == PType.TIMESTAMP || it == PType.TIMESTAMPZ } ->
                            PType.timestampz(
                                DATETIME_PRECISION,
                            )
                        codes.all { it == PType.TIME || it == PType.TIMEZ } -> PType.timez(DATETIME_PRECISION)
                        else ->
                            unsupported(
                                token,
                                "CASE with results of types ${typed.map { typeName(it.type) }.distinct().joinToString()}",
                            )
                    }
                families.size == 1 -> null
                else -> unsupported(token, "CASE with results of types ${typed.map { typeName(it.type) }.distinct().joinToString()}")
            }
        val literals = results.any { it.isStringLiteral }
        return common ?: typed.first().type.takeIf { literals && typed.first().family != TypeFamily.TEXT }
    }

    private fun numericResultType(
        token: Token,
        types: List<PType>,
    ): PType? {
        val codes = types.map { it.code() }.toSet()
        if (PType.REAL in codes || PType.DOUBLE in codes) {
            if (codes.size == 1) return null
            if (PType.REAL !in codes || codes == setOf(PType.REAL, PType.DOUBLE)) return PType.doublePrecision()
            unsupported(token, "CASE with REAL and ${types.map { typeName(it) }.filter { it != "REAL" }.distinct().joinToString()} results")
        }
        if (codes.none { it == PType.DECIMAL || it == PType.NUMERIC }) {
            if (codes.size == 1) return null
            return PType.of(listOf(PType.BIGINT, PType.INTEGER, PType.SMALLINT, PType.TINYINT).first { it in codes })
        }
        val digits = types.maxOf { integerDigits(it) }
        val scale = types.maxOf { scaleOf(it) }
        if (digits + scale > MAX_DECIMAL_PRECISION) {
            unsupported(token, "CASE with results of types ${types.distinct().joinToString()}: no DECIMAL type holds every value")
        }
        return PType.decimal(digits + scale, scale)
    }

    /**
     * Converts the CASE results to [target]: casts a value of another type, and converts a character string literal.
     * A DECIMAL result of another precision or scale is cast only if no result has the target type: PartiQL types the
     * CASE with the target type when one result has it.
     */
    private fun convertResults(
        results: List<TypedOperand>,
        target: PType?,
    ): List<Expr> {
        if (target == null) return results.map { it.expr }

        fun exact(t: PType) =
            t.code() == target.code() && (target.code() != PType.DECIMAL || (t.precision == target.precision && t.scale == target.scale))
        val hasTarget = results.any { !it.isStringLiteral && !it.isNullLiteral && (exact(it.type) || it.type.code() != target.code()) }
        return results.map { result ->
            val type = result.type
            when {
                result.isNullLiteral || result.family == TypeFamily.UNKNOWN -> result.expr
                result.isStringLiteral -> convertStringLiteral(result.ctx.start, result.literal!!.stringValue(), target, Use.RESULT)
                exact(type) -> result.expr
                type.code() != target.code() -> castTo(result.expr, target)
                hasTarget -> result.expr
                else -> castTo(result.expr, target)
            }
        }
    }

    internal companion object {
        /**
         * The datetime literal of [type] for the character string [text], or null: [RedshiftDatetime.stringToDatetime],
         * and for the date-only text of a TIMESTAMP the midnight of the date, and for a DATE the date of a timestamp.
         */
        fun datetimeLiteral(
            datetime: RedshiftDatetime,
            text: String,
            type: PType,
        ): Expr? {
            datetime.stringToDatetime(text, type)?.let { return it }
            return when (type.code()) {
                PType.TIMESTAMP -> {
                    val date = (datetime.stringToDatetime(text, PType.date()) as? ExprLit)?.lit?.stringValue() ?: return null
                    exprLit(Literal.typedString(DataType.TIMESTAMP(), "$date 00:00:00"))
                }
                PType.DATE -> {
                    val timestamp = (datetime.stringToDatetime(text, PType.timestamp(DATETIME_PRECISION)) as? ExprLit)?.lit?.stringValue()
                    if (timestamp == null || !DATE_PREFIX.containsMatchIn(timestamp)) return null
                    exprLit(Literal.typedString(DataType.DATE(), timestamp.substring(0, DATE_LENGTH)))
                }
                else -> null
            }
        }

        private val DATE_PREFIX = Regex("""^\d{4}-\d{2}-\d{2} """)

        /** A placeholder for the column of an IN subquery, to see whether a comparison converts it. */
        private val COLUMN: Expr = exprVarRef(Identifier.delimited("\$column"), false)

        /** An integer as Redshift prints it, so that the text comparison and the integer comparison agree. */
        val CANONICAL_INTEGER = Regex("0|-?[1-9][0-9]*")

        /** The text of an integer that Redshift converts to an integer type. */
        val INTEGER_TEXT = Regex("[+-]?[0-9]+")

        val DECIMAL_TEXT = Regex("-?[0-9]+(\\.[0-9]+)?")

        val MIDNIGHT = Regex("""\d{4}-\d{2}-\d{2} 00:00:00(\.0+)?""")

        const val DATE_LENGTH = 10

        /** The white space that Redshift ignores around a number or a boolean value. */
        val WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')

        /** https://docs.aws.amazon.com/redshift/latest/dg/r_Boolean_type.html */
        val TRUE_TEXT = setOf("t", "true", "y", "yes", "1")
        val FALSE_TEXT = setOf("f", "false", "n", "no", "0")

        /**
         * Redshift computes REAL with a non-REAL number, and DOUBLE PRECISION with DECIMAL, in DOUBLE PRECISION.
         */
        fun needsDouble(
            a: PType,
            b: PType,
        ): Boolean {
            val ca = a.code()
            val cb = b.code()
            val decimal = setOf(PType.DECIMAL, PType.NUMERIC)
            return (ca == PType.REAL && cb != PType.REAL && cb != PType.DOUBLE) ||
                (cb == PType.REAL && ca != PType.REAL && ca != PType.DOUBLE) ||
                (ca == PType.DOUBLE && cb in decimal) ||
                (cb == PType.DOUBLE && ca in decimal)
        }
    }
}
