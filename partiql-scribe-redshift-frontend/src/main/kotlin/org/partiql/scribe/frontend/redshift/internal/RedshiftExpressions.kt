package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNot
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.query
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.PathStep
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Builds PartiQL expressions, literals, and data types from a Redshift parse tree; see [RedshiftAstBuilder].
 */
internal class RedshiftExpressions(
    private val builder: RedshiftAstBuilder,
    private val functions: RedshiftFunctions,
    private val types: OperandTypes,
) {
    private val datetime = RedshiftDatetime(builder, this, types)

    /*
     * Boolean expressions and predicates
     */

    internal fun expr(ctx: P.ExprContext): Expr = exprOr(ctx.exprOr())

    private fun exprOr(ctx: P.ExprOrContext): Expr =
        when (ctx) {
            is P.OrContext -> exprOr(exprOr(ctx.lhs), exprAnd(ctx.rhs))
            is P.OrBaseContext -> exprAnd(ctx.exprAnd())
            else -> builder.unexpected(ctx)
        }

    private fun exprAnd(ctx: P.ExprAndContext): Expr =
        when (ctx) {
            is P.AndContext -> exprAnd(exprAnd(ctx.lhs), exprNot(ctx.rhs))
            is P.AndBaseContext -> exprNot(ctx.exprNot())
            else -> builder.unexpected(ctx)
        }

    private fun exprNot(ctx: P.ExprNotContext): Expr =
        when (ctx) {
            is P.NotContext -> exprNot(exprNot(ctx.exprNot()))
            is P.NotBaseContext -> predicate(ctx.predicate())
            else -> builder.unexpected(ctx)
        }

    private fun predicate(ctx: P.PredicateContext): Expr =
        when (ctx) {
            is P.PredicateComparisonContext -> {
                // PartiQL spells inequality as `<>` only.
                val op = if (ctx.op.NEQ() != null) "<>" else ctx.op.text
                exprOperator(op, valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            }
            is P.PredicateNullContext -> exprNullPredicate(valueExpr(ctx.lhs), ctx.NOT() != null)
            is P.PredicateBetweenContext ->
                exprBetween(valueExpr(ctx.lhs), valueExpr(ctx.lower), valueExpr(ctx.upper), ctx.NOT() != null)
            is P.PredicateInQueryContext -> exprInCollection(valueExpr(ctx.lhs), builder.exprQuerySet(ctx.query()), ctx.NOT() != null)
            is P.PredicateInListContext ->
                exprInCollection(valueExpr(ctx.lhs), exprArray(ctx.expr().map(::expr)), ctx.NOT() != null)
            is P.PredicateLikeContext -> {
                if (ctx.op.type == P.ILIKE) throw UnsupportedConstructException.at(ctx.op, "ILIKE")
                exprLike(valueExpr(ctx.lhs), valueExpr(ctx.rhs), ctx.escape?.let(::valueExpr), ctx.NOT() != null)
            }
            is P.PredicateBaseContext -> valueExpr(ctx.valueExpr())
            else -> builder.unexpected(ctx)
        }

    /*
     * Value expressions
     */

    internal fun valueExpr(ctx: P.ValueExprContext): Expr =
        when (ctx) {
            is P.ValuePrimaryContext -> primaryExpr(ctx.primaryExpr())
            is P.ValueCastShorthandContext -> exprCast(valueExpr(ctx.valueExpr()), dataType(ctx.dataType()))
            is P.ValueSignedContext -> signed(ctx.sign.text, valueExpr(ctx.valueExpr()))
            is P.ValueMultiplicativeContext -> exprOperator(ctx.op.text, valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            is P.ValueAdditiveContext -> additive(ctx)
            is P.ValueAtTimeZoneContext -> datetime.atTimeZone(ctx)
            is P.ValueConcatContext -> exprOperator("||", valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            else -> builder.unexpected(ctx)
        }

    /**
     * `+` and `-`. When an operand is a datetime or interval value, the Redshift semantics depend on the operand types;
     * see [RedshiftDatetime.additive].
     */
    private fun additive(ctx: P.ValueAdditiveContext): Expr =
        types.resolve(listOf(ctx.lhs to valueExpr(ctx.lhs), ctx.rhs to valueExpr(ctx.rhs))) { exprs, operandTypes ->
            datetime.additive(ctx.op, exprs[0], operandTypes[0], exprs[1], operandTypes[1])
                ?: exprOperator(ctx.op.text, exprs[0], exprs[1])
        }

    /**
     * A sign on a numeric literal becomes part of the literal, the same as in the PartiQL parser.
     */
    private fun signed(
        sign: String,
        value: Expr,
    ): Expr {
        if (value is ExprLit) {
            val text = value.lit.numberValue()
            val negated =
                when (value.lit.code()) {
                    Literal.INT_NUM -> Literal.intNum("$sign$text")
                    Literal.EXACT_NUM -> Literal.exactNum("$sign$text")
                    else -> null
                }
            if (negated != null && sign == "-" && !text.startsWith("-")) return exprLit(negated)
        }
        return exprOperator(sign, null, value)
    }

    internal fun primaryExpr(ctx: P.PrimaryExprContext): Expr {
        // Collect the steps of a path from the inside out, then build one path expression.
        val steps = ArrayDeque<PathStep>()
        var current = ctx
        while (true) {
            current =
                when (current) {
                    is P.PathFieldContext -> {
                        steps.addFirst(exprPathStepField(builder.simple(current.identifier())))
                        current.primaryExpr()
                    }
                    is P.PathElementContext -> {
                        steps.addFirst(exprPathStepElement(expr(current.expr())))
                        current.primaryExpr()
                    }
                    is P.PrimaryAtomContext -> break
                    else -> builder.unexpected(current)
                }
        }
        val root = atom((current as P.PrimaryAtomContext).atom())
        return if (steps.isEmpty()) root else exprPath(root, steps.toList())
    }

    private fun atom(ctx: P.AtomContext): Expr =
        when (ctx) {
            is P.AtomLiteralContext -> literal(ctx.literal())
            is P.AtomCastContext -> exprCast(expr(ctx.expr()), dataType(ctx.dataType()))
            is P.AtomCaseContext ->
                exprCase(
                    ctx.operand?.let(::expr),
                    ctx.caseWhen().map { exprCaseBranch(expr(it.condition), expr(it.result)) },
                    ctx.otherwise?.let(::expr),
                )
            is P.AtomDateFunctionContext -> datetime.dateFunction(ctx)
            is P.AtomExtractContext -> datetime.extract(ctx)
            is P.AtomPositionContext ->
                functions.position(ctx.POSITION().symbol, Operand(ctx.sub, valueExpr(ctx.sub)), Operand(ctx.str, valueExpr(ctx.str)))
            is P.AtomSubstringFromContext ->
                functions.substring(
                    ctx.SUBSTRING().symbol,
                    Operand(ctx.value, expr(ctx.value)),
                    Operand(ctx.start, expr(ctx.start)),
                    ctx.length?.let { Operand(it, expr(it)) },
                )
            is P.AtomSubstringForContext ->
                functions.substring(
                    ctx.SUBSTRING().symbol,
                    Operand(ctx.value, expr(ctx.value)),
                    null,
                    Operand(ctx.length, expr(ctx.length)),
                )
            is P.AtomTrimContext ->
                functions.trim(
                    ctx.TRIM().symbol,
                    ctx.spec,
                    ctx.chars?.let { Operand(it, expr(it)) },
                    Operand(ctx.value, expr(ctx.value)),
                )
            is P.AtomApproximateContext -> throw UnsupportedConstructException.at(ctx, "APPROXIMATE aggregate")
            is P.AtomCountStarContext ->
                if (ctx.overClause() != null) {
                    builder.window(ctx)
                } else {
                    exprCall(Identifier.regular("count"), emptyList(), null)
                }
            is P.AtomFunctionCallContext ->
                when {
                    ctx.withinGroup() != null -> throw UnsupportedConstructException.at(ctx.withinGroup(), "WITHIN GROUP")
                    ctx.overClause() != null || ctx.nullTreatment() != null -> builder.window(ctx)
                    else ->
                        functions.call(ctx.name.start, ctx.expr().map { Operand(it, expr(it)) }, builder.setQuantifier(ctx.setQuantifier()))
                }
            is P.AtomSysdateContext -> datetime.sysdate(ctx.SYSDATE().symbol)
            is P.AtomCurrentDateContext -> datetime.currentDate(ctx.CURRENT_DATE().symbol)
            is P.AtomCurrentDatetimeContext -> datetime.currentDatetime(ctx.name)
            is P.AtomSubqueryContext -> builder.exprQuerySet(ctx.query())
            is P.AtomParenthesizedContext -> expr(ctx.expr())
            is P.AtomVariableContext -> exprVarRef(Identifier.of(builder.simple(ctx.identifier())), false)
            else -> builder.unexpected(ctx)
        }

    private fun literal(ctx: P.LiteralContext): Expr =
        when (ctx) {
            is P.LiteralNullContext -> exprLit(Literal.nul())
            is P.LiteralTrueContext -> exprLit(Literal.bool(true))
            is P.LiteralFalseContext -> exprLit(Literal.bool(false))
            is P.LiteralIntegerContext -> exprLit(Literal.intNum(ctx.text))
            is P.LiteralDecimalContext -> exprLit(Literal.exactNum(ctx.text))
            is P.LiteralStringContext -> exprLit(Literal.string(stringValue(ctx.STRING_LITERAL().symbol)))
            is P.LiteralDateContext,
            is P.LiteralTimeContext,
            is P.LiteralTimeTzContext,
            is P.LiteralTimestampContext,
            is P.LiteralTimestampTzContext,
            -> datetime.literal(ctx)
            is P.LiteralIntervalContext -> datetime.intervalLiteral(ctx)
            else -> builder.unexpected(ctx)
        }

    /*
     * Data types
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */

    internal fun dataType(ctx: P.DataTypeContext): DataType =
        when (ctx) {
            is P.TypeDoublePrecisionContext -> DataType.DOUBLE_PRECISION()
            is P.TypeCharacterVaryingContext -> varchar(ctx.length?.text?.toInt())
            is P.TypeTimeContext -> DataType.TIME(DATETIME_PRECISION)
            is P.TypeTimeTzContext -> DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION)
            // TIMESTAMP without a precision is TIMESTAMP(6) in PartiQL.
            is P.TypeTimestampContext -> DataType.TIMESTAMP()
            is P.TypeTimestampTzContext -> DataType.TIMESTAMP_WITH_TIME_ZONE(DATETIME_PRECISION)
            is P.TypeDateContext -> DataType.DATE()
            is P.TypeIntervalContext -> datetime.intervalType(ctx)
            is P.TypeNamedContext -> namedType(ctx)
            else -> builder.unexpected(ctx)
        }

    private fun namedType(ctx: P.TypeNamedContext): DataType {
        val name = ctx.name.text.lowercase()
        val arg0 = ctx.arg0?.text?.toInt()
        val arg1 = ctx.arg1?.text?.toInt()
        val hasArgs = arg0 != null

        fun noArgs(type: DataType): DataType {
            if (hasArgs) throw UnsupportedConstructException.at(ctx, "type parameters for `${ctx.name.text}`")
            return type
        }
        return when (name) {
            "smallint", "int2" -> noArgs(DataType.SMALLINT())
            "integer", "int", "int4" -> noArgs(DataType.INTEGER())
            "bigint", "int8" -> noArgs(DataType.BIGINT())
            "real", "float4" -> noArgs(DataType.REAL())
            "float8" -> noArgs(DataType.DOUBLE_PRECISION())
            // FLOAT(p) is REAL for p <= 24 and DOUBLE PRECISION otherwise.
            "float" -> if (arg0 != null && arg1 == null && arg0 <= 24) DataType.REAL() else noArgs(DataType.DOUBLE_PRECISION())
            "boolean", "bool" -> noArgs(DataType.BOOLEAN())
            // DECIMAL without parameters is DECIMAL(18, 0) in Redshift.
            "decimal", "numeric" -> DataType.DECIMAL(arg0 ?: 18, arg1 ?: 0)
            // CHAR without a length is CHAR(1).
            "char", "character", "nchar", "bpchar" -> if (arg1 != null) builder.unexpected(ctx) else DataType.CHAR(arg0 ?: 1)
            "varchar", "nvarchar" -> if (arg1 != null) builder.unexpected(ctx) else varchar(arg0)
            "text" -> noArgs(varchar(null))
            else -> throw UnsupportedConstructException.at(ctx, "data type `${ctx.name.text}`")
        }
    }

    /**
     * VARCHAR without a length (and TEXT) is VARCHAR(256) in Redshift.
     */
    private fun varchar(length: Int?): DataType = DataType.VARCHAR(length ?: 256)

    /** The value of a string literal token. */
    internal fun stringValue(token: Token): String = token.text.removePrefix("'").removeSuffix("'").replace("''", "'")

    private companion object {
        /**
         * Redshift stores TIME, TIMETZ, TIMESTAMP, and TIMESTAMPTZ values with microsecond precision. PartiQL TIME
         * without a precision is TIME(0), so the translator writes the precision.
         */
        const val DATETIME_PRECISION = 6
    }
}
