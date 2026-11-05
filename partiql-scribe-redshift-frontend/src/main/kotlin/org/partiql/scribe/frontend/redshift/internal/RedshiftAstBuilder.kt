package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exclude
import org.partiql.ast.Ast.excludePath
import org.partiql.ast.Ast.excludeStepStructField
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprBoolTest
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
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.fromJoin
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.queryBodySetOp
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectItemStar
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.setOp
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.with
import org.partiql.ast.Ast.withListElement
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.DataType
import org.partiql.ast.Exclude
import org.partiql.ast.FromTableRef
import org.partiql.ast.FromType
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.JoinType
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectStar
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.Statement
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.TruthValue
import org.partiql.scribe.frontend.redshift.RedshiftFrontend
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Builds the PartiQL AST of a Redshift parse tree.
 *
 * Each rule rewrites a form of the Redshift query into a form of PartiQL. The planner then resolves the names and types
 * of the result. A construct that has no conversion yet stops the build with [TranslationStop] at its position; the
 * string, math, and datetime functions are in that set for now, and so are the `INTERVAL` literal and the operators `^`
 * and `&`. The conditional functions are translated by [ConditionalFunctions].
 * The depth of the AST is bounded by [RedshiftFrontend.MAX_NESTING]; see [nested].
 *
 * The rewrites that change the form of the query:
 *  - `TOP n` is `LIMIT n`; `LIMIT ALL` is dropped.
 *  - A `DISTINCT` query with `LIMIT` or `OFFSET` becomes a `DISTINCT` subquery with the `ORDER BY`, `LIMIT`, and `OFFSET`
 *    outside, because PartiQL evaluates `DISTINCT` after `LIMIT` and Redshift before; see [wrapDistinct].
 *  - A `GROUP BY` or `ORDER BY` ordinal is the select-list expression it refers to.
 *  - An `ORDER BY` key that is only the output name of a select item (its alias, or the name of the column it selects)
 *    is that item's expression: an output name wins in `ORDER BY` when it stands alone, and inside an expression a name
 *    is an input column. A name that several items have is ambiguous unless they are the same expression.
 *
 * A select-list alias used in `WHERE`, `GROUP BY`, `HAVING`, or a later select item (a lateral column alias) is left as
 * a name: whether it means the alias or a column depends on the schema (a FROM column of the same name wins in
 * Redshift), which a later change decides.
 *
 *  - `* EXCLUDE (c)` qualifies each excluded column with the single FROM item, or with the relation the query names;
 *    a qualifier the query writes must be a FROM item, and the relation behind a `t.*`.
 *  - `x::t` and `CONVERT(t, x)` are `CAST(x AS t)`; the Redshift type aliases map to the PartiQL type names.
 *  - A literal `LIKE` pattern is written as PartiQL takes it, escaping with a backslash only before `%`, `_`, and `\`.
 *  - `ILIKE` is `LOWER(a) LIKE b` with a literal pattern lowered here, or `LOWER(a) LIKE LOWER(b)`; see [like].
 *  - `ISNULL` and `NOTNULL` are `IS [NOT] NULL`; `!=` is `<>`.
 *  - A comma join is a `CROSS JOIN`.
 *  - A common table expression is referenced as a local variable (`@name`), so that a table of the same name does
 *    not capture it.
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting: when false, delimited
 * identifiers fold to lower case as unquoted ones do.
 * @param listener Receives a [ScribeProblem.TRANSLATION_INFO] note for a rewrite that changes the shape of the query: the
 * `DISTINCT` subquery. The other rewrites are one-to-one and keep the shape, so they are not reported.
 */
internal class RedshiftAstBuilder(
    private val caseSensitiveIdentifiers: Boolean,
    private val listener: ScribeProblemListener,
) {
    /** The common table expressions in scope, innermost last. */
    private val commonTables = ArrayList<String>()

    /**
     * The tables the query reads, as the qualified names it writes, folded, without duplicates, in order of first
     * appearance. A name that a common table expression in scope defines is not a table.
     */
    val tables = LinkedHashSet<List<String>>()

    /** How many times each common table expression name in scope is defined, for the lookup. */
    private val commonTableCounts = HashMap<String, Int>()

    /** The current depth of the AST under construction; see [nested]. */
    private var depth = 0

    private val conditional = ConditionalFunctions(this)

    /**
     * The select list of a query block, for the block's ORDER BY and for the DISTINCT subquery: the expression items in
     * order, each with the output name the planner gives it, and what the FROM clause binds.
     *
     * @param bindings The names the FROM items bind: aliases, or table names.
     * @param relations The number of FROM items (tables and subqueries), through the joins.
     */
    private class Block(val bindings: List<Identifier.Simple>, val relations: Int) {
        val exprs = ArrayList<Expr>()
        val names = ArrayList<Identifier.Simple>()

        /** Whether the name at an index is the planner's `_n` for an unnamed item, which is not a name ORDER BY can use. */
        val synthetic = ArrayList<Boolean>()

        /** `*` alone as the select list. */
        var selectStar = false

        /** The qualifiers of the `t.*` items. */
        val stars = ArrayList<Identifier.Simple>()

        private var unnamed = 0

        fun add(
            expr: Expr,
            alias: Identifier.Simple?,
        ) {
            exprs.add(expr)
            val binder = alias ?: binder(expr)
            names.add(binder ?: Identifier.Simple.delimited("_${++unnamed}"))
            synthetic.add(binder == null)
        }

        /**
         * The name the planner gives an item without an alias: the name of a variable, the last field of a path, the
         * operand's name for a `CAST`; null for another expression, which the planner names `_n`.
         */
        private fun binder(expr: Expr): Identifier.Simple? =
            when (expr) {
                is ExprVarRef -> expr.identifier.identifier
                is ExprPath -> (expr.steps.lastOrNull() as? PathStep.Field)?.field ?: binder(expr.root)
                is ExprCast -> binder(expr.value)
                else -> null
            }

        /** The items whose output name is [name], as an ORDER BY key names them: an alias or a column, not an `_n`. */
        fun named(name: Identifier.Simple): List<Int> = names.indices.filter { !synthetic[it] && names[it].text == name.text }

        /** The relation behind `*` or a single `t.*`, when there is one: the only FROM item, or `t`. */
        fun starRelation(): Identifier.Simple? =
            when {
                selectStar -> if (relations == 1) bindings.singleOrNull() else null
                else -> stars.singleOrNull()
            }

        /**
         * Whether the items [named] alike are different expressions, which makes their name ambiguous in ORDER BY. The
         * expressions are compared with every identifier delimited (they are already folded) and, when the block has
         * one FROM item, without that item as a qualifier, so that `x.b` and `b` are the same column. With several FROM
         * items `b` and `y.b` may or may not be the same column: that is [uncertain].
         */
        fun ambiguous(items: List<Int>): Ambiguity {
            val canonical = Canonical(bindings.singleOrNull()?.text)
            val forms = items.map { exprs[it].accept(canonical, Unit) as Expr }.distinct()
            if (forms.size == 1) return Ambiguity.NONE
            val columns = forms.map { it.column() }
            val field = columns.firstOrNull()?.second
            val sameField = columns.all { it != null && it.second == field }
            val uncertain = sameField && columns.any { it!!.first == null } && bindings.size != 1
            return if (uncertain) Ambiguity.UNCERTAIN else Ambiguity.AMBIGUOUS
        }

        /** The qualifier and field of an expression that is a column reference, or null. */
        private fun Expr.column(): Pair<String?, String>? =
            when (this) {
                is ExprVarRef -> if (identifier.qualifier.isEmpty()) null to identifier.identifier.text else null
                is ExprPath -> {
                    val root = root as? ExprVarRef
                    val step = steps.singleOrNull() as? PathStep.Field
                    val qualifier = root?.identifier?.takeIf { it.qualifier.isEmpty() }?.identifier?.text
                    if (qualifier != null && step != null) qualifier to step.field.text else null
                }
                else -> null
            }
    }

    private enum class Ambiguity { NONE, AMBIGUOUS, UNCERTAIN }

    /** Rewrites an expression to the form that [Block.ambiguous] compares. */
    private class Canonical(private val binding: String?) : AstRewriter<Unit>() {
        override fun visitIdentifierSimple(
            node: Identifier.Simple,
            ctx: Unit,
        ): AstNode = if (node.isRegular) Identifier.Simple.delimited(node.text) else node

        override fun visitExprPath(
            node: ExprPath,
            ctx: Unit,
        ): AstNode {
            val root = node.root as? ExprVarRef
            val first = node.steps.firstOrNull() as? PathStep.Field
            val qualifier = root?.identifier?.takeIf { it.qualifier.isEmpty() }?.identifier?.text
            if (binding != null && qualifier == binding && first != null) {
                val column = exprVarRef(Identifier.of(Identifier.Simple.delimited(first.field.text)), false)
                val rest = node.steps.drop(1)
                return if (rest.isEmpty()) column else super.visitExprPath(exprPath(column, rest), ctx)
            }
            return super.visitExprPath(node, ctx)
        }
    }

    /**
     * Converts [body] one level deeper in the AST. The depth is bounded by [RedshiftFrontend.MAX_NESTING], so that the
     * recursion of this builder, and of the planner and the targets over the AST, is bounded by the text.
     */
    fun <T> nested(
        at: Token,
        body: () -> T,
    ): T {
        if (depth >= RedshiftFrontend.MAX_NESTING) {
            throw TranslationStop.unsupported(at, "a query nested more than ${RedshiftFrontend.MAX_NESTING} levels deep")
        }
        depth++
        try {
            return body()
        } finally {
            depth--
        }
    }

    fun root(ctx: P.RootContext): Statement = query(query(ctx.query()).query)

    /*
     * Queries
     */

    /**
     * A translated query term. When the term is one `SELECT` block, possibly in parentheses, [items] and [block] are its
     * select list, which the ORDER BY, LIMIT, and OFFSET of an enclosing query see, and [top] is its `TOP` clause when
     * no query has taken it yet.
     */
    private class Term(
        val query: ExprQuerySet,
        val items: List<P.SelectItemContext>?,
        val block: Block?,
        val top: P.TopClauseContext?,
    )

    /** A subquery, one level deeper than the query it is in. */
    fun querySet(ctx: P.QueryContext): ExprQuerySet = nested(ctx.start) { query(ctx).query }

    private fun query(ctx: P.QueryContext): Term {
        val scope = commonTables.size
        try {
            val with =
                ctx.withClause()?.let { clause ->
                    with(
                        clause.commonTableExpression().map { cte ->
                            cte.columnAliases()?.let { throw TranslationStop.unsupported(it, "column list of a common table expression") }
                            val element = withListElement(simple(cte.name), querySet(cte.query()), null)
                            // A common table expression sees the ones before it, not itself (that would be RECURSIVE).
                            defineCommonTable(fold(cte.name))
                            element
                        },
                        false,
                    )
                }
            val term = queryTerm(ctx.queryTerm())
            val result = clauses(ctx, term, with)
            // The select list stays visible through parentheses only while no clause has been added around it.
            return if (result === term.query) Term(result, term.items, term.block, null) else Term(result, null, null, null)
        } finally {
            while (commonTables.size > scope) undefineCommonTable()
        }
    }

    private fun defineCommonTable(name: String) {
        commonTables.add(name)
        commonTableCounts.merge(name, 1, Int::plus)
    }

    private fun undefineCommonTable() {
        val name = commonTables.removeAt(commonTables.lastIndex)
        if (commonTableCounts.merge(name, -1, Int::plus) == 0) commonTableCounts.remove(name)
    }

    private fun isCommonTable(name: String): Boolean = name in commonTableCounts

    private fun queryTerm(ctx: P.QueryTermContext): Term =
        when (ctx) {
            is P.QueryTermPrimaryContext -> queryPrimary(ctx.queryPrimary())
            is P.QueryTermUnionContext ->
                nested(ctx.UNION().symbol) {
                    val setq = ctx.setQuantifier()?.let(::setQuantifier) ?: SetQuantifier.DISTINCT()
                    val body = queryBodySetOp(setOp(SetOpType.UNION(), setq), false, queryTerm(ctx.lhs).query, queryTerm(ctx.rhs).query)
                    Term(exprQuerySet(body), null, null, null)
                }
            else -> unexpected(ctx)
        }

    private fun queryPrimary(ctx: P.QueryPrimaryContext): Term =
        when (ctx) {
            is P.QueryPrimarySpecificationContext -> {
                val spec = ctx.querySpecification()
                val (query, block) = specification(spec)
                Term(query, spec.selectList().selectItem(), block, spec.topClause())
            }
            is P.QueryPrimaryParenthesizedContext -> query(ctx.query())
            else -> unexpected(ctx)
        }

    /**
     * Adds the ORDER BY, LIMIT, OFFSET, and WITH of [ctx] to [term], its query term. The ORDER BY sees the select list of
     * the term when it is one SELECT block. A parenthesized query that has its own ORDER BY or LIMIT cannot take another.
     */
    private fun clauses(
        ctx: P.QueryContext,
        term: Term,
        with: With?,
    ): ExprQuerySet {
        val orderByCtx = ctx.orderByClause()
        val limitCtx = ctx.limitClause()
        val top = term.top
        val inner = term.query
        if (orderByCtx == null && limitCtx == null && top == null && with == null) return inner
        if (inner.orderBy != null || inner.limit != null || inner.offset != null || inner.with != null) {
            val at: ParserRuleContext = orderByCtx ?: limitCtx ?: ctx
            throw TranslationStop.unsupported(at, "ORDER BY, LIMIT, or OFFSET on a parenthesized query that has its own")
        }
        var limit = limitCtx?.limit?.let(::limitValue)
        val offset = limitCtx?.offset?.let { integer(it, "OFFSET") }
        if (top != null) {
            if (limitCtx?.limit != null) throw TranslationStop.invalid(top, "TOP and LIMIT in the same query")
            limit = integer(top.INTEGER_LITERAL().symbol, "TOP")
        }
        val body = inner.body
        if (isDistinct(body) && (limit != null || offset != null)) {
            val block = term.block ?: throw TranslationStop.unsupported(ctx, "$WRAPPED on a query whose select list is not in view")
            return wrapDistinct(ctx, body as QueryBody.SFW, block, orderByCtx, term.items.orEmpty(), limit, offset, with)
        }
        val sorts = orderByCtx?.sortItem()?.map { sortItem(it, term.items, term.block) }
        return exprQuerySet(body, sorts?.let(::orderBy), limit, offset, with)
    }

    /**
     * PartiQL evaluates DISTINCT after LIMIT and OFFSET; Redshift before them. The DISTINCT query becomes a subquery,
     * and the outer query sorts its rows and limits them.
     *
     * The outer query reads the subquery's columns by name, so each column needs its own: every item of the subquery is
     * given its output name as its alias, or a unique one (`$1`) when several items share a name, and the outer query
     * gives those items the shared name back. With `*` the columns are not known here, so `*` is kept, and the outer
     * query copies the columns by name; that is only right when the names cannot repeat, which is `*` over one FROM
     * item or one `t.*` alone. An ORDER BY key must be an output column of the subquery: an ordinal, an output name, or
     * the expression of an item (Redshift requires an `ORDER BY` key under `DISTINCT` to be in the select list); a name
     * that several items have, with different expressions, is ambiguous, as in Redshift.
     */
    private fun wrapDistinct(
        ctx: P.QueryContext,
        body: QueryBody.SFW,
        block: Block,
        orderByCtx: P.OrderByClauseContext?,
        items: List<P.SelectItemContext>,
        limit: Expr?,
        offset: Expr?,
        with: With?,
    ): ExprQuerySet {
        note(ctx, "DISTINCT with LIMIT or OFFSET was wrapped in a subquery, because PartiQL evaluates DISTINCT after LIMIT")
        val alias = Identifier.Simple.delimited("\$distinct")
        val subquery = exprVarRef(Identifier.of(alias), false)

        // The names are folded already; delimited, the outer query reads exactly the column the subquery named.
        fun column(name: Identifier.Simple): Expr = exprPath(subquery, listOf(exprPathStepField(Identifier.Simple.delimited(name.text))))
        val star = block.selectStar || block.stars.isNotEmpty()
        val inner: QueryBody.SFW
        val outer: Select
        val columns: List<Identifier.Simple>
        if (star) {
            if (block.selectStar && block.relations > 1) {
                throw TranslationStop.unsupported(ctx.queryTerm(), "`*` over several FROM items with $WRAPPED (a column name may repeat)")
            }
            if (block.stars.isNotEmpty() && items.size > 1) {
                throw TranslationStop.unsupported(ctx.queryTerm(), "`t.*` with other select items and $WRAPPED (a column name may repeat)")
            }
            inner = body
            outer = selectStar(null)
            columns = emptyList()
        } else {
            val taken = block.names.map { it.text }.toSet()
            val counts = block.names.groupingBy { it.text }.eachCount()
            columns =
                block.names.mapIndexed { i, name ->
                    if (counts[name.text] == 1) {
                        Identifier.Simple.delimited(name.text)
                    } else {
                        // `$i` for the i-th item, unless an item is named so; then the next free number past the list.
                        val unique = generateSequence(i + 1) { it + block.names.size }.map { "\$$it" }.first { it !in taken }
                        Identifier.Simple.delimited(unique)
                    }
                }
            val select = selectList(block.exprs.mapIndexed { i, expr -> selectItemExpr(expr, columns[i]) }, SetQuantifier.DISTINCT())
            inner = queryBodySFW(select, body.exclude, body.from, body.let, body.where, body.groupBy, body.having, body.window)
            outer = selectList(columns.mapIndexed { i, name -> selectItemExpr(column(name), block.names[i]) }, null)
        }
        val sorts =
            orderByCtx?.sortItem()?.map { sortItem ->
                val expr = sortItem.expr()
                val name = bareName(expr)?.identifier()
                val key: Expr =
                    when {
                        INTEGER.matches(expr.text) -> {
                            val ordinal = ordinal(expr, items)
                            if (star) throw TranslationStop.unsupported(expr, "ordinal $ordinal refers to `*`")
                            column(columns[ordinal - 1])
                        }
                        // An output name wins when the key stands alone.
                        name?.size == 1 && block.named(simple(name[0])).isNotEmpty() -> {
                            val named = block.named(simple(name[0]))
                            checkAmbiguity(block, named, simple(name[0]), expr)
                            column(columns[named[0]])
                        }
                        // Under `*` the columns are not known here: the key must be a column of the relation behind it.
                        star && name != null -> {
                            val relation = block.starRelation()
                            if (name.size > 1 && (relation == null || simple(name[name.size - 2]).text != relation.text || name.size > 2)) {
                                throw TranslationStop.unsupported(expr, "ORDER BY a column of another relation than the `*`, with $WRAPPED")
                            }
                            column(simple(name.last()))
                        }
                        else -> {
                            // Otherwise the key is an input expression, and it is one of the select items.
                            val index = block.exprs.indexOf(expr(expr))
                            if (index < 0) throw TranslationStop.unsupported(expr, "ORDER BY key that is not a select item, with $WRAPPED")
                            column(columns[index])
                        }
                    }
                sort(key, sortItem.dir, sortItem.nulls)
            }
        val from = from(listOf(fromExpr(exprQuerySet(inner), FromType.SCAN(), alias, null)))
        return exprQuerySet(queryBodySFW(outer, from), sorts?.let(::orderBy), limit, offset, with)
    }

    /** Stops when the select items [named] alike are different expressions; see [Block.ambiguous]. */
    private fun checkAmbiguity(
        block: Block,
        named: List<Int>,
        name: Identifier.Simple,
        at: ParserRuleContext,
    ) {
        when (block.ambiguous(named)) {
            Ambiguity.NONE -> {}
            Ambiguity.AMBIGUOUS -> throw TranslationStop.invalid(at, "ORDER BY `${name.text}` is ambiguous")
            Ambiguity.UNCERTAIN ->
                throw TranslationStop.unsupported(
                    at,
                    "ORDER BY `${name.text}` names several select items that may be different columns (ambiguous when they are)",
                )
        }
    }

    /** The name an expression consists of, when it is only a (qualified) name. */
    private fun bareName(ctx: P.ExprContext): P.QualifiedNameContext? = (bareAtom(ctx) as? P.AtomNameContext)?.qualifiedName()

    /** The atom an expression consists of, when it is only an atom. */
    private fun bareAtom(ctx: P.ExprContext): P.AtomContext? {
        val or = ctx.exprOr() as? P.OrBaseContext ?: return null
        val and = or.exprAnd() as? P.AndBaseContext ?: return null
        val not = and.exprNot() as? P.NotBaseContext ?: return null
        val predicate = not.predicate() as? P.PredicateBaseContext ?: return null
        return (predicate.valueExpr() as? P.ValuePrimaryContext)?.atom()
    }

    private fun isDistinct(body: QueryBody): Boolean {
        val select = (body as? QueryBody.SFW)?.select ?: return false
        val setq = (select as? SelectList)?.setq ?: (select as? SelectStar)?.setq
        return setq?.code() == SetQuantifier.DISTINCT
    }

    /** `LIMIT ALL` is the absence of a limit. */
    private fun limitValue(ctx: P.LimitValueContext): Expr? =
        if (ctx.ALL() != null) null else integer(ctx.INTEGER_LITERAL().symbol, "LIMIT")

    /** An integer literal of a clause; Redshift takes a BIGINT, so a larger value is invalid. */
    private fun integer(
        token: Token,
        clause: String,
    ): Expr {
        token.text.toLongOrNull() ?: throw TranslationStop.invalid(token, "$clause value ${token.text} is out of range")
        return exprLit(Literal.intNum(token.text))
    }

    /** Translates a `SELECT ... FROM ... WHERE ... GROUP BY ... HAVING` block; returns it with the block of its select list. */
    private fun specification(ctx: P.QuerySpecificationContext): Pair<ExprQuerySet, Block> {
        val setq = ctx.setQuantifier()?.let(::setQuantifier)
        val fromRef = commaJoin(ctx.fromClause().tableReference())
        val block = Block(fromRef.bindings, ctx.fromClause().tableReference().sumOf(::relations))
        val items = ctx.selectList().selectItem()
        val select: Select =
            if (items.size == 1 && items[0] is P.SelectItemAllContext) {
                block.selectStar = true
                selectStar(setq)
            } else {
                selectList(items.map { selectItem(it, block) }, setq)
            }
        val exclude = ctx.excludeClause()?.let { excludeClause(it, block) }
        val where = ctx.whereClause()?.let { expr(it.expr()) }
        val groupBy =
            ctx.groupByClause()?.let { clause ->
                groupBy(GroupByStrategy.FULL(), clause.expr().map { groupByKey(ordinalOrExpr(it, items), null) }, null)
            }
        val having = ctx.havingClause()?.let { expr(it.expr()) }
        return exprQuerySet(queryBodySFW(select, exclude, from(listOf(fromRef.ref)), null, where, groupBy, having, null)) to block
    }

    private fun selectItem(
        ctx: P.SelectItemContext,
        block: Block,
    ): SelectItem =
        when (ctx) {
            is P.SelectItemAllContext -> throw TranslationStop.unsupported(ctx, "`*` with other select items")
            is P.SelectItemStarContext -> {
                block.stars.add(simple(ctx.qualifiedName().identifier().last()))
                selectItemStar(name(ctx.qualifiedName()))
            }
            is P.SelectItemExprContext -> {
                val expr = expr(ctx.expr())
                val alias = ctx.alias?.let(::label) ?: ctx.bareAlias?.let(::simple)
                block.add(expr, alias)
                selectItemExpr(expr, alias)
            }
            else -> unexpected(ctx)
        }

    /**
     * `* EXCLUDE (c1, c2)`. PartiQL excludes a path from a FROM item (`EXCLUDE t.c1`), so each column is qualified with
     * the relation behind the `*`: the one the query names, or the single FROM item when it names none. A column that
     * the query qualifies itself must name that relation, or a FROM item under `*`; a name that is no FROM item would
     * exclude nothing.
     */
    private fun excludeClause(
        ctx: P.ExcludeClauseContext,
        block: Block,
    ): Exclude {
        if (!block.selectStar && block.stars.size != 1 || block.exprs.isNotEmpty()) {
            throw TranslationStop.unsupported(ctx, "EXCLUDE with select items other than `*`")
        }
        val relation = block.starRelation()
        val bindings = block.bindings.map { it.text }
        val paths =
            ctx.qualifiedName().map { name ->
                val parts = name.identifier().map(::simple)
                val root =
                    if (parts.size > 1) {
                        val qualifier = parts[parts.lastIndex - 1]
                        if (qualifier.text !in bindings) {
                            throw TranslationStop.invalid(
                                name,
                                "EXCLUDE ${name.text}: `${qualifier.text}` is not a FROM item",
                            )
                        }
                        if (block.stars.isNotEmpty() && qualifier.text != relation?.text) {
                            throw TranslationStop.unsupported(name, "EXCLUDE ${name.text} of another relation than the `*`")
                        }
                        qualifier
                    } else {
                        relation
                            ?: throw TranslationStop.unsupported(name, "EXCLUDE of an unqualified column without a single named FROM item")
                    }
                excludePath(exprVarRef(Identifier.of(root), false), listOf(excludeStepStructField(parts.last())))
            }
        return exclude(paths)
    }

    /** A GROUP BY key or ORDER BY sort: an ordinal refers to a select item, which must be an expression. */
    private fun ordinalOrExpr(
        ctx: P.ExprContext,
        items: List<P.SelectItemContext>?,
    ): Expr {
        if (!INTEGER.matches(ctx.text)) return expr(ctx)
        if (items == null) throw TranslationStop.unsupported(ctx, "ordinal after a set operation")
        val ordinal = ordinal(ctx, items)
        return when (val item = items[ordinal - 1]) {
            is P.SelectItemExprContext -> expr(item.expr())
            // The columns behind `*` are known after planning, not here.
            else -> throw TranslationStop.unsupported(ctx, "ordinal $ordinal refers to `*`")
        }
    }

    /** The 1-based ordinal [ctx] holds, which must point into [items]. */
    private fun ordinal(
        ctx: P.ExprContext,
        items: List<P.SelectItemContext>,
    ): Int {
        val ordinal = ctx.text.toIntOrNull()
        if (ordinal == null || ordinal !in 1..items.size) {
            throw TranslationStop.invalid(ctx, "ordinal ${ctx.text} is out of range of the select list")
        }
        return ordinal
    }

    /**
     * A sort key. A key that is only the output name of a select item (its alias, or the name of the column it selects)
     * is that item's expression: an output name wins in ORDER BY, but only when it stands alone. Inside an expression a
     * name is an input column, as in Redshift. A name that several items have is ambiguous when their expressions
     * differ, as in Redshift.
     */
    private fun sortItem(
        ctx: P.SortItemContext,
        items: List<P.SelectItemContext>?,
        block: Block?,
    ): Sort {
        val name = bareName(ctx.expr())?.identifier()?.singleOrNull()?.let(::simple)
        if (name != null && block != null) {
            val named = block.named(name)
            if (named.isNotEmpty()) {
                checkAmbiguity(block, named, name, ctx.expr())
                return sort(block.exprs[named[0]], ctx.dir, ctx.nulls)
            }
        }
        return sort(ordinalOrExpr(ctx.expr(), items), ctx.dir, ctx.nulls)
    }

    /** Redshift sorts NULL last in ascending order and first in descending order; PartiQL is told so explicitly. */
    private fun sort(
        key: Expr,
        dir: Token?,
        nulls: Token?,
    ): Sort {
        val descending = dir?.type == P.DESC
        val nullsOrder =
            when (nulls?.type) {
                P.FIRST -> Nulls.FIRST()
                P.LAST -> Nulls.LAST()
                else -> if (descending) Nulls.FIRST() else Nulls.LAST()
            }
        return sort(key, if (descending) Order.DESC() else Order.ASC(), nullsOrder)
    }

    private fun setQuantifier(ctx: P.SetQuantifierContext): SetQuantifier =
        if (ctx.DISTINCT() != null) SetQuantifier.DISTINCT() else SetQuantifier.ALL()

    /*
     * FROM
     */

    /** A translated FROM item with the names it binds. */
    private class Ref(val ref: FromTableRef, val bindings: List<Identifier.Simple>)

    /** The FROM list as a chain of `CROSS JOIN`s; each item after the first is a level of the chain. */
    private fun commaJoin(items: List<P.TableReferenceContext>): Ref {
        fun join(last: Int): Ref =
            if (last == 0) {
                tableReference(items[0])
            } else {
                nested(items[last].start) {
                    val lhs = join(last - 1)
                    val rhs = tableReference(items[last])
                    Ref(fromJoin(lhs.ref, rhs.ref, JoinType.CROSS(), null), lhs.bindings + rhs.bindings)
                }
            }
        return join(items.lastIndex)
    }

    /** The number of tables and subqueries in a FROM item, through its joins. */
    private fun relations(ctx: P.TableReferenceContext): Int =
        when (ctx) {
            is P.TableBaseContext -> relations(ctx.tablePrimary())
            is P.TableCrossJoinContext -> relations(ctx.lhs) + relations(ctx.rhs)
            is P.TableJoinContext -> relations(ctx.lhs) + relations(ctx.rhs)
            else -> 1
        }

    private fun relations(ctx: P.TablePrimaryContext): Int = if (ctx is P.TableParenthesizedContext) relations(ctx.tableReference()) else 1

    private fun tableReference(ctx: P.TableReferenceContext): Ref =
        when (ctx) {
            is P.TableBaseContext -> tablePrimary(ctx.tablePrimary())
            is P.TableCrossJoinContext ->
                nested(ctx.rhs.start) {
                    val lhs = tableReference(ctx.lhs)
                    val rhs = tablePrimary(ctx.rhs)
                    Ref(fromJoin(lhs.ref, rhs.ref, JoinType.CROSS(), null), lhs.bindings + rhs.bindings)
                }
            is P.TableJoinContext ->
                nested(ctx.rhs.start) {
                    val lhs = tableReference(ctx.lhs)
                    val rhs = tablePrimary(ctx.rhs)
                    Ref(fromJoin(lhs.ref, rhs.ref, joinType(ctx.joinType()), expr(ctx.condition)), lhs.bindings + rhs.bindings)
                }
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

    private fun tablePrimary(ctx: P.TablePrimaryContext): Ref =
        when (ctx) {
            is P.TableSubqueryContext -> {
                val alias = ctx.tableAlias()?.let(::tableAlias)
                Ref(fromExpr(querySet(ctx.query()), FromType.SCAN(), alias, null), listOfNotNull(alias))
            }
            is P.TableParenthesizedContext -> {
                ctx.tableAlias()?.let { throw TranslationStop.unsupported(it, "alias of a parenthesized join") }
                tableReference(ctx.tableReference())
            }
            is P.TableNameContext -> {
                val parts = ctx.qualifiedName().identifier().map(::simple)
                val alias = ctx.tableAlias()?.let(::tableAlias)
                val isCommonTable = parts.size == 1 && isCommonTable(parts[0].text)
                if (!isCommonTable) tables.add(parts.map { it.text })
                Ref(fromExpr(exprVarRef(Identifier.of(parts), isCommonTable), FromType.SCAN(), alias, null), listOf(alias ?: parts.last()))
            }
            else -> unexpected(ctx)
        }

    private fun tableAlias(ctx: P.TableAliasContext): Identifier.Simple {
        ctx.columnAliases()?.let { throw TranslationStop.unsupported(it, "column aliases of a FROM item") }
        return simple(ctx.alias)
    }

    /*
     * Expressions
     */

    fun expr(ctx: P.ExprContext): Expr = exprOr(ctx.exprOr())

    // Each operator of a chain (`a AND b AND c`) is a level of the AST; see [nested].

    private fun exprOr(ctx: P.ExprOrContext): Expr =
        when (ctx) {
            is P.OrContext -> nested(ctx.OR().symbol) { exprOr(exprOr(ctx.lhs), exprAnd(ctx.rhs)) }
            is P.OrBaseContext -> exprAnd(ctx.exprAnd())
            else -> unexpected(ctx)
        }

    private fun exprAnd(ctx: P.ExprAndContext): Expr =
        when (ctx) {
            is P.AndContext -> nested(ctx.AND().symbol) { exprAnd(exprAnd(ctx.lhs), exprNot(ctx.rhs)) }
            is P.AndBaseContext -> exprNot(ctx.exprNot())
            else -> unexpected(ctx)
        }

    private fun exprNot(ctx: P.ExprNotContext): Expr =
        when (ctx) {
            is P.NotContext -> nested(ctx.NOT().symbol) { exprNot(exprNot(ctx.exprNot())) }
            is P.NotBaseContext -> predicate(ctx.predicate())
            else -> unexpected(ctx)
        }

    private fun predicate(ctx: P.PredicateContext): Expr =
        when (ctx) {
            is P.PredicateComparisonContext -> {
                val op = if (ctx.op.text == "!=") "<>" else ctx.op.text
                exprOperator(op, valueExpr(ctx.lhs), valueExpr(ctx.rhs))
            }
            is P.PredicateNullContext -> exprNullPredicate(valueExpr(ctx.lhs), ctx.NOT() != null)
            is P.PredicateNullPostfixContext -> exprNullPredicate(valueExpr(ctx.lhs), ctx.op.type == P.NOTNULL)
            is P.PredicateTruthContext -> {
                val truth =
                    when (ctx.truth.type) {
                        P.TRUE -> TruthValue.TRUE()
                        P.FALSE -> TruthValue.FALSE()
                        else -> TruthValue.UNKNOWN()
                    }
                exprBoolTest(valueExpr(ctx.lhs), ctx.NOT() != null, truth)
            }
            is P.PredicateBetweenContext -> exprBetween(valueExpr(ctx.lhs), valueExpr(ctx.lower), valueExpr(ctx.upper), ctx.NOT() != null)
            is P.PredicateInQueryContext -> exprInCollection(valueExpr(ctx.lhs), querySet(ctx.query()), ctx.NOT() != null)
            is P.PredicateInListContext -> {
                // The PartiQL printer does not print a subquery inside a list.
                ctx.expr().forEach {
                    if (bareAtom(
                            it,
                        ) is P.AtomSubqueryContext
                    ) {
                        throw TranslationStop.unsupported(it, "subquery as an element of an IN list")
                    }
                }
                exprInCollection(valueExpr(ctx.lhs), exprArray(ctx.expr().map(::expr)), ctx.NOT() != null)
            }
            is P.PredicateLikeContext -> like(ctx)
            is P.PredicateBaseContext -> valueExpr(ctx.valueExpr())
            else -> unexpected(ctx)
        }

    /**
     * `LIKE` and `ILIKE`. Redshift escapes with a backslash when there is no `ESCAPE` clause, takes an escaped
     * character of any kind literally, and takes `ESCAPE ''` as no escape character; PartiQL escapes with nothing by
     * default and takes an escape only before `%`, `_`, and the escape character itself. A literal pattern is therefore
     * rewritten to the form PartiQL takes, escaping with a backslash only where needed; see [canonicalPattern]. A
     * pattern that is not a literal is kept with Redshift's escape character.
     *
     * `ILIKE` is `LOWER(a) LIKE b` with a literal pattern lowered here (lowering cannot create an escape character once
     * the pattern is in canonical form), or `LOWER(a) LIKE LOWER(b)` for another pattern, whose escape character must
     * then not be a letter.
     */
    private fun like(ctx: P.PredicateLikeContext): Expr {
        val not = ctx.NOT() != null
        val lhs = valueExpr(ctx.lhs)
        val pattern = valueExpr(ctx.rhs)
        val escape = ctx.escape?.let(::valueExpr)
        val escapeLiteral = (escape as? ExprLit)?.lit?.takeIf { it.code() == Literal.STRING }?.stringValue()
        if (escapeLiteral != null && escapeLiteral.length > 1) {
            throw TranslationStop.invalid(
                ctx.escape,
                "ESCAPE must be a single character",
            )
        }
        val patternLiteral = (pattern as? ExprLit)?.lit?.takeIf { it.code() == Literal.STRING }?.stringValue()
        val insensitive = ctx.op.type == P.ILIKE
        if (insensitive && escape != null && escapeLiteral == null) {
            throw TranslationStop.unsupported(ctx.escape, "ILIKE with an escape character that is not a string literal")
        }
        val subject = if (insensitive) lower(lhs) else lhs
        if (patternLiteral != null && (escape == null || escapeLiteral != null)) {
            // The escape character: a backslash by default, none with ESCAPE ''.
            val escapeChar = if (escapeLiteral == null) '\\' else escapeLiteral.singleOrNull()
            val written = canonicalPattern(patternLiteral, escapeChar, insensitive, ctx.rhs)
            return exprLike(subject, exprLit(Literal.string(written)), if ('\\' in written) BACKSLASH else null, not)
        }
        // The pattern, or the escape character, is known at run time only, so Redshift's escape character is kept.
        if (insensitive && escapeLiteral?.singleOrNull()?.isLetter() == true) {
            throw TranslationStop.unsupported(ctx.escape, "ILIKE with a letter as the escape character and a pattern that is not a literal")
        }
        val written =
            when {
                escapeLiteral == "" -> null
                escape != null -> escape
                else -> BACKSLASH
            }
        return exprLike(subject, if (insensitive) lower(pattern) else pattern, written, not)
    }

    /**
     * A `LIKE` pattern that escapes with [escape], written so that it escapes with a backslash and only where PartiQL
     * takes an escape: before `%`, `_`, and `\`. An escaped other character is that character, and a backslash of the
     * pattern is doubled. With [lower] the pattern is folded to lower case, which cannot create an escape character.
     */
    private fun canonicalPattern(
        pattern: String,
        escape: Char?,
        lower: Boolean,
        at: ParserRuleContext,
    ): String {
        val out = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i++]
            val literal =
                when {
                    c == escape -> {
                        if (i == pattern.length) throw TranslationStop.invalid(at, "LIKE pattern must not end with the escape character")
                        val escaped = pattern[i++]
                        if (escaped in "%_\\") out.append('\\')
                        escaped
                    }
                    c == '\\' -> {
                        out.append('\\')
                        c
                    }
                    else -> c
                }
            out.append(if (lower) literal.lowercase() else literal.toString())
        }
        return out.toString()
    }

    private fun lower(expr: Expr): Expr = exprCall(Identifier.regular("lower"), listOf(expr), null)

    private fun valueExpr(ctx: P.ValueExprContext): Expr =
        when (ctx) {
            is P.ValuePrimaryContext -> atom(ctx.atom())
            is P.ValueCastShorthandContext ->
                nested(
                    ctx.DOUBLE_COLON().symbol,
                ) { exprCast(valueExpr(ctx.valueExpr()), dataType(ctx.dataType())) }
            is P.ValueSignedContext -> nested(ctx.sign) { exprOperator(ctx.sign.text, null, valueExpr(ctx.valueExpr())) }
            // PartiQL has no counterpart of `^` and `&` yet; the operators are translated with the functions, by a later change.
            is P.ValueExponentContext -> throw TranslationStop.unsupported(ctx.op, "operator `^` is not translated yet")
            is P.ValueMultiplicativeContext -> nested(ctx.op) { exprOperator(ctx.op.text, valueExpr(ctx.lhs), valueExpr(ctx.rhs)) }
            is P.ValueAdditiveContext -> nested(ctx.op) { exprOperator(ctx.op.text, valueExpr(ctx.lhs), valueExpr(ctx.rhs)) }
            is P.ValueOperatorContext ->
                if (ctx.op.type == P.CONCAT) {
                    nested(ctx.op) { exprOperator("||", valueExpr(ctx.lhs), valueExpr(ctx.rhs)) }
                } else {
                    throw TranslationStop.unsupported(ctx.op, "operator `&` is not translated yet")
                }
            else -> unexpected(ctx)
        }

    private fun atom(ctx: P.AtomContext): Expr =
        when (ctx) {
            is P.AtomLiteralContext -> literal(ctx.literal())
            is P.AtomCastContext -> exprCast(expr(ctx.expr()), dataType(ctx.dataType()))
            is P.AtomConvertContext -> exprCast(expr(ctx.expr()), dataType(ctx.dataType()))
            is P.AtomCaseContext ->
                nested(ctx.start) {
                    exprCase(
                        ctx.operand?.let(::expr),
                        ctx.caseWhen().map { exprCaseBranch(expr(it.condition), expr(it.result)) },
                        ctx.otherwise?.let(::expr),
                    )
                }
            is P.AtomCountStarContext -> exprCall(Identifier.regular("count"), emptyList(), null)
            is P.AtomAggregateContext ->
                exprCall(Identifier.regular(foldAscii(ctx.name.text)), listOf(expr(ctx.expr())), ctx.setQuantifier()?.let(::setQuantifier))
            is P.AtomFunctionCallContext -> function(ctx.name.start, ctx.expr())
            // The datetime functions are translated by a later change, each with its own rule.
            is P.AtomDateFunctionContext -> notYet(ctx.name)
            is P.AtomExtractEpochContext -> notYet(ctx.EXTRACT().symbol)
            is P.AtomGetdateContext -> notYet(ctx.GETDATE().symbol)
            is P.AtomSysdateContext -> notYet(ctx.SYSDATE().symbol)
            is P.AtomCurrentDateContext -> notYet(ctx.CURRENT_DATE().symbol)
            is P.AtomSubqueryContext -> querySet(ctx.query())
            is P.AtomParenthesizedContext -> expr(ctx.expr())
            is P.AtomNameContext -> name(ctx.qualifiedName())
            else -> unexpected(ctx)
        }

    /**
     * A scalar function call. Each family of functions has its own rules: the conditional functions in
     * [ConditionalFunctions]; the string, math, and datetime functions are translated by later changes.
     */
    private fun function(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr = conditional.translate(name, args) ?: notYet(name)

    private fun notYet(name: Token): Nothing =
        throw TranslationStop.unsupported(
            name,
            "function ${name.text.uppercase()} is not translated yet",
        )

    /** A name as a variable reference with field steps: `t.a` is the field `a` of the variable `t`. */
    private fun name(ctx: P.QualifiedNameContext): Expr {
        val parts = ctx.identifier().map(::simple)
        val root: Expr = exprVarRef(Identifier.of(parts[0]), false)
        return if (parts.size == 1) root else exprPath(root, parts.drop(1).map(::exprPathStepField))
    }

    private fun literal(ctx: P.LiteralContext): Expr =
        when (ctx) {
            is P.LiteralNullContext -> exprLit(Literal.nul())
            is P.LiteralTrueContext -> exprLit(Literal.bool(true))
            is P.LiteralFalseContext -> exprLit(Literal.bool(false))
            // Redshift's widest number is DECIMAL(38); a longer literal would be rounded by the planner.
            is P.LiteralIntegerContext -> exprLit(Literal.intNum(digits(ctx)))
            is P.LiteralDecimalContext -> exprLit(Literal.exactNum(digits(ctx)))
            is P.LiteralStringContext -> exprLit(Literal.string(stringValue(ctx.STRING_LITERAL().symbol)))
            is P.LiteralDateContext -> {
                val value = stringValue(ctx.STRING_LITERAL().symbol)
                if (!DATE.matches(value)) throw TranslationStop.unsupported(ctx, "DATE literal format '$value'")
                exprLit(Literal.typedString(DataType.DATE(), value))
            }
            is P.LiteralTimestampContext -> {
                val value = stringValue(ctx.STRING_LITERAL().symbol)
                if (!TIMESTAMP.matches(value)) throw TranslationStop.unsupported(ctx, "TIMESTAMP literal format '$value'")
                exprLit(Literal.typedString(DataType.TIMESTAMP(), value.replace('T', ' ')))
            }
            // Datetime arithmetic is translated by a later change.
            is P.LiteralIntervalContext -> throw TranslationStop.unsupported(ctx, "INTERVAL literal")
            else -> unexpected(ctx)
        }

    /** The text of a numeric literal of at most 38 digits. */
    private fun digits(ctx: P.LiteralContext): String {
        val text = ctx.text
        if (text.count { it.isDigit() } > 38) throw TranslationStop.unsupported(ctx, "numeric literal with more than 38 digits")
        return text
    }

    /**
     * The value of a string literal token. Two quotes are a quote. A backslash is an escape character in Redshift string
     * literals: two backslashes are a backslash, `\'` is a quote, and `\b`, `\f`, `\n`, `\r`, `\t` are the control
     * characters. Other escapes are not documented, so they are unsupported.
     */
    private fun stringValue(token: Token): String {
        val text = token.text
        val value = StringBuilder()
        var i = 1
        while (i < text.length - 1) {
            when (val c = text[i]) {
                '\'' -> {
                    value.append('\'')
                    i += 2
                }
                '\\' -> {
                    val escaped =
                        ESCAPES[text[i + 1]]
                            ?: throw TranslationStop.unsupported(token, "backslash escape `\\${text[i + 1]}` in a string literal")
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

    /*
     * Types
     */

    /**
     * The PartiQL type of a Redshift type, with the Redshift defaults and bounds: `VARCHAR` is `VARCHAR(256)` and `TEXT`
     * is the same; `CHAR` is `CHAR(1)` and `BPCHAR` is `CHAR(256)`; `VARCHAR(MAX)` is the unbounded string and
     * `CHAR(MAX)` is `CHAR(4096)`; `DECIMAL` is `DECIMAL(18, 0)`. A length or precision outside Redshift's range is
     * invalid. Redshift measures a character length in bytes and PartiQL in characters; the number is kept.
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */
    private fun dataType(ctx: P.DataTypeContext): DataType =
        when (ctx) {
            is P.TypeVarcharContext -> {
                val length = ctx.length
                when {
                    length == null -> DataType.VARCHAR(256)
                    length.MAX() != null -> DataType.STRING()
                    else -> DataType.VARCHAR(bounded(length.INTEGER_LITERAL().symbol, 1..65535, "VARCHAR length"))
                }
            }
            is P.TypeCharContext -> {
                val length = ctx.length
                when {
                    length == null -> DataType.CHAR(if (ctx.name.type == P.BPCHAR) 256 else 1)
                    length.MAX() != null -> DataType.CHAR(4096)
                    else -> DataType.CHAR(bounded(length.INTEGER_LITERAL().symbol, 1..4096, "CHAR length"))
                }
            }
            is P.TypeDoubleContext -> DataType.DOUBLE_PRECISION()
            is P.TypeRealContext -> DataType.REAL()
            is P.TypeDecimalContext -> {
                val precision = ctx.precision?.let { bounded(it, 1..38, "DECIMAL precision") } ?: 18
                val scale = ctx.scale?.let { bounded(it, 0..minOf(precision, 37), "DECIMAL scale") } ?: 0
                DataType.DECIMAL(precision, scale)
            }
            is P.TypeIntegerContext ->
                when (ctx.name.type) {
                    P.SMALLINT, P.INT2 -> DataType.SMALLINT()
                    P.BIGINT, P.INT8 -> DataType.BIGINT()
                    else -> DataType.INT()
                }
            is P.TypeTimestampContext -> DataType.TIMESTAMP()
            is P.TypeDateContext -> DataType.DATE()
            is P.TypeBooleanContext -> DataType.BOOL()
            else -> unexpected(ctx)
        }

    /*
     * Identifiers
     */

    /**
     * A name. An unquoted name folds its ASCII letters to lower case. A quoted name keeps its case only with
     * `enable_case_sensitive_identifier`; otherwise Redshift folds it too.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
     */
    fun simple(ctx: P.IdentifierContext): Identifier.Simple =
        when (ctx) {
            is P.IdentifierQuotedContext -> {
                val text = ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
                Identifier.Simple.delimited(if (caseSensitiveIdentifiers) text else foldAscii(text))
            }
            else -> Identifier.Simple.regular(foldAscii(ctx.text))
        }

    /** The alias after `AS` in the select list, which may be a reserved word; a reserved word is delimited in PartiQL. */
    private fun label(ctx: P.ColumnLabelContext): Identifier.Simple =
        ctx.identifier()?.let(::simple) ?: Identifier.Simple.delimited(foldAscii(ctx.text))

    private fun fold(ctx: P.IdentifierContext): String = simple(ctx).text

    private fun note(
        ctx: ParserRuleContext,
        message: String,
    ) {
        listener.report(TranslationStop.info(ctx.start.line, ctx.start.charPositionInLine + 1, message))
    }

    /** The value of an integer token that Redshift bounds. */
    private fun bounded(
        token: Token,
        range: IntRange,
        what: String,
    ): Int {
        val value = token.text.toIntOrNull()
        if (value == null || value !in range) {
            throw TranslationStop.invalid(token, "$what ${token.text} is out of range (${range.first} to ${range.last})")
        }
        return value
    }

    /** A parse tree node the builder has no rule for; the grammar and the builder have drifted apart. */
    private fun unexpected(ctx: ParserRuleContext): Nothing =
        throw TranslationStop.unsupported(ctx, "${ctx.text} (no conversion rule for ${ctx::class.simpleName})")

    private companion object {
        const val WRAPPED = "DISTINCT and LIMIT or OFFSET"
        val BACKSLASH: Expr = exprLit(Literal.string("\\"))
        val INTEGER = Regex("""\d+""")
        val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        val TIMESTAMP = Regex("""\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(\.\d{1,6})?""")

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
