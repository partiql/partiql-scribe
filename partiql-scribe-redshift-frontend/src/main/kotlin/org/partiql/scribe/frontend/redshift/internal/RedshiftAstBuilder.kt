package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprExtract
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
import org.partiql.ast.Ast.exprSessionAttribute
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.fromJoin
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectItemStar
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.FromTableRef
import org.partiql.ast.FromType
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.JoinType
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.Query
import org.partiql.ast.Select
import org.partiql.ast.SelectItem
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.SessionAttribute
import org.partiql.scribe.frontend.redshift.RedshiftProblem
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Builds a PartiQL AST from a Redshift parse tree.
 *
 * Each method translates one grammar rule. A construct that has no translation throws
 * [UnsupportedConstructException] with the location of the construct in the input.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting. When false (the Redshift
 * default), Redshift folds quoted and unquoted identifiers to lower case, so both become PartiQL regular
 * (case-insensitive) identifiers. When true, quoted identifiers become PartiQL delimited (case-sensitive) identifiers.
 * @param types The static types of operands, for the rules that depend on them; see [OperandTypes].
 */
internal class RedshiftAstBuilder(
    private val caseSensitiveIdentifiers: Boolean,
    private val notes: MutableList<RedshiftProblem>,
    private val types: OperandTypes,
) {
    private val functions = RedshiftFunctions(notes, types)

    fun root(ctx: P.RootContext): Query = query(exprQuerySet(ctx.query()))

    /*
     * Queries
     */

    // The ORDER BY clause is in the query block of the SELECT, for the operand types.
    private fun exprQuerySet(ctx: P.QueryContext): ExprQuerySet = types.inScope(ctx) { exprQuerySetInScope(ctx) }

    private fun exprQuerySetInScope(ctx: P.QueryContext): ExprQuerySet {
        val spec = ctx.querySpecification()
        val body = querySpecification(spec)
        val orderBy = ctx.orderByClause()?.let { orderBy(it.sortItem().map(::sortItem)) }
        var limit = ctx.limitClause()?.limit?.let(::valueExpr)
        val offset = ctx.limitClause()?.offset?.let(::valueExpr)
        spec.top?.let { top ->
            if (limit != null) throw UnsupportedConstructException.at(top, "TOP together with LIMIT")
            note(top.start, "Redshift `TOP n` was replaced by PartiQL `LIMIT n`")
            limit = exprLit(Literal.intNum(top.INTEGER_LITERAL().text))
        }
        return exprQuerySet(body, orderBy, limit, offset)
    }

    private fun querySpecification(ctx: P.QuerySpecificationContext): org.partiql.ast.QueryBody {
        ctx.qualifyClause()?.let { throw UnsupportedConstructException.at(it, "QUALIFY clause") }
        val fromClause = ctx.fromClause() ?: throw UnsupportedConstructException.at(ctx, "SELECT without FROM")
        val select = select(ctx.selectList(), setQuantifier(ctx.setQuantifier()))
        val from = from(fromClause.tableReference().map(::tableReference))
        val where = ctx.whereClause()?.let { expr(it.expr()) }
        val groupBy =
            ctx.groupByClause()?.let { clause ->
                val keys =
                    clause.expr().map { key ->
                        val e = expr(key)
                        if (e is ExprLit && e.lit.code() == Literal.INT_NUM) {
                            throw UnsupportedConstructException.at(key, "GROUP BY column position")
                        }
                        groupByKey(e)
                    }
                groupBy(GroupByStrategy.FULL(), keys)
            }
        val having = ctx.havingClause()?.let { expr(it.expr()) }
        return queryBodySFW(select, null, from, null, where, groupBy, having, null)
    }

    private fun select(
        ctx: P.SelectListContext,
        setq: SetQuantifier?,
    ): Select =
        when (ctx) {
            is P.SelectAllContext -> selectStar(setq)
            is P.SelectItemsContext -> selectList(ctx.selectItem().map(::selectItem), setq)
            else -> unexpected(ctx)
        }

    private fun selectItem(ctx: P.SelectItemContext): SelectItem =
        when (ctx) {
            is P.SelectItemExprContext -> selectItemExpr(expr(ctx.expr()), ctx.alias?.let(::simple))
            is P.SelectItemStarContext -> selectItemStar(primaryExpr(ctx.primaryExpr()))
            else -> unexpected(ctx)
        }

    private fun setQuantifier(ctx: P.SetQuantifierContext?): SetQuantifier? =
        when {
            ctx == null -> null
            ctx.DISTINCT() != null -> SetQuantifier.DISTINCT()
            else -> SetQuantifier.ALL()
        }

    /**
     * Redshift sorts NULL values last for ASC and first for DESC. The translator always writes the null order, so the
     * result does not depend on the PartiQL default.
     */
    private fun sortItem(ctx: P.SortItemContext): org.partiql.ast.Sort {
        val e = expr(ctx.expr())
        if (e is ExprLit && e.lit.code() == Literal.INT_NUM) {
            throw UnsupportedConstructException.at(ctx, "ORDER BY column position")
        }
        val descending = ctx.dir?.type == P.DESC
        val nullsFirst =
            when (ctx.nulls?.type) {
                P.FIRST -> true
                P.LAST -> false
                else -> descending
            }
        return sort(
            e,
            if (descending) Order.DESC() else Order.ASC(),
            if (nullsFirst) Nulls.FIRST() else Nulls.LAST(),
        )
    }

    /*
     * FROM clause
     */

    private fun tableReference(ctx: P.TableReferenceContext): FromTableRef =
        when (ctx) {
            is P.TableBaseContext -> tablePrimary(ctx.tablePrimary())
            is P.TableJoinContext ->
                fromJoin(tableReference(ctx.lhs), tablePrimary(ctx.rhs), joinType(ctx.joinType()), expr(ctx.condition))
            is P.TableCrossJoinContext -> fromJoin(tableReference(ctx.lhs), tablePrimary(ctx.rhs), JoinType.CROSS(), null)
            else -> unexpected(ctx)
        }

    private fun tablePrimary(ctx: P.TablePrimaryContext): FromTableRef =
        when (ctx) {
            is P.TableNameContext -> fromExpr(primaryExpr(ctx.primaryExpr()), FromType.SCAN(), ctx.alias?.let(::simple), null)
            is P.TableSubqueryContext -> fromExpr(exprQuerySet(ctx.query()), FromType.SCAN(), simple(ctx.alias), null)
            else -> unexpected(ctx)
        }

    private fun joinType(ctx: P.JoinTypeContext?): JoinType =
        when {
            ctx == null || ctx.INNER() != null -> JoinType.INNER()
            ctx.LEFT() != null -> JoinType.LEFT_OUTER()
            ctx.RIGHT() != null -> JoinType.RIGHT_OUTER()
            ctx.FULL() != null -> JoinType.FULL_OUTER()
            else -> unexpected(ctx)
        }

    /*
     * Boolean expressions and predicates
     */

    private fun expr(ctx: P.ExprContext): Expr = exprOr(ctx.exprOr())

    private fun exprOr(ctx: P.ExprOrContext): Expr =
        when (ctx) {
            is P.OrContext -> exprOr(exprOr(ctx.lhs), exprAnd(ctx.rhs))
            is P.OrBaseContext -> exprAnd(ctx.exprAnd())
            else -> unexpected(ctx)
        }

    private fun exprAnd(ctx: P.ExprAndContext): Expr =
        when (ctx) {
            is P.AndContext -> exprAnd(exprAnd(ctx.lhs), exprNot(ctx.rhs))
            is P.AndBaseContext -> exprNot(ctx.exprNot())
            else -> unexpected(ctx)
        }

    private fun exprNot(ctx: P.ExprNotContext): Expr =
        when (ctx) {
            is P.NotContext -> exprNot(exprNot(ctx.exprNot()))
            is P.NotBaseContext -> predicate(ctx.predicate())
            else -> unexpected(ctx)
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
            is P.PredicateInQueryContext -> exprInCollection(valueExpr(ctx.lhs), exprQuerySet(ctx.query()), ctx.NOT() != null)
            is P.PredicateInListContext ->
                exprInCollection(valueExpr(ctx.lhs), exprArray(ctx.expr().map(::expr)), ctx.NOT() != null)
            is P.PredicateLikeContext -> {
                if (ctx.op.type == P.ILIKE) throw UnsupportedConstructException.at(ctx.op, "ILIKE")
                exprLike(valueExpr(ctx.lhs), valueExpr(ctx.rhs), ctx.escape?.let(::valueExpr), ctx.NOT() != null)
            }
            is P.PredicateBaseContext -> valueExpr(ctx.valueExpr())
            else -> unexpected(ctx)
        }

    /*
     * Value expressions
     */

    private fun valueExpr(ctx: P.ValueExprContext): Expr =
        when (ctx) {
            is P.ValuePrimaryContext -> primaryExpr(ctx.primaryExpr())
            is P.ValueCastShorthandContext -> exprCast(valueExpr(ctx.valueExpr()), dataType(ctx.dataType()))
            is P.ValueSignedContext -> signed(ctx.sign.text, valueExpr(ctx.valueExpr()))
            is P.ValueMultiplicativeContext -> exprOperator(ctx.op.text, valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            is P.ValueAdditiveContext -> exprOperator(ctx.op.text, valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            is P.ValueConcatContext -> exprOperator("||", valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            else -> unexpected(ctx)
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

    private fun primaryExpr(ctx: P.PrimaryExprContext): Expr {
        // Collect the steps of a path from the inside out, then build one path expression.
        val steps = ArrayDeque<PathStep>()
        var current = ctx
        while (true) {
            current =
                when (current) {
                    is P.PathFieldContext -> {
                        steps.addFirst(exprPathStepField(simple(current.identifier())))
                        current.primaryExpr()
                    }
                    is P.PathElementContext -> {
                        steps.addFirst(exprPathStepElement(expr(current.expr())))
                        current.primaryExpr()
                    }
                    is P.PrimaryAtomContext -> break
                    else -> unexpected(current)
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
            is P.AtomDateFunctionContext -> dateFunction(ctx)
            is P.AtomExtractContext -> extract(ctx)
            is P.AtomCountStarContext -> exprCall(Identifier.regular("count"), emptyList(), null)
            is P.AtomFunctionCallContext ->
                functions.call(ctx.name.start, ctx.expr().map { Operand(it, expr(it)) }, setQuantifier(ctx.setQuantifier()))
            is P.AtomSysdateContext -> functions.sysdate(ctx.SYSDATE().symbol)
            is P.AtomCurrentDateContext -> exprSessionAttribute(SessionAttribute.CURRENT_DATE())
            is P.AtomSubqueryContext -> exprQuerySet(ctx.query())
            is P.AtomParenthesizedContext -> expr(ctx.expr())
            is P.AtomVariableContext -> exprVarRef(Identifier.of(simple(ctx.identifier())), false)
            else -> unexpected(ctx)
        }

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
    private fun dateFunction(ctx: P.AtomDateFunctionContext): Expr {
        val partText = ctx.part.text.lowercase()
        val part = DATE_PARTS[partText] ?: throw UnsupportedConstructException.at(ctx.part, "datepart `${ctx.part.text}`")
        val isAdd = ctx.name.type == P.DATEADD
        val function = if (isAdd) "date_add" else "date_diff"
        note(ctx.name, "Redshift `${ctx.name.text.uppercase()}` was replaced by PartiQL `${function.uppercase()}`")
        val args = ctx.expr().map(::expr)
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
    private fun extract(ctx: P.AtomExtractContext): Expr {
        val partToken = ctx.part.start
        val partText =
            if (ctx.part.STRING_LITERAL() != null) stringValue(partToken).lowercase() else ctx.part.text.lowercase()
        // TIMEZONE_HOUR and TIMEZONE_MINUTE are not supported: the Scribe PartiQL target cannot print them.
        val part = DATE_PARTS[partText] ?: throw UnsupportedConstructException.at(partToken, "EXTRACT datepart `${ctx.part.text}`")
        if (part == "second") {
            note(
                partToken,
                "Redshift `EXTRACT(SECOND ...)` returns INTEGER for TIMESTAMP and TIME values, without the fraction; " +
                    "PartiQL returns a DECIMAL with the fraction",
            )
        }
        return exprExtract(DatetimeField.parse(part.uppercase()), expr(ctx.expr()))
    }

    private fun literal(ctx: P.LiteralContext): Expr =
        when (ctx) {
            is P.LiteralNullContext -> exprLit(Literal.nul())
            is P.LiteralTrueContext -> exprLit(Literal.bool(true))
            is P.LiteralFalseContext -> exprLit(Literal.bool(false))
            is P.LiteralIntegerContext -> exprLit(Literal.intNum(ctx.text))
            is P.LiteralDecimalContext -> exprLit(Literal.exactNum(ctx.text))
            is P.LiteralStringContext -> exprLit(Literal.string(stringValue(ctx.STRING_LITERAL().symbol)))
            is P.LiteralDateContext -> {
                val value = stringValue(ctx.STRING_LITERAL().symbol)
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
            else -> unexpected(ctx)
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
        val value = stringValue(string)
        if (!pattern.matches(value)) throw UnsupportedConstructException.at(ctx, "$name literal format '$value'")
        return exprLit(Literal.typedString(type, value))
    }

    /*
     * Data types
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */

    private fun dataType(ctx: P.DataTypeContext): DataType =
        when (ctx) {
            is P.TypeDoublePrecisionContext -> DataType.DOUBLE_PRECISION()
            is P.TypeCharacterVaryingContext -> varchar(ctx.length?.text?.toInt())
            is P.TypeTimeContext -> DataType.TIME(DATETIME_PRECISION)
            is P.TypeTimeTzContext -> DataType.TIME_WITH_TIME_ZONE(DATETIME_PRECISION)
            // TIMESTAMP without a precision is TIMESTAMP(6) in PartiQL.
            is P.TypeTimestampContext -> DataType.TIMESTAMP()
            is P.TypeTimestampTzContext -> DataType.TIMESTAMP_WITH_TIME_ZONE(DATETIME_PRECISION)
            is P.TypeDateContext -> DataType.DATE()
            is P.TypeNamedContext -> namedType(ctx)
            else -> unexpected(ctx)
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
            "char", "character", "nchar", "bpchar" -> if (arg1 != null) unexpected(ctx) else DataType.CHAR(arg0 ?: 1)
            "varchar", "nvarchar" -> if (arg1 != null) unexpected(ctx) else varchar(arg0)
            "text" -> noArgs(varchar(null))
            else -> throw UnsupportedConstructException.at(ctx, "data type `${ctx.name.text}`")
        }
    }

    /**
     * VARCHAR without a length (and TEXT) is VARCHAR(256) in Redshift.
     */
    private fun varchar(length: Int?): DataType = DataType.VARCHAR(length ?: 256)

    /*
     * Identifiers
     */

    /**
     * Redshift folds identifiers to lower case, and the folded name is also the output column name. The translator
     * folds the same way, so that derived PartiQL column names agree with Redshift column names.
     */
    private fun simple(ctx: P.IdentifierContext): Identifier.Simple =
        when (ctx) {
            is P.IdentifierUnquotedContext, is P.IdentifierNonReservedContext -> Identifier.Simple.regular(ctx.text.lowercase())
            is P.IdentifierQuotedContext -> {
                val text = ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
                if (caseSensitiveIdentifiers) Identifier.Simple.delimited(text) else Identifier.Simple.regular(text.lowercase())
            }
            else -> unexpected(ctx)
        }

    private fun stringValue(token: Token): String = token.text.removePrefix("'").removeSuffix("'").replace("''", "'")

    private fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }

    private fun unexpected(ctx: ParserRuleContext): Nothing = throw UnsupportedConstructException.at(ctx, "`${ctx.text}`")

    private companion object {
        /** The prefix of hidden PartiQL built-in function names; see `FunctionUtils` in partiql-spi. */
        const val SYSTEM_PREFIX_INTERNAL = "\uFDEF"

        /**
         * Redshift stores TIME, TIMETZ, TIMESTAMP, and TIMESTAMPTZ values with microsecond precision. PartiQL TIME
         * without a precision is TIME(0), so the translator writes the precision.
         */
        const val DATETIME_PRECISION = 6

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
