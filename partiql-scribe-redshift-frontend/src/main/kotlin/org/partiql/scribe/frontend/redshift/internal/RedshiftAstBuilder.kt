package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprQuerySet
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
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
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
    internal val functions = RedshiftFunctions(notes, types)
    internal val exprs = RedshiftExpressions(this, functions, types)

    fun root(ctx: P.RootContext): Query = query(exprQuerySet(ctx.query()))

    /*
     * Queries
     */

    // The ORDER BY clause is in the query block of the SELECT, for the operand types.
    internal fun exprQuerySet(ctx: P.QueryContext): ExprQuerySet = types.inScope(ctx) { exprQuerySetInScope(ctx) }

    private fun exprQuerySetInScope(ctx: P.QueryContext): ExprQuerySet {
        val spec = ctx.querySpecification()
        val body = querySpecification(spec)
        val orderBy = ctx.orderByClause()?.let { orderBy(it.sortItem().map(::sortItem)) }
        var limit = ctx.limitClause()?.limit?.let(exprs::valueExpr)
        val offset = ctx.limitClause()?.offset?.let(exprs::valueExpr)
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
        val where = ctx.whereClause()?.let { exprs.expr(it.expr()) }
        val groupBy =
            ctx.groupByClause()?.let { clause ->
                val keys =
                    clause.expr().map { key ->
                        val e = exprs.expr(key)
                        if (e is ExprLit && e.lit.code() == Literal.INT_NUM) {
                            throw UnsupportedConstructException.at(key, "GROUP BY column position")
                        }
                        groupByKey(e)
                    }
                groupBy(GroupByStrategy.FULL(), keys)
            }
        val having = ctx.havingClause()?.let { exprs.expr(it.expr()) }
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
            is P.SelectItemExprContext -> selectItemExpr(exprs.expr(ctx.expr()), ctx.alias?.let(::simple))
            is P.SelectItemStarContext -> selectItemStar(exprs.primaryExpr(ctx.primaryExpr()))
            else -> unexpected(ctx)
        }

    internal fun setQuantifier(ctx: P.SetQuantifierContext?): SetQuantifier? =
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
        val e = exprs.expr(ctx.expr())
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
                fromJoin(tableReference(ctx.lhs), tablePrimary(ctx.rhs), joinType(ctx.joinType()), exprs.expr(ctx.condition))
            is P.TableCrossJoinContext -> fromJoin(tableReference(ctx.lhs), tablePrimary(ctx.rhs), JoinType.CROSS(), null)
            else -> unexpected(ctx)
        }

    private fun tablePrimary(ctx: P.TablePrimaryContext): FromTableRef =
        when (ctx) {
            is P.TableNameContext -> fromExpr(exprs.primaryExpr(ctx.primaryExpr()), FromType.SCAN(), ctx.alias?.let(::simple), null)
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
     * Identifiers
     */

    /**
     * Redshift folds identifiers to lower case, and the folded name is also the output column name. The translator
     * folds the same way, so that derived PartiQL column names agree with Redshift column names.
     */
    internal fun simple(ctx: P.IdentifierContext): Identifier.Simple =
        when (ctx) {
            is P.IdentifierUnquotedContext, is P.IdentifierNonReservedContext -> Identifier.Simple.regular(ctx.text.lowercase())
            is P.IdentifierQuotedContext -> {
                val text = ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
                if (caseSensitiveIdentifiers) Identifier.Simple.delimited(text) else Identifier.Simple.regular(text.lowercase())
            }
            else -> unexpected(ctx)
        }

    internal fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }

    internal fun unexpected(ctx: ParserRuleContext): Nothing = throw UnsupportedConstructException.at(ctx, "`${ctx.text}`")
}
