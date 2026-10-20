package org.partiql.scribe.frontend.redshift.internal

import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectItemStar
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.sort
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.AstVisitor
import org.partiql.ast.From
import org.partiql.ast.FromType
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.WindowPartition
import org.partiql.ast.WindowSpecification
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.ExprWindowFunction
import org.partiql.ast.expr.PathStep
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Assembles the PartiQL query of a Redshift query block from its translated clauses.
 *
 * Two differences between the languages need more than one PartiQL query block:
 *  - PartiQL evaluates the SELECT clause and DISTINCT after ORDER BY, LIMIT, and OFFSET; Redshift evaluates DISTINCT
 *    before them. A DISTINCT query with LIMIT or OFFSET becomes a DISTINCT subquery with an outer ORDER BY and LIMIT.
 *  - PartiQL has no QUALIFY, and its planner does not allow a window function in ORDER BY. The window functions become
 *    columns of a subquery that the outer query filters and sorts.
 *
 * The subquery names its columns with the output names, or with `$c1`, `$c2`, ... when output names repeat, because
 * the outer query refers to the columns by name.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_QUALIFY_clause.html
 * https://docs.aws.amazon.com/redshift/latest/dg/r_ORDER_BY_clause.html
 */
internal class RedshiftQueryBlocks(
    private val builder: RedshiftAstBuilder,
    private val scope: Scope,
    private val generated: PassNames,
) {
    /** A select item: its output name and translation, or the FROM item of an unexpanded `*`. */
    class Column(
        val name: Identifier.Simple,
        val expr: Expr?,
        val star: Binding?,
        val ctx: P.SelectItemContext,
    )

    class Parts(
        val items: List<Column>,
        val from: From,
        val where: Expr?,
        val keys: List<Expr>?,
        val having: Expr?,
        val qualify: Expr?,
        val qualifyCtx: P.QualifyClauseContext?,
        val sorts: List<RedshiftAstBuilder.SortKey>?,
        val limit: Expr?,
        val offset: Expr?,
        val distinct: Boolean,
        val distinctCtx: P.SetQuantifierContext?,
        val spec: P.QuerySpecificationContext,
    )

    fun assemble(
        parts: Parts,
        itemType: (P.SelectItemContext, Expr) -> Expr,
    ): RedshiftAstBuilder.Translated {
        val windowSort = parts.sorts?.any { containsWindowFunction(it.sort.expr) } == true
        val distinctLimit = parts.distinct && (parts.limit != null || parts.offset != null)
        val wrap = parts.qualify != null || windowSort || distinctLimit
        val names = parts.items.map { it.name }
        var inner = names
        var alias: Identifier.Simple? = null
        // The QUALIFY condition of the outer query, with references to the columns of the subquery.
        val hidden = mutableListOf<Pair<Identifier.Simple, Expr>>()
        var where: Expr? = null
        if (wrap) {
            val construct =
                when {
                    parts.qualify != null -> "QUALIFY"
                    windowSort -> "ORDER BY of a window function"
                    else -> "SELECT DISTINCT with LIMIT or OFFSET"
                }
            parts.items.firstOrNull { it.star != null }?.let {
                throw UnsupportedConstructException.at(it.ctx, "$construct with `*` of a FROM item with unknown columns")
            }
            if (names.map { it.text }.toSet().size != names.size) inner = names.indices.map { generated.fresh("\$c${it + 1}") }
            alias =
                generated.fresh(
                    when {
                        parts.qualify != null -> "\$q"
                        windowSort -> "\$w"
                        else -> "\$d"
                    },
                )
            where = parts.qualify?.let { QualifySplit(parts.items, inner, alias, hidden).outer(it) }
        }

        // GROUP BY keys and their references after the aggregation.
        var items = parts.items
        var having = parts.having
        var sorts = parts.sorts?.map { it.sort }
        var hiddenExprs: List<Pair<Identifier.Simple, Expr>> = hidden
        val groupBy =
            parts.keys?.let { keys ->
                val aliases = keys.map { generated.fresh("\$k") }
                val substitution = KeySubstitution(keys, aliases, scope, builder::bindingOf)
                items = items.map { Column(it.name, it.expr?.let(substitution::apply), it.star, it.ctx) }
                having = having?.let(substitution::apply)
                hiddenExprs = hiddenExprs.map { (name, expr) -> name to substitution.apply(expr) }
                sorts = sorts?.map { sort(substitution.apply(it.expr), it.order, it.nulls) }
                groupBy(GroupByStrategy.FULL(), keys.zip(aliases).map { (key, keyAlias) -> groupByKey(key, keyAlias) }, null)
            }
        if (!wrap) {
            val select = select(items, itemType, if (parts.distinct) SetQuantifier.DISTINCT() else null)
            val body = queryBodySFW(select, null, parts.from, null, parts.where, groupBy, having)
            val columns = if (items.any { it.star != null }) null else items.map { it.name }
            return RedshiftAstBuilder.Translated(exprQuerySet(body, sorts?.let(::orderBy), parts.limit, parts.offset), columns)
        }

        // The subquery computes the items, the parts of the QUALIFY condition, and the sort expressions that are not
        // items.
        val hiddenSorts = mutableListOf<Pair<Identifier.Simple, Expr>>()
        val sortColumns =
            sorts?.mapIndexed { i, s ->
                val index = parts.sorts!![i].item ?: items.indexOfFirst { it.expr == s.expr }
                when {
                    index >= 0 -> inner[index]
                    parts.distinct ->
                        throw InvalidQueryException.at(
                            parts.sorts!![i].ctx,
                            "for SELECT DISTINCT, ORDER BY expressions must appear in the select list",
                        )
                    else -> generated.fresh("\$o").also { hiddenSorts.add(it to s.expr) }
                }
            }
        val innerItems =
            items.mapIndexed { i, item -> selectItemExpr(itemType(item.ctx, item.expr!!), inner[i]) } +
                (hiddenExprs + hiddenSorts).map { (name, expr) -> selectItemExpr(expr, name) }
        val innerDistinct = if (distinctLimit && parts.qualify == null) SetQuantifier.DISTINCT() else null
        val innerBody = queryBodySFW(selectList(innerItems, innerDistinct), null, parts.from, null, parts.where, groupBy, having)
        var source = exprQuerySet(innerBody, null, null, null)
        var outerAlias = alias!!
        when {
            parts.qualify != null -> builder.note(parts.qualifyCtx!!.start, "Redshift `QUALIFY` was replaced by a filter on a subquery")
            windowSort ->
                builder.note(
                    parts.sorts!!.first().ctx.start,
                    "The query was moved into a subquery so that ORDER BY can use the window function result",
                )
        }
        if (distinctLimit) {
            builder.note(
                parts.distinctCtx!!.start,
                "Redshift `SELECT DISTINCT` with LIMIT or OFFSET was moved into a subquery, " +
                    "because PartiQL applies DISTINCT after LIMIT and OFFSET",
            )
        }
        if (distinctLimit && parts.qualify != null) {
            // SELECT DISTINCT over the filtered rows, then sort and limit them.
            val distinctItems = inner.map { selectItemExpr(field(outerAlias, it), it) }
            val distinctBody =
                queryBodySFW(
                    selectList(distinctItems, SetQuantifier.DISTINCT()),
                    null,
                    from(listOf(fromExpr(source, FromType.SCAN(), outerAlias, null))),
                    null,
                    where,
                )
            source = exprQuerySet(distinctBody, null, null, null)
            outerAlias = generated.fresh("\$d")
            where = null
        }
        val outerItems = inner.mapIndexed { i, name -> selectItemExpr(field(outerAlias, name), names[i]) }
        val outerDistinct = if (parts.distinct && !distinctLimit) SetQuantifier.DISTINCT() else null
        val outerSorts = sorts?.mapIndexed { i, s -> sort(field(outerAlias, sortColumns!![i]), s.order, s.nulls) }
        val outerBody =
            queryBodySFW(
                selectList(outerItems, outerDistinct),
                null,
                from(listOf(fromExpr(source, FromType.SCAN(), outerAlias, null))),
                null,
                where,
            )
        return RedshiftAstBuilder.Translated(exprQuerySet(outerBody, outerSorts?.let(::orderBy), parts.limit, parts.offset), names)
    }

    /**
     * Splits a QUALIFY condition into the condition of the outer query and the columns of the subquery: a reference to
     * a select-list alias is the column of the item, so that the condition uses the same value as the item (a window
     * function with ties may number the rows differently each time it is computed); a window function, an aggregate,
     * a subquery, or a column is a hidden column of the subquery; the rest of the condition is in the outer query.
     */
    private inner class QualifySplit(
        private val items: List<Column>,
        private val inner: List<Identifier.Simple>,
        private val alias: Identifier.Simple,
        private val hidden: MutableList<Pair<Identifier.Simple, Expr>>,
    ) {
        fun outer(condition: Expr): Expr = rewriter.visitExpr(condition, Unit) as Expr

        private val rewriter =
            object : IdentityRewriter<Unit>() {
                override fun visitExpr(
                    node: Expr,
                    ctx: Unit,
                ): AstNode {
                    val i = items.indexOfFirst { it.expr === node }
                    if (i >= 0) return field(alias, inner[i])
                    if (node is ExprPath) {
                        val j = items.indexOfFirst { it.expr === node.root }
                        if (j >= 0) return exprPath(field(alias, inner[j]), node.steps)
                    }
                    val atomic =
                        node is ExprWindowFunction || node is ExprQuerySet || node is ExprVarRef || node is ExprPath ||
                            (node is ExprCall && KeySubstitution.isAggregate(node))
                    if (atomic || !containsItem(node)) {
                        if (isConstant(node)) return node
                        val name = generated.fresh("\$qualify")
                        hidden.add(name to node)
                        return field(alias, name)
                    }
                    return super.visitExpr(node, ctx)
                }
            }

        private fun containsItem(expr: Expr): Boolean {
            var found = false
            val walker =
                object : AstWalker() {
                    override fun defaultVisit(
                        node: AstNode,
                        ctx: Unit,
                    ) {
                        if (items.any { it.expr === node }) found = true
                        if (!found) super.defaultVisit(node, ctx)
                    }
                }
            expr.accept(walker, Unit)
            return found
        }

        private fun isConstant(expr: Expr): Boolean {
            var constant = true
            val walker =
                object : AstWalker() {
                    override fun defaultVisit(
                        node: AstNode,
                        ctx: Unit,
                    ) {
                        val variable =
                            node is ExprVarRef || node is ExprQuerySet || node is ExprWindowFunction ||
                                (node is ExprCall && KeySubstitution.isAggregate(node))
                        if (variable) constant = false
                        if (constant) super.defaultVisit(node, ctx)
                    }
                }
            expr.accept(walker, Unit)
            return constant
        }
    }

    private fun select(
        items: List<Column>,
        itemType: (P.SelectItemContext, Expr) -> Expr,
        setq: SetQuantifier?,
    ) = if (items.size == 1 && items[0].star != null && items[0].ctx is P.SelectItemAllContext) {
        selectStar(setq)
    } else {
        selectList(
            items.map { item ->
                val star = item.star
                if (star != null) {
                    selectItemStar(
                        exprVarRef(Identifier.of(star.alias), false),
                    )
                } else {
                    selectItemExpr(itemType(item.ctx, item.expr!!), item.name)
                }
            },
            setq,
        )
    }

    private fun field(
        alias: Identifier.Simple,
        column: Identifier.Simple,
    ): Expr = exprPath(exprVarRef(Identifier.of(alias), false), listOf(exprPathStepField(column)))

    companion object {
        /** True if [expr] contains an aggregate call or a window function outside its subqueries. */
        fun containsAggregateOrWindow(expr: Expr): Boolean {
            var found = false
            val visitor =
                object : AstWalker() {
                    override fun defaultVisit(
                        node: AstNode,
                        ctx: Unit,
                    ) {
                        when {
                            node is ExprQuerySet -> return
                            node is ExprWindowFunction || (node is ExprCall && KeySubstitution.isAggregate(node)) -> found = true
                            else -> super.defaultVisit(node, ctx)
                        }
                    }
                }
            expr.accept(visitor, Unit)
            return found
        }

        /** True if [expr] contains a window function outside its subqueries. */
        fun containsWindowFunction(expr: Expr): Boolean {
            var found = false
            val visitor =
                object : AstWalker() {
                    @Deprecated("This feature is experimental and is subject to change.")
                    override fun visitExprWindowFunction(
                        node: ExprWindowFunction,
                        ctx: Unit,
                    ) {
                        found = true
                    }

                    override fun visitExprQuerySet(
                        node: ExprQuerySet,
                        ctx: Unit,
                    ) {}
                }
            expr.accept(visitor, Unit)
            return found
        }
    }
}

/**
 * Replaces each reference to a GROUP BY key after the aggregation (in the select list, HAVING, QUALIFY, ORDER BY, and
 * window specifications) with the alias of the key.
 *
 * The PartiQL planner matches select items with the keys, but it does not match HAVING and ORDER BY expressions:
 * `GROUP BY UPPER(t.c) ORDER BY UPPER(t.c)` does not plan, and `t.c` in HAVING resolves to an outer FROM item `t` if
 * there is one. A reference is an expression equal to a key, or a path that extends a key, outside the aggregate calls
 * of the query block, whose variables are FROM items of the block.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_GROUP_BY_clause.html
 */
internal class KeySubstitution(
    private val keys: List<Expr>,
    private val aliases: List<Identifier.Simple>,
    private val scope: Scope,
    private val bindingOf: (ExprVarRef) -> Binding?,
) {
    private val partitionNames = keys.map { qualifiedName(it) }

    fun apply(expr: Expr): Expr = rewriter.visitExpr(expr, 0) as Expr

    private val rewriter =
        object : IdentityRewriter<Int>() {
            override fun visitExpr(
                node: Expr,
                ctx: Int,
            ): AstNode = replace(node) ?: super.visitExpr(node, ctx)

            override fun visitExprCall(
                node: ExprCall,
                ctx: Int,
            ): AstNode = if (ctx == 0 && isAggregate(node)) node else super.visitExprCall(node, ctx)

            override fun visitExprQuerySet(
                node: ExprQuerySet,
                ctx: Int,
            ): AstNode = super.visitExprQuerySet(node, ctx + 1)

            @Deprecated("This feature is experimental and is subject to change.")
            override fun visitWindowPartition(
                node: WindowPartition,
                ctx: Int,
            ): AstNode {
                val i = partitionNames.indexOf(node.columnReference)
                return if (i >= 0) WindowPartition(Identifier.of(aliases[i])) else node
            }
        }

    private fun replace(node: Expr): Expr? {
        keys.forEachIndexed { i, key ->
            val ref = { exprVarRef(Identifier.of(aliases[i]), false) }
            if (node == key && ownRoots(node)) return ref()
            if (node is ExprPath && ownRoots(node)) {
                if (node.root == key) return exprPath(ref(), node.steps)
                val keySteps = (key as? ExprPath)?.takeIf { it.root == node.root }?.steps
                if (keySteps != null && node.steps.size > keySteps.size && node.steps.subList(0, keySteps.size) == keySteps) {
                    return exprPath(ref(), node.steps.drop(keySteps.size))
                }
            }
        }
        return null
    }

    /** True if the variables of [expr], outside its subqueries, are FROM items of the query block. */
    private fun ownRoots(expr: Expr): Boolean {
        var own = true
        val visitor =
            object : AstWalker() {
                override fun visitExprVarRef(
                    node: ExprVarRef,
                    ctx: Unit,
                ) {
                    val binding = bindingOf(node)
                    if (binding != null && binding.scope !== scope) own = false
                }

                override fun visitExprQuerySet(
                    node: ExprQuerySet,
                    ctx: Unit,
                ) {}
            }
        expr.accept(visitor, Unit)
        return own
    }

    /** The qualified name of a key that is a column or a SUPER path, as a window PARTITION BY writes it. */
    private fun qualifiedName(expr: Expr): Identifier? {
        val root = (if (expr is ExprPath) expr.root else expr) as? ExprVarRef ?: return null
        if (root.identifier.hasQualifier()) return null
        val parts = mutableListOf(root.identifier.identifier)
        if (expr is ExprPath) {
            for (step in expr.steps) {
                parts.add(
                    when (step) {
                        is PathStep.Field -> step.field
                        is PathStep.Element -> {
                            val element = step.element
                            if (element is ExprLit && element.lit.code() == Literal.STRING) {
                                Identifier.Simple.delimited(
                                    element.lit.stringValue(),
                                )
                            } else {
                                return null
                            }
                        }
                        else -> return null
                    },
                )
            }
        }
        return Identifier.of(parts)
    }

    companion object {
        /** The PartiQL aggregate functions. */
        private val AGGREGATES = setOf("count", "sum", "avg", "min", "max", "every", "any", "some", "group_as")

        fun isAggregate(call: ExprCall): Boolean {
            if (call.function.hasQualifier()) return false
            return call.function.identifier.text.removePrefix("\uFDEF").lowercase() in AGGREGATES
        }
    }
}

/**
 * A visitor of all nodes of an AST. The PLK [AstVisitor] visits the window function types RANK, DENSE_RANK,
 * PERCENT_RANK, CUME_DIST, and ROW_NUMBER by calling them again, which never ends; this visitor visits their children.
 */
@Suppress("DEPRECATION")
internal abstract class AstWalker : AstVisitor<Unit, Unit>() {
    override fun defaultReturn(
        node: AstNode,
        ctx: Unit,
    ) {}

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowFunctionTypeRank(
        node: WindowFunctionType.Rank,
        ctx: Unit,
    ) = defaultVisit(node, ctx)

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowFunctionTypeDenseRank(
        node: WindowFunctionType.DenseRank,
        ctx: Unit,
    ) = defaultVisit(node, ctx)

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowFunctionTypePercentRank(
        node: WindowFunctionType.PercentRank,
        ctx: Unit,
    ) = defaultVisit(node, ctx)

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowFunctionTypeCumeDist(
        node: WindowFunctionType.CumeDist,
        ctx: Unit,
    ) = defaultVisit(node, ctx)

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowFunctionTypeRowNumber(
        node: WindowFunctionType.RowNumber,
        ctx: Unit,
    ) = defaultVisit(node, ctx)
}

/**
 * Flattens nested paths: the builder makes `t.x` for the column `x` at the root of `x.y`, and the expressions add the
 * step `.y`, which gives `(t.x).y`. The flat path `t.x.y` is the same path, and it is equal to the same path written
 * with the alias, which GROUP BY keys and their references need.
 */
internal class Paths {
    fun flatten(expr: Expr): Expr = rewriter.visitExpr(expr, Unit) as Expr

    /** The number of field steps after a variable that [flatten] removes: the table name of a qualified column. */
    private val skips = java.util.IdentityHashMap<ExprVarRef, Int>()

    fun skip(
        ref: ExprVarRef,
        steps: Int,
    ) {
        skips[ref] = steps
    }

    private val rewriter =
        object : IdentityRewriter<Unit>() {
            override fun visitExprPath(
                node: ExprPath,
                ctx: Unit,
            ): AstNode {
                val visited = super.visitExprPath(node, ctx) as ExprPath
                val root = visited.root
                val flat = if (root is ExprPath) exprPath(root.root, root.steps + visited.steps) else visited
                val skip = (flat.root as? ExprVarRef)?.let { skips.remove(it) } ?: return flat
                return exprPath(flat.root, flat.steps.drop(skip))
            }
        }
}

/**
 * A rewriter that keeps the nodes that it does not change, so that rewrites can compare nodes by identity. The PLK
 * [AstRewriter] makes a new copy of every identifier and of every window specification with PARTITION BY.
 */
@Suppress("DEPRECATION")
internal abstract class IdentityRewriter<C> : AstRewriter<C>() {
    override fun visitIdentifierSimple(
        node: Identifier.Simple,
        ctx: C,
    ): AstNode = node

    @Deprecated("This feature is experimental and is subject to change.")
    override fun visitWindowSpecification(
        node: WindowSpecification,
        ctx: C,
    ): AstNode {
        val partitions =
            node.partitionClause?.let { list ->
                val visited = list.map { visitWindowPartition(it, ctx) as WindowPartition }
                if (visited.indices.all { visited[it] === list[it] }) list else visited
            }
        val order = node.orderClause?.let { visitOrderBy(it, ctx) as org.partiql.ast.OrderBy }
        return if (partitions !== node.partitionClause || order !== node.orderClause) {
            WindowSpecification(
                node.existingName,
                partitions,
                order,
            )
        } else {
            node
        }
    }
}
