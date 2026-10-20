package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNot
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.query
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.PathStep
import org.partiql.spi.types.PType
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Builds PartiQL expressions, literals, and data types from a Redshift parse tree; see [RedshiftAstBuilder].
 *
 * The rules for comparisons and the other conditions are in [RedshiftConditions], pattern matching is in
 * [RedshiftPatterns], the operators are in [RedshiftOperators], and casts and data types are in [RedshiftCasts].
 */
internal class RedshiftExpressions(
    private val builder: RedshiftAstBuilder,
    private val functions: RedshiftFunctions,
    private val types: OperandTypes,
) {
    private val datetime = RedshiftDatetime(builder, this, types)
    private val conditions = RedshiftConditions(builder, this, datetime, types)
    private val patterns = RedshiftPatterns(builder, this, types)
    private val operators = RedshiftOperators(builder, this, datetime, types)
    private val casts = RedshiftCasts(builder, this, datetime, types)

    /*
     * Boolean expressions and predicates
     */

    /**
     * An expression. The condition of WHERE, HAVING, QUALIFY, and a join is translated as a [condition].
     */
    internal fun expr(ctx: P.ExprContext): Expr {
        val parent = ctx.parent
        val isCondition =
            parent is P.WhereClauseContext ||
                parent is P.HavingClauseContext ||
                parent is P.QualifyClauseContext ||
                parent is P.JoinOnContext
        return if (isCondition) condition(ctx) else exprOr(ctx.exprOr())
    }

    /**
     * A condition: the operand of AND, OR, and NOT, the condition of a searched CASE, and the condition of WHERE,
     * HAVING, and a join. Redshift accepts an integer condition (non-zero is TRUE); PartiQL treats a condition that is
     * not BOOLEAN as never true, so the translation reports it as unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_logical_condition.html
     */
    internal fun condition(ctx: P.ExprContext): Expr = condition(ctx, exprOr(ctx.exprOr()))

    private fun condition(
        ctx: ParserRuleContext,
        expr: Expr,
    ): Expr {
        if (isBooleanForm(ctx)) return expr
        if (expr is ExprLit) {
            val code = expr.lit.code()
            if (code != Literal.BOOL && code != Literal.NULL) unsupported(ctx.start, "non-boolean condition")
            return expr
        }
        return types.resolveTyped(listOf(Operand(ctx, expr))) { (x) ->
            if (x.family !in setOf(TypeFamily.BOOLEAN, TypeFamily.UNKNOWN, TypeFamily.SUPER, TypeFamily.UNTYPED)) {
                unsupported(ctx.start, "non-boolean condition")
            }
            x.expr
        }
    }

    private fun exprOr(ctx: P.ExprOrContext): Expr =
        when (ctx) {
            is P.OrContext -> exprOr(condition(ctx.lhs, exprOr(ctx.lhs)), condition(ctx.rhs, exprAnd(ctx.rhs)))
            is P.OrBaseContext -> exprAnd(ctx.exprAnd())
            else -> builder.unexpected(ctx)
        }

    private fun exprAnd(ctx: P.ExprAndContext): Expr =
        when (ctx) {
            is P.AndContext -> exprAnd(condition(ctx.lhs, exprAnd(ctx.lhs)), condition(ctx.rhs, exprNot(ctx.rhs)))
            is P.AndBaseContext -> exprNot(ctx.exprNot())
            else -> builder.unexpected(ctx)
        }

    private fun exprNot(ctx: P.ExprNotContext): Expr =
        when (ctx) {
            is P.NotContext -> exprNot(condition(ctx.exprNot(), exprNot(ctx.exprNot())))
            is P.NotBaseContext -> predicate(ctx.predicate())
            else -> builder.unexpected(ctx)
        }

    private fun predicate(ctx: P.PredicateContext): Expr =
        when (ctx) {
            is P.PredicateQuantifiedContext -> conditions.quantified(ctx)
            is P.PredicateComparisonContext -> conditions.comparison(ctx)
            is P.PredicateNullContext -> exprNullPredicate(valueExpr(ctx.lhs), ctx.NOT() != null)
            is P.PredicateTruthContext -> conditions.truth(ctx)
            is P.PredicateDistinctContext -> conditions.distinct(ctx)
            is P.PredicateBetweenContext -> conditions.between(ctx)
            is P.PredicateInQueryContext -> conditions.inQuery(ctx.IN().symbol, ctx.lhs, ctx.query(), ctx.NOT() != null)
            is P.PredicateInListContext -> conditions.inList(ctx)
            is P.PredicateRowInQueryContext -> unsupported(ctx.IN().symbol, "row value IN subquery")
            is P.PredicateRowInListContext -> conditions.rowInList(ctx)
            is P.PredicateLikeContext -> patterns.like(ctx)
            is P.PredicateSimilarToContext -> patterns.similarTo(ctx)
            is P.PredicateLikeOperatorContext -> patterns.likeOperator(ctx)
            is P.PredicatePosixContext -> patterns.posix(ctx)
            is P.PredicateBaseContext -> valueExpr(ctx.valueExpr())
            else -> builder.unexpected(ctx)
        }

    /*
     * Value expressions
     */

    internal fun valueExpr(ctx: P.ValueExprContext): Expr =
        when (ctx) {
            is P.ValuePrimaryContext -> primaryExpr(ctx.primaryExpr())
            is P.ValueCastShorthandContext ->
                casts.cast(ctx.DOUBLE_COLON().symbol, ctx, Operand(ctx.valueExpr(), valueExpr(ctx.valueExpr())), ctx.dataType())
            is P.ValueSignedContext -> operators.signed(ctx)
            is P.ValueExponentContext -> operators.exponent(ctx)
            is P.ValueMultiplicativeContext -> operators.multiplicative(ctx)
            is P.ValueAdditiveContext -> operators.additive(ctx)
            is P.ValuePrefixOperatorContext -> operators.prefix(ctx)
            is P.ValueAtTimeZoneContext -> datetime.atTimeZone(ctx)
            is P.ValueOperatorContext -> operators.operator(ctx)
            else -> builder.unexpected(ctx)
        }

    /**
     * A primary expression, and a SUPER navigation path. Redshift folds the attribute names of a path to lower case
     * (ASCII letters only; a quoted name is kept when identifiers are case-sensitive), and then matches them exactly.
     * A PartiQL regular path step matches without regard to case, so the attribute steps are delimited. The first step
     * after a name can also be a column of a table: it is a regular step unless the name has the dynamic type, which
     * is the case for a SUPER column and for an element of an unnested array. The steps of a FROM source, which name a
     * table, are regular.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/query-super.html
     */
    internal fun primaryExpr(ctx: P.PrimaryExprContext): Expr {
        // Collect the steps of a path from the inside out, then build one path expression.
        val steps = ArrayDeque<P.PrimaryExprContext>()
        var current = ctx
        while (current is P.PathFieldContext || current is P.PathElementContext) {
            steps.addFirst(current)
            current =
                when (current) {
                    is P.PathFieldContext -> current.primaryExpr()
                    else -> (current as P.PathElementContext).primaryExpr()
                }
        }
        val atomCtx = (current as? P.PrimaryAtomContext ?: builder.unexpected(current)).atom()
        val atom = atom(atomCtx)
        if (steps.isEmpty()) return atom
        val fromSource = ctx.parent is P.TableNameContext
        val named = atomCtx is P.AtomVariableContext
        val columnStep = named && !fromSource && steps.first() is P.PathFieldContext
        val relation = columnStep && isRelationName(atomCtx)
        // Otherwise the type of the name decides whether its first step is a column or an attribute. The decision does
        // not change the type of the path, so the name is probed without a placeholder.
        val root = if (columnStep && !relation) types.operand(atomCtx, atom) else atom
        val dynamicRoot = !relation && types.typeOf(atomCtx)?.code() == PType.DYNAMIC
        val pathSteps =
            steps.mapIndexed { i, step ->
                when (step) {
                    is P.PathFieldContext -> {
                        val regular = fromSource || (named && i == 0 && !dynamicRoot)
                        exprPathStepField(if (regular) builder.simple(step.identifier()) else attribute(step.identifier()))
                    }
                    else -> element(step as P.PathElementContext)
                }
            }
        return exprPath(root, pathSteps.toList<PathStep>())
    }

    /**
     * True if [root] is the name of a table or subquery in the FROM clause of a query block that contains it, so that
     * the first step after it is a column. A FROM item whose source is rooted at another FROM item (an unnest) binds
     * an element instead. For other names the translation probes the type.
     */
    private fun isRelationName(root: P.AtomVariableContext): Boolean {
        val name = builder.simple(root.identifier())
        var ctx: ParserRuleContext? = root
        while (ctx != null) {
            val specs =
                when (ctx) {
                    is P.QuerySpecificationContext -> listOf(ctx)
                    is P.QueryContext -> ctx.children.orEmpty().filterIsInstance<P.QuerySpecificationContext>()
                    else -> emptyList()
                }
            for (spec in specs) {
                val items = spec.children.orEmpty().filterIsInstance<P.FromClauseContext>().flatMap(::fromItems)
                val bindings = items.map { it to bindingName(it) }
                val item = bindings.firstOrNull { (_, binding) -> binding != null && matches(binding, name) }?.first ?: continue
                if (item !is P.TableNameContext) return true
                val sourceRoot = sourceRootName(item.primaryExpr()) ?: return false
                return bindings.none { (other, binding) -> other !== item && binding != null && matches(binding, sourceRoot) }
            }
            ctx = ctx.parent as? ParserRuleContext
        }
        return false
    }

    /** The table and subquery items of a FROM clause, without the items of nested queries. */
    private fun fromItems(ctx: ParserRuleContext): List<ParserRuleContext> =
        when (ctx) {
            is P.TableNameContext, is P.TableSubqueryContext -> listOf(ctx)
            is P.ExprContext -> emptyList()
            else -> ctx.children.orEmpty().filterIsInstance<ParserRuleContext>().flatMap(::fromItems)
        }

    /** The name that a FROM item binds: its alias, or the last name of a table path. */
    private fun bindingName(item: ParserRuleContext): Identifier.Simple? =
        when (item) {
            is P.TableSubqueryContext -> item.tableAlias()?.alias?.let(builder::simple)
            is P.TableNameContext ->
                item.tableAlias()?.alias?.let(builder::simple)
                    ?: when (val source = item.primaryExpr()) {
                        is P.PathFieldContext -> builder.simple(source.identifier())
                        is P.PrimaryAtomContext -> (source.atom() as? P.AtomVariableContext)?.let { builder.simple(it.identifier()) }
                        else -> null
                    }
            else -> null
        }

    /** The first name of a FROM source path, or null if the source is not rooted at a name. */
    private fun sourceRootName(ctx: P.PrimaryExprContext): Identifier.Simple? =
        when (ctx) {
            is P.PathFieldContext -> sourceRootName(ctx.primaryExpr())
            is P.PathElementContext -> sourceRootName(ctx.primaryExpr())
            is P.PrimaryAtomContext -> (ctx.atom() as? P.AtomVariableContext)?.let { builder.simple(it.identifier()) }
            else -> null
        }

    private fun matches(
        binding: Identifier.Simple,
        name: Identifier.Simple,
    ): Boolean = if (binding.isRegular && name.isRegular) binding.text.equals(name.text, ignoreCase = true) else binding.text == name.text

    /** A SUPER attribute name: delimited, with the ASCII letters folded to lower case unless the name is quoted and identifiers are case-sensitive. */
    private fun attribute(ctx: P.IdentifierContext): Identifier.Simple {
        val simple = builder.simple(ctx)
        val text =
            when {
                ctx is P.IdentifierQuotedContext && !simple.isRegular -> simple.text
                ctx is P.IdentifierQuotedContext -> foldAscii(ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\""))
                else -> foldAscii(ctx.text)
            }
        return Identifier.Simple.delimited(text)
    }

    private fun foldAscii(text: String): String = text.map { if (it in 'A'..'Z') it + ('a' - 'A') else it }.joinToString("")

    /**
     * An array subscript `x[n]`, 0-based in both languages. Redshift documents `[]` for array indexes only, so a
     * character string key is unsupported.
     */
    private fun element(ctx: P.PathElementContext): PathStep {
        val index = expr(ctx.expr())
        if (index is ExprLit && index.lit.code() == Literal.STRING) {
            unsupported(ctx.BRACKET_LEFT().symbol, "bracket navigation with a string key; Redshift documents [] only for array indexes")
        }
        // The planner drops an index of the dynamic type, with the probes in it; in a probe pass, where an index can
        // be a placeholder, the index is cast to INT. The cast does not change the type of the path.
        return exprPathStepElement(if (types.probing && index !is ExprLit) exprCast(index, DataType.INT()) else index)
    }

    private fun atom(ctx: P.AtomContext): Expr =
        when (ctx) {
            is P.AtomLiteralContext -> literal(ctx.literal())
            is P.AtomCastContext -> casts.cast(ctx.CAST().symbol, ctx, Operand(ctx.expr(), expr(ctx.expr())), ctx.dataType())
            is P.AtomTryCastContext -> casts.tryCast(ctx)
            is P.AtomConvertContext -> casts.convert(ctx)
            is P.AtomCaseContext -> conditions.case(ctx)
            is P.AtomExistsContext -> exists(ctx)
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
            // https://docs.aws.amazon.com/redshift/latest/dg/r_object_transform_function.html
            is P.AtomObjectTransformContext ->
                throw UnsupportedConstructException.at(ctx, "function `OBJECT_TRANSFORM`; PartiQL has no object transformation")
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
                    else -> {
                        // The type of an aggregate call follows from the types of its arguments; see [resolveTyped].
                        val aggregate = ctx.name.text.lowercase() in AGGREGATE_FUNCTIONS
                        val args =
                            ctx.expr().map { arg -> Operand.deferred(arg) { if (aggregate) types.operand(arg, expr(arg)) else expr(arg) } }
                        functions.call(ctx.name.start, args, builder.setQuantifier(ctx.setQuantifier()))
                    }
                }
            is P.AtomSysdateContext -> datetime.sysdate(ctx.SYSDATE().symbol)
            is P.AtomCurrentDateContext -> datetime.currentDate(ctx.CURRENT_DATE().symbol)
            is P.AtomCurrentDatetimeContext -> datetime.currentDatetime(ctx.name)
            is P.AtomSessionUserContext -> sessionUser(ctx.name)
            is P.AtomSubqueryContext -> builder.exprQuerySet(ctx.query())
            is P.AtomParenthesizedContext -> expr(ctx.expr())
            is P.AtomVariableContext -> builder.variable(ctx)
            else -> builder.unexpected(ctx)
        }

    /**
     * `EXISTS (subquery)`, the same in both languages.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_exists_condition.html
     */
    private fun exists(ctx: P.AtomExistsContext): Expr =
        exprCall(Identifier.regular("exists"), listOf(builder.exprQuerySet(ctx.query())), null)

    /**
     * `CURRENT_USER`, `USER`, `SESSION_USER`, and `CURRENT_USER_ID` return the Redshift user, which a PartiQL session
     * does not have.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CURRENT_USER.html
     */
    private fun sessionUser(name: Token): Expr = unsupported(name, "`${name.text.uppercase()}`")

    private fun literal(ctx: P.LiteralContext): Expr =
        when (ctx) {
            is P.LiteralNullContext -> exprLit(Literal.nul())
            is P.LiteralTrueContext -> exprLit(Literal.bool(true))
            is P.LiteralFalseContext -> exprLit(Literal.bool(false))
            is P.LiteralIntegerContext -> exprLit(Literal.intNum(ctx.text))
            is P.LiteralDecimalContext -> exprLit(Literal.exactNum(ctx.text))
            // The Redshift type of a literal with an exponent is not documented; PostgreSQL types `1e3` as NUMERIC.
            is P.LiteralFloatContext -> unsupported(ctx.start, "numeric literal with an exponent")
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
     */

    /** The PartiQL type of a Redshift data type; see [RedshiftCasts.target]. */
    internal fun dataType(ctx: P.DataTypeContext): DataType = casts.dataType(ctx)

    /**
     * The value of a string literal token. Two quotes are a quote. A backslash is an escape character in Redshift string
     * literals: two backslashes are a backslash, `\'` is a quote, and `\b`, `\f`, `\n`, `\r`, `\t` are the control
     * characters. Other escapes are not documented, so they are unsupported.
     */
    internal fun stringValue(token: Token): String {
        val text = token.text
        val value = StringBuilder()
        var i = 1
        while (i < text.length - 1) {
            val c = text[i]
            when {
                c == '\'' -> {
                    value.append('\'')
                    i += 2
                }
                c == '\\' -> {
                    val escaped = ESCAPES[text[i + 1]] ?: unsupported(token, "backslash escape `\\${text[i + 1]}` in a string literal")
                    value.append(escaped)
                    i += 2
                }
                else -> {
                    value.append(c)
                    i++
                }
            }
        }
        return value.toString()
    }

    private companion object {
        /** https://github.com/partiql/partiql-scribe/issues/158 */
        val ESCAPES =
            mapOf(
                '\\' to '\\',
                '\'' to '\'',
                'b' to '\b',
                'f' to '\u000C',
                'n' to '\n',
                'r' to '\r',
                't' to '\t',
            )
    }
}
