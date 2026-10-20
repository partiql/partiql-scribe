package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.DataType
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.math.BigInteger
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * An operand of a type-dependent rule with its static type; see [resolveTyped].
 *
 * @property expr The translation of the operand. In a probe pass, an operand that is not a literal is wrapped in a probe
 * call, so a rule must build its result from this expression.
 */
internal class TypedOperand(
    val ctx: ParserRuleContext,
    val expr: Expr,
    val type: PType,
    /** True if the static type is not known: the operand contains an aggregate call; see [resolveTyped]. */
    val untyped: Boolean = false,
    /** True if [expr] is wrapped in a probe call in a probe pass. */
    val probed: Boolean = false,
) {
    /** The literal, if the operand is a literal. Literals are not probed, so [expr] is the literal itself. */
    val literal: Literal? = (expr as? ExprLit)?.lit

    val isNullLiteral: Boolean get() = literal?.code() == Literal.NULL

    /** A character string literal `'...'`, which Redshift converts to the type of the other operand. */
    val isStringLiteral: Boolean get() = literal?.code() == Literal.STRING

    val isNumericLiteral: Boolean get() = literal?.code() in NUMERIC_LITERALS

    val family: TypeFamily get() = if (untyped) TypeFamily.UNTYPED else TypeFamily.of(type)

    private companion object {
        val NUMERIC_LITERALS = setOf(Literal.INT_NUM, Literal.EXACT_NUM, Literal.APPROX_NUM)
    }
}

/**
 * The type families of the comparison and conversion rules. Redshift converts values implicitly within a family, and
 * between some families; see https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html.
 */
internal enum class TypeFamily {
    /** The type of the NULL literal. */
    UNKNOWN,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    FLOAT,
    TEXT,
    DATETIME,
    INTERVAL,

    /** A SUPER value: the dynamic type, or a collection or structure type of the catalog. */
    SUPER,

    /** An operand whose static type is not known; see [TypedOperand.untyped]. */
    UNTYPED,
    OTHER,
    ;

    val isNumeric: Boolean get() = this == INTEGER || this == DECIMAL || this == FLOAT

    companion object {
        fun of(type: PType): TypeFamily =
            when (type.code()) {
                PType.UNKNOWN -> UNKNOWN
                PType.BOOL -> BOOLEAN
                PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT -> INTEGER
                PType.DECIMAL, PType.NUMERIC -> DECIMAL
                PType.REAL, PType.DOUBLE -> FLOAT
                PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB -> TEXT
                PType.DATE, PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ -> DATETIME
                PType.INTERVAL_YM, PType.INTERVAL_DT -> INTERVAL
                PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.STRUCT, PType.ROW, PType.VARIANT -> SUPER
                else -> OTHER
            }
    }
}

/**
 * Resolves the static types of [operands] and translates them with [rule], as [OperandTypes.resolve] does, with these
 * differences:
 *  - Literal operands are not probed: their types follow from the literal (see [literalType]), so that a long IN list
 *    of literals needs no probes.
 *  - Operands that contain an aggregate call are not probed: the PartiQL planner does not find an aggregate call in
 *    the argument of a function call, such as the probe. The type of COUNT, SUM, AVG, MIN, and MAX follows from the
 *    type of the argument (see [aggregateType]); another operand with an aggregate call is [TypedOperand.untyped].
 *  - In a probe pass, an operand of the dynamic type gives the placeholder too, because its type can be the type of a
 *    placeholder of the last pass, and an [UnsupportedConstructException] from [rule] gives the placeholder, because an
 *    operand type can still change; the final pass reports it. The placeholder keeps the probes of the operands even
 *    where the translation would not plan, and the type of a translation with a dynamic operand is dynamic too.
 *  - The placeholder accepts any number of operands.
 */
internal fun OperandTypes.resolveTyped(
    operands: List<Operand>,
    rule: (List<TypedOperand>) -> Expr,
): Expr {
    var known = true
    val typed =
        operands.map { operand ->
            val lit = (operand.expr as? ExprLit)?.lit
            when {
                // The planner types an INTERVAL literal.
                lit != null && literalType(lit).code() != PType.DYNAMIC -> TypedOperand(operand.ctx, operand.expr, literalType(lit))
                // A select-list alias (`s` in `SELECT SUM(b) AS s, s * 2`) can stand for an aggregate.
                containsAggregate(operand.ctx) || RedshiftQueryBlocks.containsAggregateOrWindow(operand.expr) -> {
                    val type = aggregateType(operand.ctx)
                    // Redshift computes REAL with another number in DOUBLE PRECISION, which needs the type.
                    if (type == null && !probing && aggregatesReal(operand.ctx)) {
                        throw UnsupportedConstructException.at(
                            operand.ctx,
                            "operator on an expression with an aggregate of a REAL value",
                        )
                    }
                    TypedOperand(operand.ctx, operand.expr, type ?: PType.dynamic(), untyped = type == null)
                }
                else -> {
                    val type = typeOf(operand.ctx)
                    if (type == null) known = false
                    TypedOperand(operand.ctx, operand(operand.ctx, operand.expr), type ?: PType.dynamic(), probed = true)
                }
            }
        }
    if (probing && (!known || typed.any { it.probed && it.type.code() == PType.DYNAMIC })) return placeholder(typed)
    return try {
        typed.firstOrNull { isStale(it) }?.let {
            throw UnsupportedConstructException.at(it.ctx, "expression nested too deeply to resolve the types of its operands")
        }
        rule(typed)
    } catch (ex: UnsupportedConstructException) {
        if (probing) placeholder(typed) else throw ex
    }
}

/**
 * The placeholder of a rule in a probe pass: a call of the placeholder function with the probed operands. An operand
 * with an aggregate call is an argument of COALESCE instead, where the planner finds the aggregate call.
 */
private fun OperandTypes.placeholder(operands: List<TypedOperand>): Expr {
    val pending = pending(operands.filter { it.probed }.map { it.expr }.chunkedPending(this))
    val aggregates = operands.filter { !it.probed && it.literal == null }.map { it.expr }
    return if (aggregates.isEmpty()) pending else exprCoalesce(listOf(pending) + aggregates)
}

/**
 * The names of the aggregate functions. An operand that calls one of them in its own query block is not probed; see
 * [resolveTyped].
 */
internal val AGGREGATE_FUNCTIONS =
    setOf(
        "any_value", "approximate", "avg", "bit_and", "bit_or", "bool_and", "bool_or", "count", "every", "listagg", "max",
        "median", "min", "percentile_cont", "percentile_disc", "stddev", "stddev_pop", "stddev_samp", "sum", "var_pop",
        "var_samp", "variance",
    )

/** True if [ctx] calls an aggregate function of its own query block, outside a window. */
internal fun containsAggregate(ctx: ParserRuleContext): Boolean =
    when (ctx) {
        is P.AtomCountStarContext -> ctx.overClause() == null
        is P.AtomApproximateContext -> true
        is P.AtomFunctionCallContext ->
            (ctx.overClause() == null && ctx.name.text.lowercase() in AGGREGATE_FUNCTIONS) || ctx.expr().any(::containsAggregate)
        // A subquery has its own aggregation.
        is P.QueryContext -> false
        else -> (0 until ctx.childCount).any { (ctx.getChild(it) as? ParserRuleContext)?.let(::containsAggregate) == true }
    }

/** True if [ctx] calls an aggregate function of its own query block with a REAL argument. */
private fun OperandTypes.aggregatesReal(ctx: ParserRuleContext): Boolean =
    when (ctx) {
        is P.AtomFunctionCallContext -> {
            val aggregate = ctx.overClause() == null && ctx.name.text.lowercase() in AGGREGATE_FUNCTIONS
            (aggregate && ctx.expr().any { typeOf(it)?.code() == PType.REAL }) || ctx.expr().any { aggregatesReal(it) }
        }
        is P.QueryContext -> false
        else -> (0 until ctx.childCount).any { (ctx.getChild(it) as? ParserRuleContext)?.let { c -> aggregatesReal(c) } == true }
    }

/**
 * The type of an operand that is an aggregate call, from the type of its argument, which the translation probes (see
 * [RedshiftExpressions]); null if it is not known. The types are the Redshift result types: COUNT is BIGINT, MIN and
 * MAX have the type of the argument, SUM and AVG of an integer are BIGINT, of a DECIMAL(p,s) DECIMAL(38,s), and of a
 * floating-point number DOUBLE PRECISION.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_SUM.html, https://docs.aws.amazon.com/redshift/latest/dg/r_AVG.html
 */
private fun OperandTypes.aggregateType(ctx: ParserRuleContext): PType? =
    when (val call = unwrap(ctx)) {
        is P.AtomCountStarContext -> PType.bigint()
        is P.AtomFunctionCallContext -> {
            val name = call.name.text.lowercase()
            val arg = call.expr().singleOrNull()?.let { typeOf(it) }
            when {
                call.overClause() != null || call.withinGroup() != null -> null
                name == "count" -> PType.bigint()
                arg == null -> null
                name == "min" || name == "max" -> arg
                name != "sum" && name != "avg" -> null
                else ->
                    when (TypeFamily.of(arg)) {
                        TypeFamily.INTEGER -> PType.bigint()
                        TypeFamily.DECIMAL -> PType.decimal(MAX_DECIMAL_PRECISION, arg.scale)
                        TypeFamily.FLOAT -> PType.doublePrecision()
                        else -> null
                    }
            }
        }
        else -> null
    }

/** Nests [this] in placeholders so that no placeholder has more than [MAX_PENDING_ARGS] arguments. */
private fun List<Expr>.chunkedPending(types: OperandTypes): List<Expr> =
    if (size <= MAX_PENDING_ARGS) this else chunked(MAX_PENDING_ARGS).map { types.pending(it.chunkedPending(types)) }.chunkedPending(types)

/** The placeholder of [OperandTypes] accepts up to 16 arguments. */
private const val MAX_PENDING_ARGS = 16

/**
 * In the final pass, an operand of a form that is never of the dynamic type (arithmetic, a cast) has the dynamic type
 * only if the probe passes stopped before its type was known (see [OperandTypes.MAX_PASSES]).
 */
private fun OperandTypes.isStale(operand: TypedOperand): Boolean =
    !probing && !operand.untyped && operand.type.code() == PType.DYNAMIC && operand.literal == null && isNeverDynamic(operand.ctx)

private fun isNeverDynamic(ctx: ParserRuleContext): Boolean =
    when (val inner = unwrap(ctx)) {
        is P.ValueAdditiveContext, is P.ValueMultiplicativeContext, is P.ValueCastShorthandContext -> true
        is P.ValueSignedContext -> true
        is P.AtomCastContext, is P.AtomConvertContext -> true
        else -> inner !== ctx && isNeverDynamic(inner)
    }

/** True if [ctx] is a predicate or a logical operator, whose value is BOOLEAN (or unknown, for SUPER operands). */
internal fun isBooleanForm(ctx: ParserRuleContext): Boolean =
    when (val inner = unwrap(ctx)) {
        is P.OrContext, is P.AndContext, is P.NotContext, is P.AtomExistsContext -> true
        is P.LiteralTrueContext, is P.LiteralFalseContext, is P.LiteralNullContext -> true
        is P.PredicateContext -> inner !is P.PredicateBaseContext
        else -> false
    }

/**
 * Returns the innermost context of [ctx] that is not a pass-through rule: the base alternatives of the expression rules
 * and parentheses.
 */
internal fun unwrap(ctx: ParserRuleContext): ParserRuleContext =
    when (ctx) {
        is P.ExprContext -> unwrap(ctx.exprOr())
        is P.OrBaseContext -> unwrap(ctx.exprAnd())
        is P.AndBaseContext -> unwrap(ctx.exprNot())
        is P.NotBaseContext -> unwrap(ctx.predicate())
        is P.PredicateBaseContext -> unwrap(ctx.valueExpr())
        is P.ValuePrimaryContext -> unwrap(ctx.primaryExpr())
        is P.PrimaryAtomContext -> unwrap(ctx.atom())
        is P.AtomParenthesizedContext -> unwrap(ctx.expr())
        is P.AtomLiteralContext -> ctx.literal()
        else -> ctx
    }

/**
 * The static type of [lit], as the PartiQL planner types it: integer literals by magnitude, exact numeric literals as
 * DECIMAL(digits, fraction digits).
 */
internal fun literalType(lit: Literal): PType =
    when (lit.code()) {
        Literal.BOOL -> PType.bool()
        Literal.INT_NUM -> {
            val value = BigInteger(lit.numberValue())
            when {
                value.bitLength() < Int.SIZE_BITS -> PType.integer()
                value.bitLength() < Long.SIZE_BITS -> PType.bigint()
                else -> decimalType(BigDecimal(value))
            }
        }
        Literal.EXACT_NUM -> decimalType(lit.bigDecimalValue())
        Literal.APPROX_NUM -> PType.doublePrecision()
        Literal.STRING -> PType.string()
        Literal.TYPED_STRING ->
            when (lit.dataType().code()) {
                DataType.DATE -> PType.date()
                DataType.TIME -> PType.time(DATETIME_PRECISION)
                DataType.TIME_WITH_TIME_ZONE -> PType.timez(DATETIME_PRECISION)
                DataType.TIMESTAMP -> PType.timestamp(DATETIME_PRECISION)
                DataType.TIMESTAMP_WITH_TIME_ZONE -> PType.timestampz(DATETIME_PRECISION)
                else -> PType.dynamic()
            }
        else -> PType.unknown()
    }

private fun decimalType(value: BigDecimal): PType {
    val scale = value.scale().coerceAtLeast(0)
    val precision = (value.precision() - value.scale().coerceAtMost(0)).coerceAtLeast(scale).coerceAtLeast(1)
    return PType.decimal(precision.coerceAtMost(MAX_DECIMAL_PRECISION), scale.coerceAtMost(MAX_DECIMAL_PRECISION))
}

/** The integer digits of a value of an integer or DECIMAL type: SMALLINT 5, INTEGER 10, BIGINT 19, DECIMAL(p,s) p-s. */
internal fun integerDigits(type: PType): Int =
    when (type.code()) {
        PType.TINYINT -> 3
        PType.SMALLINT -> 5
        PType.INTEGER -> 10
        PType.BIGINT -> 19
        PType.DECIMAL, PType.NUMERIC -> type.precision - type.scale
        else -> MAX_DECIMAL_PRECISION
    }

/** The scale of a value of an integer or DECIMAL type. */
internal fun scaleOf(type: PType): Int =
    when (type.code()) {
        PType.DECIMAL, PType.NUMERIC -> type.scale
        else -> 0
    }

/** The PartiQL data type to cast to for the numeric, boolean, and datetime [type], or null for other types. */
internal fun dataTypeOf(type: PType): DataType? =
    when (type.code()) {
        PType.BOOL -> DataType.BOOL()
        PType.SMALLINT -> DataType.SMALLINT()
        PType.INTEGER -> DataType.INT()
        PType.BIGINT -> DataType.BIGINT()
        PType.DECIMAL, PType.NUMERIC -> DataType.DECIMAL(type.precision, type.scale)
        PType.REAL -> DataType.REAL()
        PType.DOUBLE -> DataType.DOUBLE_PRECISION()
        PType.DATE -> DataType.DATE()
        PType.TIME -> DataType.TIME(DATETIME_PRECISION)
        PType.TIMEZ -> DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION)
        PType.TIMESTAMP -> DataType.TIMESTAMP()
        PType.TIMESTAMPZ -> DataType.TIMESTAMP_WITH_TIME_ZONE(DATETIME_PRECISION)
        else -> null
    }

/** `CAST(expr AS type)`, for a [type] that [dataTypeOf] maps. */
internal fun castTo(
    expr: Expr,
    type: PType,
): Expr = exprCast(expr, dataTypeOf(type) ?: error("No cast to $type"))

/** A short name of [type] for messages, in Redshift terms. */
internal fun typeName(type: PType): String =
    when (type.code()) {
        PType.BOOL -> "BOOLEAN"
        PType.TINYINT, PType.SMALLINT -> "SMALLINT"
        PType.INTEGER -> "INTEGER"
        PType.BIGINT -> "BIGINT"
        PType.DECIMAL, PType.NUMERIC -> "DECIMAL"
        PType.REAL -> "REAL"
        PType.DOUBLE -> "DOUBLE PRECISION"
        PType.CHAR -> "CHAR"
        PType.VARCHAR, PType.STRING, PType.CLOB -> "VARCHAR"
        PType.DATE -> "DATE"
        PType.TIME -> "TIME"
        PType.TIMEZ -> "TIMETZ"
        PType.TIMESTAMP -> "TIMESTAMP"
        PType.TIMESTAMPZ -> "TIMESTAMPTZ"
        PType.INTERVAL_YM, PType.INTERVAL_DT -> "INTERVAL"
        PType.UNKNOWN -> "NULL"
        PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.STRUCT, PType.ROW, PType.VARIANT -> "SUPER"
        else -> type.toString()
    }

/** Throws the unsupported-construct exception for [token]. */
internal fun unsupported(
    token: Token,
    construct: String,
): Nothing = throw UnsupportedConstructException.at(token, construct)

/**
 * Redshift stores TIME, TIMETZ, TIMESTAMP, and TIMESTAMPTZ values with microsecond precision. PartiQL TIME without a
 * precision is TIME(0), so the translator writes the precision.
 */
internal const val DATETIME_PRECISION = 6

internal const val MAX_DECIMAL_PRECISION = 38
