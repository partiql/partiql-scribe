package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprIsType
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
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
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.setOp
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.with
import org.partiql.ast.Ast.withListElement
import org.partiql.ast.DataType
import org.partiql.ast.From
import org.partiql.ast.FromTableRef
import org.partiql.ast.FromType
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.JoinType
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SelectItem
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.scribe.frontend.redshift.RedshiftProblem
import org.partiql.spi.types.PType
import java.util.IdentityHashMap
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Builds a PartiQL AST from a Redshift parse tree.
 *
 * Each method translates one grammar rule. A construct that has no translation throws
 * [UnsupportedConstructException] with the location of the construct in the input, and a query that Redshift rejects
 * throws [InvalidQueryException].
 *
 * The builder resolves the names of a query as Redshift does and writes the result into the AST: every column
 * reference is qualified with the alias of its FROM item, and references to select-list aliases are replaced by the
 * aliased expressions. See [Scope].
 *
 * @param caseSensitiveIdentifiers The Redshift `enable_case_sensitive_identifier` setting. When false (the Redshift
 * default), Redshift folds quoted and unquoted identifiers to lower case, so both become PartiQL regular
 * (case-insensitive) identifiers. When true, quoted identifiers become PartiQL delimited (case-sensitive) identifiers.
 * @param types The static types of operands, for the rules that depend on them; see [OperandTypes].
 * @param catalog The tables of the session, for the columns of FROM items.
 * @param analysis The decisions about names that hold for all passes over the parse tree.
 */
internal class RedshiftAstBuilder(
    private val caseSensitiveIdentifiers: Boolean,
    private val notes: MutableList<RedshiftProblem>,
    private val types: OperandTypes,
    private val catalog: RedshiftCatalog,
    private val analysis: NameAnalysis,
) {
    internal val functions = RedshiftFunctions(notes, types)
    internal val exprs = RedshiftExpressions(this, functions, types)
    private val windows = RedshiftWindows(this)
    private val generated = PassNames(analysis)
    private val paths = Paths()

    /** The query blocks that contain the expression that the builder translates, the innermost last. */
    private val blocks = ArrayDeque<Block>()

    /** The clause of the innermost query block that the builder translates. */
    private var clause = Clause.OTHER

    /** The FROM item of each variable that the builder made for a column reference or a FROM alias. */
    private val roots = IdentityHashMap<ExprVarRef, Binding>()

    fun root(ctx: P.RootContext): Query {
        val queries = ctx.query()
        if (queries.size > 1) throw UnsupportedConstructException.at(queries[1], "multiple statements")
        return query(query(queries[0], parent = null, commonTables = emptyList()).expr)
    }

    /*
     * Queries
     */

    /** A translated query and its output columns, or null columns if they are not known. */
    internal class Translated(
        val expr: ExprQuerySet,
        val columns: List<Identifier.Simple>?,
    )

    /**
     * Translates a subquery of an expression (scalar, IN, EXISTS), which can refer to the FROM items of the query
     * blocks that contain it.
     */
    internal fun exprQuerySet(ctx: P.QueryContext): ExprQuerySet {
        val outer = blocks.lastOrNull()?.scope
        return inClause(Clause.OTHER) { query(ctx, outer, outer?.commonTables ?: emptyList()).expr }
    }

    /**
     * `[WITH ...] query-term [ORDER BY ...] [LIMIT ...] [OFFSET ...]`.
     *
     * A common table expression becomes a PartiQL WITH element with a name that is unique in the statement, and each
     * reference to it in FROM becomes `@"$cte_<name>" AS <alias>`: an unqualified FROM name is a catalog table in
     * PartiQL, so the reference must be a local variable. The PartiQL planner replaces the references with the query.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WITH_clause.html
     */
    private fun query(
        ctx: P.QueryContext,
        parent: Scope?,
        commonTables: List<CommonTable>,
    ): Translated {
        var tables = commonTables
        val with =
            ctx.withClause()?.let { clause ->
                clause.RECURSIVE()?.let { throw UnsupportedConstructException.at(it.symbol, "WITH RECURSIVE") }
                val elements =
                    clause.commonTableExpression().map { cte ->
                        val name = simple(cte.name)
                        val body = query(cte.query(), parent, tables)
                        val columnList = cte.columnAliases()?.identifier()?.map(::simple)
                        val renamed = columnList?.let { renameColumns(body, it, cte.columnAliases(), "WITH column list") } ?: body
                        val partiqlName = generated.fresh("\$cte_${name.text}")
                        tables = tables + CommonTable(name, partiqlName, renamed.columns?.map { it.text })
                        withListElement(partiqlName, renamed.expr, null)
                    }
                note(clause.WITH().symbol, "Redshift `WITH` queries were replaced by subqueries")
                with(elements, false)
            }
        val term = ctx.queryTerm()
        val primary = (term as? P.QueryTermPrimaryContext)?.queryPrimary()
        val result =
            when {
                primary is P.QueryPrimarySpecificationContext ->
                    block(primary.querySpecification(), ctx.orderByClause(), ctx.limitClause(), parent, tables, setOperand = false)
                else -> setOperation(term, ctx.orderByClause(), ctx.limitClause(), parent, tables)
            }
        if (with == null) return result
        val expr = result.expr
        return Translated(exprQuerySet(expr.body, expr.orderBy, expr.limit, expr.offset, with), result.columns)
    }

    /*
     * Set operations
     */

    /**
     * UNION, INTERSECT, EXCEPT, and MINUS. The output columns have the names of the first query, so the items of the
     * later queries are aliased with these names: PartiQL names a column `_` when the names of the queries differ. When
     * names of the first query repeat, PartiQL cannot tell the columns apart, so every query names its items
     * `$c1`, `$c2`, ..., and an outer query renames them.
     *
     * ORDER BY of a set operation can use the output names and positions.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_UNION.html
     */
    private fun setOperation(
        ctx: P.QueryTermContext,
        orderBy: P.OrderByClauseContext?,
        limit: P.LimitClauseContext?,
        parent: Scope?,
        tables: List<CommonTable>,
    ): Translated {
        val term = setTerm(ctx, parent, tables)
        val names =
            term.columns ?: throw UnsupportedConstructException.at(ctx, "set operation over `*` of a FROM item with unknown columns")
        val unique = names.map { it.text }.toSet().size == names.size
        val (expr, columns) =
            if (unique) {
                renameLaterOperands(term, names) to names
            } else {
                val positional = names.indices.map { generated.fresh("\$c${it + 1}") }
                val renamed = renameColumns(term, positional, ctx, "set operation")
                renameLaterOperands(Translated(renamed.expr, positional), positional) to positional
            }
        val sorts = orderBy?.sortItem()?.map { setOperationSort(it, columns, names) }
        val (limitExpr, offsetExpr) = limitOffset(limit)
        if (unique) {
            return Translated(exprQuerySet(expr.body, sorts?.let(::orderBy), limitExpr, offsetExpr), names)
        }
        // SELECT "$s"."$c1" AS n1, ... FROM (<set operation>) AS "$s"
        val alias = generated.fresh("\$s")
        val items = columns.indices.map { selectItemExpr(field(alias, columns[it]), names[it]) }
        val outerSorts = sorts?.map { sort(field(alias, ((it.expr as ExprVarRef).identifier.identifier)), it.order, it.nulls) }
        val body = queryBodySFW(selectList(items, null), null, from(listOf(fromExpr(expr, FromType.SCAN(), alias, null))))
        note(ctx.start, "The set operation was moved into a subquery, because its column names repeat")
        return Translated(exprQuerySet(body, outerSorts?.let(::orderBy), limitExpr, offsetExpr), names)
    }

    /** The set operation [ctx] with the names of its first query; the later queries are renamed by the caller. */
    private fun setTerm(
        ctx: P.QueryTermContext,
        parent: Scope?,
        tables: List<CommonTable>,
    ): Translated =
        when (ctx) {
            is P.QueryTermPrimaryContext ->
                when (val primary = ctx.queryPrimary()) {
                    is P.QueryPrimarySpecificationContext ->
                        block(
                            primary.querySpecification(),
                            null,
                            null,
                            parent,
                            tables,
                            setOperand = true,
                        )
                    is P.QueryPrimaryParenthesizedContext -> query(primary.query(), parent, tables)
                    else -> unexpected(primary)
                }
            is P.QueryTermSetOpContext -> {
                val lhs = setTerm(ctx.lhs, parent, tables)
                val rhs = setTerm(ctx.rhs, parent, tables)
                val lhsColumns = lhs.columns
                val rhsColumns = rhs.columns
                if (lhsColumns != null && rhsColumns != null && lhsColumns.size != rhsColumns.size) {
                    throw InvalidQueryException.at(ctx.op, "each ${ctx.op.text.uppercase()} query must have the same number of columns")
                }
                val type =
                    when (ctx.op.type) {
                        P.UNION -> SetOpType.UNION()
                        P.INTERSECT -> SetOpType.INTERSECT()
                        else -> SetOpType.EXCEPT()
                    }
                val all = ctx.setQuantifier()?.ALL() != null
                if (all && ctx.op.type != P.UNION) {
                    throw InvalidQueryException.at(ctx.setQuantifier(), "${ctx.op.text.uppercase()} ALL is not supported in Redshift")
                }
                if (ctx.op.type == P.MINUS_KEYWORD) note(ctx.op, "Redshift `MINUS` was replaced by PartiQL `EXCEPT`")
                val left = if (type.code() == SetOpType.EXCEPT) distinctLeftOperand(ctx.lhs, lhs) else lhs.expr
                val setq = if (all) SetQuantifier.ALL() else SetQuantifier.DISTINCT()
                // In a probe pass, an OUTER set operation: the planner rejects a set operation over the dynamic type of
                // the placeholders and drops the typed queries, but it does not check the types of an OUTER one.
                val body = queryBodySetOp(setOp(type, setq), types.probing, left, rhs.expr)
                Translated(exprQuerySet(body, null, null, null), lhs.columns)
            }
            else -> unexpected(ctx)
        }

    /**
     * EXCEPT returns the distinct rows of the left query that the right query does not return. PLK 1.6.1 returns
     * duplicate rows of the left query; a SELECT DISTINCT left query has the same result without the defect.
     */
    private fun distinctLeftOperand(
        ctx: P.QueryTermContext,
        lhs: Translated,
    ): Expr {
        val primary = (ctx as? P.QueryTermPrimaryContext)?.queryPrimary() as? P.QueryPrimarySpecificationContext ?: return lhs.expr
        val expr = lhs.expr
        val body = expr.body as? QueryBody.SFW ?: return lhs.expr
        if (expr.limit != null || expr.offset != null || expr.orderBy != null) return lhs.expr
        val select =
            when (val s = body.select) {
                is org.partiql.ast.SelectList -> if (s.setq != null) return lhs.expr else selectList(s.items, SetQuantifier.DISTINCT())
                is org.partiql.ast.SelectStar -> if (s.setq != null) return lhs.expr else selectStar(SetQuantifier.DISTINCT())
                else -> return lhs.expr
            }
        note(primary.start, "DISTINCT was added to the left query of EXCEPT, which returns distinct rows")
        return exprQuerySet(sfw(body, select = select), null, null, null)
    }

    /** Aliases the items of every query of [term] except the first with [names]. */
    private fun renameLaterOperands(
        term: Translated,
        names: List<Identifier.Simple>,
    ): ExprQuerySet {
        val body = term.expr.body as? QueryBody.SetOp ?: return term.expr
        val lhs = (body.lhs as? ExprQuerySet)?.let { renameLaterOperands(Translated(it, names), names) } ?: body.lhs
        val rhs = (body.rhs as? ExprQuerySet)?.let { renameAll(it, names) } ?: body.rhs
        return exprQuerySet(
            queryBodySetOp(body.type, body.isOuter, lhs, rhs),
            term.expr.orderBy,
            term.expr.limit,
            term.expr.offset,
            term.expr.with,
        )
    }

    private fun renameAll(
        expr: ExprQuerySet,
        names: List<Identifier.Simple>,
    ): ExprQuerySet =
        when (val body = expr.body) {
            is QueryBody.SetOp -> {
                val lhs = (body.lhs as? ExprQuerySet)?.let { renameAll(it, names) } ?: body.lhs
                val rhs = (body.rhs as? ExprQuerySet)?.let { renameAll(it, names) } ?: body.rhs
                exprQuerySet(queryBodySetOp(body.type, body.isOuter, lhs, rhs), expr.orderBy, expr.limit, expr.offset, expr.with)
            }
            is QueryBody.SFW -> {
                val select = body.select as? org.partiql.ast.SelectList ?: return expr
                val items =
                    select.items.mapIndexed { i, item ->
                        if (item is SelectItem.Expr && i < names.size) selectItemExpr(item.expr, names[i]) else item
                    }
                exprQuerySet(sfw(body, select = selectList(items, select.setq)), expr.orderBy, expr.limit, expr.offset, expr.with)
            }
            else -> expr
        }

    /**
     * Gives the first `names.size` output columns of [query] the [names]: the column list of a common table expression
     * or a derived table, `WITH x(a, b) AS (...)`, `(...) AS s(a, b)`.
     */
    private fun renameColumns(
        query: Translated,
        names: List<Identifier.Simple>,
        ctx: ParserRuleContext,
        construct: String,
    ): Translated {
        val columns =
            query.columns
                ?: throw UnsupportedConstructException.at(ctx, "$construct for a query with `*` of a FROM item with unknown columns")
        if (names.size > columns.size) {
            throw InvalidQueryException.at(ctx, "$construct has ${names.size} names for ${columns.size} columns")
        }
        val renamed = names + columns.drop(names.size)
        return Translated(renameAll(query.expr, renamed), renamed)
    }

    /**
     * A sort item of a set operation: an output column name or position.
     */
    private fun setOperationSort(
        ctx: P.SortItemContext,
        columns: List<Identifier.Simple>,
        names: List<Identifier.Simple>,
    ): Sort {
        val index =
            position(ctx.expr())?.let { k ->
                if (k < 1 || k > columns.size) throw InvalidQueryException.at(ctx, "ORDER BY position $k is not in the select list")
                note(ctx.start, "Redshift `ORDER BY <position>` was replaced by the output column name")
                k - 1
            } ?: bareName(ctx.expr())?.let { name ->
                val matching = names.indices.filter { matches(name, names[it].text) }
                when (matching.size) {
                    1 -> matching.single()
                    0 -> throw InvalidQueryException.at(ctx, "ORDER BY `${name.text}` is not an output column of the set operation")
                    else -> throw InvalidQueryException.at(ctx, "ORDER BY `${name.text}` is ambiguous")
                }
            } ?: throw InvalidQueryException.at(ctx, "ORDER BY of a set operation can use only output column names or positions")
        return sortOf(ctx, exprVarRef(Identifier.of(columns[index]), false))
    }

    /*
     * Query blocks
     */

    /** A select item, after `*` is expanded. */
    private class Item(
        val ctx: P.SelectItemContext,
        /** The output name: the alias, or the name that Redshift derives. */
        val name: Identifier.Simple,
        /** True if the name is an alias in the input, which other clauses can refer to. */
        val isAlias: Boolean,
        /** The translation, or null for a `*` over a FROM item with unknown columns. */
        val expr: Expr?,
        /** The FROM item of a `*` that is not expanded. */
        val star: Binding?,
        val hasAggregate: Boolean,
        val hasWindow: Boolean,
        /** True if the item is a column of an expanded `*`. */
        val fromStar: Boolean,
    )

    private inner class Block(
        val spec: P.QuerySpecificationContext,
        val scope: Scope,
    ) {
        /** The select items, expanded; while the select list is translated, the items before the current one. */
        val items = mutableListOf<Item>()

        /** The counter of the PartiQL planner for the names of FROM items without an alias. */
        var unaliased = 0
    }

    private enum class Clause { SELECT, TABLE_NAME, FROM_SOURCE, JOIN_CONDITION, WHERE, GROUP_BY, HAVING, QUALIFY, ORDER_BY, WINDOW, OTHER }

    private fun <T> inClause(
        c: Clause,
        block: () -> T,
    ): T {
        val saved = clause
        clause = c
        try {
            return block()
        } finally {
            clause = saved
        }
    }

    /** True while the builder translates a FROM source expression, for example `t.items` in `FROM t, t.items AS i`. */
    internal val inFromSource: Boolean get() = clause == Clause.FROM_SOURCE

    /**
     * A query block: `SELECT ... [FROM ...] [WHERE ...] [GROUP BY ...] [HAVING ...] [QUALIFY ...]` with the ORDER BY and
     * LIMIT of the query, if the query is not a set operation.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html
     */
    private fun block(
        spec: P.QuerySpecificationContext,
        orderBy: P.OrderByClauseContext?,
        limit: P.LimitClauseContext?,
        parent: Scope?,
        tables: List<CommonTable>,
        setOperand: Boolean,
    ): Translated =
        types.inScope(spec) {
            val block = Block(spec, Scope(parent, tables))
            blocks.addLast(block)
            try {
                translateBlock(block, orderBy, limit, setOperand)
            } finally {
                blocks.removeLast()
            }
        }

    private fun translateBlock(
        block: Block,
        orderBy: P.OrderByClauseContext?,
        limitCtx: P.LimitClauseContext?,
        setOperand: Boolean,
    ): Translated {
        val spec = block.spec
        spec.intoClause()?.let { throw UnsupportedConstructException.at(it, "SELECT INTO") }
        spec.connectByClause()?.let { throw UnsupportedConstructException.at(it, "CONNECT BY") }
        val from = inClause(Clause.FROM_SOURCE) { spec.fromClause()?.let { fromClause(it, block) } } ?: noFrom(block)
        selectItems(block)
        spec.excludeClause()?.let { exclude(it, block) }
        val where = spec.whereClause()?.let { inClause(Clause.WHERE) { expr(it.expr()) } }
        val keys = spec.groupByClause()?.let { groupByKeys(it, block) }
        val having = spec.havingClause()?.let { inClause(Clause.HAVING) { expr(it.expr()) } }
        val qualify = spec.qualifyClause()?.let { qualifyClause(it, spec) }
        val sorts = orderBy?.sortItem()?.map { sortItem(it, block) }
        if (setOperand) spec.top?.let { throw UnsupportedConstructException.at(it, "TOP in a set operation operand") }
        var (limit, offset) = limitOffset(limitCtx)
        spec.top?.let { top ->
            if (limitCtx?.limit != null) throw InvalidQueryException.at(top, "TOP and LIMIT can't be used in the same query")
            note(top.start, "Redshift `TOP n` was replaced by PartiQL `LIMIT n`")
            limit = exprLit(Literal.intNum(top.INTEGER_LITERAL().text))
        }
        val distinct = spec.setQuantifier()?.DISTINCT() != null
        return RedshiftQueryBlocks(this, block.scope, generated).assemble(
            RedshiftQueryBlocks.Parts(
                items = block.items.map { RedshiftQueryBlocks.Column(it.name, it.expr, it.star, it.ctx) },
                from = from,
                where = where,
                keys = keys,
                having = having,
                qualify = qualify,
                qualifyCtx = spec.qualifyClause(),
                sorts = sorts,
                limit = limit,
                offset = offset,
                distinct = distinct,
                distinctCtx = spec.setQuantifier(),
                spec = spec,
            ),
            ::selectItemTypes,
        )
    }

    /**
     * `QUALIFY condition` filters the rows after the window functions; the translation computes the condition in a
     * subquery and filters on it (see [RedshiftQueryBlocks]). Directly after the FROM clause, Redshift reads QUALIFY as
     * the alias of an unaliased FROM item, so the FROM item needs an alias.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_QUALIFY_clause.html
     */
    private fun qualifyClause(
        ctx: P.QualifyClauseContext,
        spec: P.QuerySpecificationContext,
    ): Expr {
        val last = spec.fromClause()?.tableReference()?.lastOrNull() as? P.TableBaseContext
        val follows = spec.whereClause() == null && spec.groupByClause() == null && spec.havingClause() == null
        val unaliased =
            when (val primary = last?.tablePrimary()) {
                is P.TableNameContext -> primary.tableAlias() == null
                is P.TableSubqueryContext -> primary.tableAlias() == null
                else -> false
            }
        if (follows && unaliased) {
            throw InvalidQueryException.at(
                ctx,
                "the FROM item before QUALIFY needs an alias; Redshift reads QUALIFY as its alias",
            )
        }
        return inClause(Clause.QUALIFY) { expr(ctx.expr()) }
    }

    /**
     * Wraps every select item whose static type is dynamic (a SUPER value) in a one-argument COALESCE, so that a failed
     * navigation gives NULL, as in Redshift, instead of a missing attribute.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/query-super.html
     */
    private fun selectItemTypes(
        ctx: P.SelectItemContext,
        expr: Expr,
    ): Expr {
        val operand = (ctx as? P.SelectItemExprContext)?.expr() ?: return expr
        if (expr is ExprCoalesce || isColumn(expr)) return expr
        // An aggregate or window function does not return MISSING. The planner also does not find an aggregate in the
        // argument of a function call, such as the probe of the operand types.
        if (containsAggregate(operand) || containsWindow(operand) || RedshiftQueryBlocks.containsAggregateOrWindow(expr)) return expr
        return types.resolve(listOf(operand to expr)) { probed, operandTypes ->
            if (operandTypes[0].code() == PType.DYNAMIC && probed[0] !is ExprCoalesce) {
                if (!types.probing) {
                    note(
                        operand.start,
                        "The SUPER value was wrapped in COALESCE, so that PartiQL returns NULL instead of MISSING",
                    )
                }
                exprCoalesce(listOf(probed[0]))
            } else {
                probed[0]
            }
        }
    }

    /**
     * True if [expr] is a column of a FROM item with known columns, or an unnested array element, which is never
     * MISSING.
     */
    private fun isColumn(expr: Expr): Boolean {
        if (expr is ExprVarRef) return roots[expr]?.kind == Binding.Kind.ELEMENT
        if (expr !is org.partiql.ast.expr.ExprPath || expr.steps.size != 1) return false
        if (expr.steps[0] !is org.partiql.ast.expr.PathStep.Field) return false
        val binding = (expr.root as? ExprVarRef)?.let { roots[it] } ?: return false
        return binding.kind == Binding.Kind.RELATION && binding.columns != null
    }

    /** `SELECT 1`: Redshift selects one row. */
    private fun noFrom(block: Block): From {
        note(block.spec.start, "Redshift SELECT without FROM was given a one-row FROM source")
        val alias = generated.fresh("\$no_from")
        val binding = Binding(alias, alias, Binding.Kind.RELATION, emptyList(), block.scope, block.spec)
        block.scope.bindings.add(binding)
        return from(listOf(fromExpr(exprBag(listOf(exprLit(Literal.intNum(1)))), FromType.SCAN(), alias, null)))
    }

    /*
     * Select list
     */

    private fun selectItems(block: Block) {
        val list = block.spec.selectList().selectItem()
        var unnamed = 0
        for (itemCtx in list) {
            when (itemCtx) {
                is P.SelectItemAllContext -> {
                    val bindings = block.scope.bindings.filter { it.origin !== block.spec }
                    if (bindings.isEmpty()) throw InvalidQueryException.at(itemCtx, "SELECT * with no tables specified is not valid")
                    // A USING join has one column for each pair of join columns, first in the output.
                    if (block.scope.usingColumns.isNotEmpty()) throw UnsupportedConstructException.at(itemCtx, "SELECT * with JOIN USING")
                    bindings.forEach { star(itemCtx, it, block) }
                }
                is P.SelectItemStarContext -> {
                    // `t.*` for a FROM item `t`; `t.x.*` is over a SUPER value.
                    val name = nameParts(itemCtx.primaryExpr())?.singleOrNull()
                    val binding = name?.let { block.scope.resolveBinding(it) }
                    if (binding == null || binding.kind != Binding.Kind.RELATION) {
                        throw UnsupportedConstructException.at(itemCtx, ".* over a SUPER value")
                    }
                    star(itemCtx, binding, block)
                }
                is P.SelectItemExprContext -> {
                    val translated = inClause(Clause.SELECT) { expr(itemCtx.expr()) }
                    val alias = itemCtx.alias?.let(::simple)
                    val derived = alias ?: derivedName(itemCtx.expr())
                    val name =
                        derived ?: run {
                            unnamed++
                            val n = Identifier.Simple.delimited("_$unnamed")
                            note(
                                itemCtx.start,
                                "The output column of an unnamed expression is named `${n.text}` in PartiQL; Redshift uses a default name",
                            )
                            n
                        }
                    block.items.add(
                        Item(
                            itemCtx,
                            name,
                            isAlias = alias != null,
                            expr = translated,
                            star = null,
                            hasAggregate = containsAggregate(itemCtx.expr()),
                            hasWindow = containsWindow(itemCtx.expr()),
                            fromStar = false,
                        ),
                    )
                }
                else -> unexpected(itemCtx)
            }
        }
    }

    /** Expands `*` or `t.*` for the FROM item [binding]. */
    private fun star(
        ctx: P.SelectItemContext,
        binding: Binding,
        block: Block,
    ) {
        if (binding.kind == Binding.Kind.ELEMENT) {
            throw UnsupportedConstructException.at(
                ctx,
                "SELECT * with an unnested SUPER value `${binding.name.text}`",
            )
        }
        val columns = binding.columns
        if (columns == null) {
            block.items.add(Item(ctx, binding.alias, false, null, binding, hasAggregate = false, hasWindow = false, fromStar = true))
            return
        }
        columns.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.let {
            throw UnsupportedConstructException.at(ctx, "* over a FROM item with duplicate column name `${it.key}`")
        }
        for (column in columns) {
            // The step that a reference to the column has, so that the item is equal to the reference.
            val step =
                if (caseSensitiveIdentifiers && column != foldAscii(column)) {
                    Identifier.Simple.delimited(
                        column,
                    )
                } else {
                    Identifier.Simple.regular(foldAscii(column))
                }
            val name = Identifier.Simple.delimited(column)
            block.items.add(
                Item(ctx, name, false, columnRef(binding, step), null, hasAggregate = false, hasWindow = false, fromStar = true),
            )
        }
    }

    /**
     * `SELECT * EXCLUDE a, b`: the expansion of `*` without the columns. EXCLUDE names columns, not the select items
     * of an explicit select list.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_EXCLUDE_list.html
     */
    private fun exclude(
        ctx: P.ExcludeClauseContext,
        block: Block,
    ) {
        if (block.items.any { !it.fromStar }) throw UnsupportedConstructException.at(ctx, "EXCLUDE with a select list other than *")
        if (block.items.any { it.star != null }) {
            throw UnsupportedConstructException.at(
                ctx,
                "EXCLUDE over a FROM item with unknown columns",
            )
        }
        for (idCtx in ctx.identifier()) {
            val id = simple(idCtx)
            val excluded = block.items.filter { matches(id, it.name.text) }
            if (excluded.isEmpty()) {
                throw InvalidQueryException.at(
                    idCtx,
                    "column `${id.text}` of EXCLUDE is not a column of the FROM items",
                )
            }
            block.items.removeAll(excluded)
        }
        if (block.items.isEmpty()) throw InvalidQueryException.at(ctx, "EXCLUDE removes all columns")
        note(ctx.start, "Redshift `EXCLUDE` was replaced by the list of the remaining columns")
    }

    /**
     * The output name that Redshift gives an item without an alias: the column for a column reference, the last
     * attribute for a SUPER path, the function name (lower case) for a function call, the operand for a CAST. Other
     * expressions get the PartiQL name.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_list.html
     */
    private fun derivedName(ctx: P.ExprContext): Identifier.Simple? {
        val value = valueOf(ctx) ?: return null
        return derivedName(value)
    }

    private fun derivedName(ctx: P.ValueExprContext): Identifier.Simple? =
        when (ctx) {
            is P.ValueCastShorthandContext -> derivedName(ctx.valueExpr())
            is P.ValuePrimaryContext -> derivedName(ctx.primaryExpr())
            else -> null
        }

    private fun derivedName(ctx: P.PrimaryExprContext): Identifier.Simple? =
        when (ctx) {
            is P.PathFieldContext -> Identifier.Simple.delimited(simple(ctx.identifier()).text)
            is P.PathElementContext -> derivedName(ctx.primaryExpr())
            is P.PrimaryAtomContext ->
                when (val atom = ctx.atom()) {
                    is P.AtomVariableContext -> Identifier.Simple.delimited(simple(atom.identifier()).text)
                    is P.AtomCastContext -> derivedName(atom.expr())
                    is P.AtomParenthesizedContext -> derivedName(atom.expr())
                    is P.AtomFunctionCallContext -> Identifier.Simple.delimited(foldAscii(atom.name.text.removeSurrounding("\"")))
                    is P.AtomCountStarContext -> Identifier.Simple.delimited("count")
                    is P.AtomApproximateContext -> (atom.atom() as? P.AtomCountStarContext)?.let { Identifier.Simple.delimited("count") }
                    else -> null
                }
            else -> null
        }

    /*
     * FROM clause
     */

    /** A translated FROM item and its bindings. */
    private class TableRef(
        val ref: FromTableRef,
        val bindings: List<Binding>,
        /** True for a SUPER expression that unnests an array, which needs no join condition. */
        val lateral: Boolean = false,
    )

    private fun fromClause(
        ctx: P.FromClauseContext,
        block: Block,
    ): From = from(ctx.tableReference().map { tableReference(it, block).ref })

    private fun tableReference(
        ctx: P.TableReferenceContext,
        block: Block,
    ): TableRef =
        when (ctx) {
            is P.TableBaseContext -> tablePrimary(ctx.tablePrimary(), block)
            is P.TableJoinContext -> join(ctx, block)
            is P.TableCrossJoinContext -> {
                val lhs = tableReference(ctx.lhs, block)
                val rhs = tablePrimary(ctx.rhs, block)
                TableRef(fromJoin(lhs.ref, rhs.ref, JoinType.CROSS(), null), lhs.bindings + rhs.bindings)
            }
            is P.TableNaturalJoinContext -> throw UnsupportedConstructException.at(ctx.NATURAL().symbol, "NATURAL JOIN")
            is P.TablePivotContext -> throw UnsupportedConstructException.at(ctx.PIVOT().symbol, "PIVOT")
            is P.TableUnpivotContext -> throw UnsupportedConstructException.at(ctx.UNPIVOT().symbol, "UNPIVOT")
            else -> unexpected(ctx)
        }

    /**
     * `lhs [INNER | LEFT | RIGHT | FULL] JOIN rhs ON condition` and `... USING (columns)`. A join with a SUPER
     * expression on the right needs no condition.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause30.html
     */
    private fun join(
        ctx: P.TableJoinContext,
        block: Block,
    ): TableRef {
        val lhs = tableReference(ctx.lhs, block)
        val rhs = tablePrimary(ctx.rhs, block)
        val type = joinType(ctx.joinType())
        val bindings = lhs.bindings + rhs.bindings
        val condition =
            when (val spec = ctx.joinSpecification()) {
                is P.JoinOnContext -> {
                    val scope = block.scope
                    val saved = scope.visible
                    scope.visible = bindings
                    try {
                        inClause(Clause.JOIN_CONDITION) { expr(spec.condition) }
                    } finally {
                        scope.visible = saved
                    }
                }
                is P.JoinUsingContext -> using(spec, lhs, rhs, type, block)
                null -> {
                    if (!rhs.lateral) throw InvalidQueryException.at(ctx.JOIN().symbol, "JOIN without ON or USING")
                    exprLit(Literal.bool(true))
                }
                else -> unexpected(spec)
            }
        return TableRef(fromJoin(lhs.ref, rhs.ref, type, condition), bindings)
    }

    private fun joinType(ctx: P.JoinTypeContext?): JoinType =
        when {
            ctx == null || ctx.INNER() != null -> JoinType.INNER()
            ctx.LEFT() != null -> JoinType.LEFT_OUTER()
            ctx.RIGHT() != null -> JoinType.RIGHT_OUTER()
            ctx.FULL() != null -> JoinType.FULL_OUTER()
            else -> unexpected(ctx)
        }

    /**
     * `JOIN ... USING (c, ...)` is `JOIN ... ON l.c = r.c AND ...`, and an unqualified reference to `c` is the column of
     * the left FROM item for INNER and LEFT joins, of the right FROM item for RIGHT joins, and
     * `COALESCE(l.c, r.c)` for FULL joins. Each side must have exactly one FROM item with the column.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_Join_examples.html
     */
    private fun using(
        ctx: P.JoinUsingContext,
        lhs: TableRef,
        rhs: TableRef,
        type: JoinType,
        block: Block,
    ): Expr {
        note(
            ctx.USING().symbol,
            "Redshift `JOIN ... USING` was replaced by `JOIN ... ON`, and references to the join columns were qualified",
        )
        val conditions =
            ctx.identifier().map { idCtx ->
                val name = simple(idCtx)
                val left = usingSide(lhs, name, idCtx, block)
                val right = usingSide(rhs, name, idCtx, block)
                val merged: () -> Expr =
                    when (type.code()) {
                        JoinType.RIGHT_OUTER -> right
                        JoinType.FULL_OUTER -> { -> exprCoalesce(listOf(left(), right())) }
                        else -> left
                    }
                block.scope.usingColumns.add(UsingColumn(name, merged, lhs.bindings + rhs.bindings))
                exprOperator("=", left(), right()) as Expr
            }
        return conditions.reduce { acc, e -> org.partiql.ast.Ast.exprAnd(acc, e) }
    }

    private fun usingSide(
        side: TableRef,
        name: Identifier.Simple,
        ctx: ParserRuleContext,
        block: Block,
    ): () -> Expr {
        val using = block.scope.usingColumns.lastOrNull { matches(name, it.name.text) && side.bindings.containsAll(it.bindings) }
        if (using != null) return using.expr
        if (side.bindings.any {
                it.columns == null
            }
        ) {
            throw UnsupportedConstructException.at(ctx, "JOIN USING with a FROM item with unknown columns")
        }
        val owners = side.bindings.filter { it.kind == Binding.Kind.RELATION && it.hasColumn(name) }
        val owner =
            when (owners.size) {
                1 -> owners.single()
                0 -> throw InvalidQueryException.at(ctx, "column `${name.text}` of USING is not a column of the joined tables")
                else -> throw InvalidQueryException.at(ctx, "column `${name.text}` of USING is ambiguous")
            }
        return { columnRef(owner, name) }
    }

    /**
     * A table, view, common table expression, subquery, SUPER expression, or UNNEST.
     */
    private fun tablePrimary(
        ctx: P.TablePrimaryContext,
        block: Block,
    ): TableRef =
        when (ctx) {
            is P.TableNameContext -> tableName(ctx, block)
            is P.TableTemporaryContext -> throw UnsupportedConstructException.at(ctx, "temporary table `${ctx.TEMP_TABLE_NAME().text}`")
            is P.TableSubqueryContext -> derivedTable(ctx, block)
            is P.TableParenthesizedContext -> {
                ctx.index?.let { throw UnsupportedConstructException.at(ctx.AT().symbol, "AT index in a FROM item") }
                val inner = ctx.tableReference()
                val alias = ctx.tableAlias()
                if (alias != null) {
                    val primary = (inner as? P.TableBaseContext)?.tablePrimary() as? P.TableNameContext
                    if (primary == null || primary.tableAlias() != null) {
                        throw UnsupportedConstructException.at(
                            alias,
                            "alias of a parenthesized join",
                        )
                    }
                    tableName(primary, block, alias)
                } else {
                    tableReference(inner, block)
                }
            }
            is P.TableUnnestContext -> unnest(ctx, block)
            is P.TableObjectUnpivotContext -> throw UnsupportedConstructException.at(ctx, "UNPIVOT of a SUPER object")
            else -> unexpected(ctx)
        }

    /**
     * A FROM item with a name: a common table expression, a catalog table, or a SUPER expression rooted at an earlier
     * FROM item, which unnests an array (`FROM t, t.items AS i`).
     *
     * Redshift unnests only arrays: a FROM item over a value that is not an array gives no rows. PartiQL iterates a
     * non-array value once, so the source is `CASE WHEN x IS LIST THEN x ELSE [] END`. The root of the source is `@t`,
     * because PartiQL resolves a FROM source name as a catalog table first.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/query-super.html
     */
    private fun tableName(
        ctx: P.TableNameContext,
        block: Block,
        aliasCtx: P.TableAliasContext? = ctx.tableAlias(),
    ): TableRef {
        ctx.ASTERISK()?.let { throw UnsupportedConstructException.at(it.symbol, "`table_name *`") }
        ctx.index?.let { throw UnsupportedConstructException.at(ctx.AT().symbol, "AT index in a FROM item") }
        aliasCtx?.columnAliases()?.let {
            throw UnsupportedConstructException.at(
                it,
                "column aliases for table `${ctx.primaryExpr().text}`",
            )
        }
        val scope = block.scope
        val parts = nameParts(ctx.primaryExpr())
        val explicit = aliasCtx?.alias?.let(::simple)
        val commonTable = parts?.singleOrNull()?.let { scope.commonTable(it) }
        if (commonTable != null) {
            val name = explicit ?: parts.single()
            val source = exprVarRef(Identifier.of(commonTable.partiqlName), isQualified = true)
            return named(ctx, block, source, explicit, name, Binding.Kind.RELATION, commonTable.columns)
        }
        val table = parts?.let { catalog.table(it) }
        val root = bareRoot(ctx.primaryExpr())
        val lateral = table == null && root != null && scope.resolveBinding(root) != null
        if (!lateral) {
            // A catalog table. If the catalog does not know the name, the planner reports it.
            val source = inClause(Clause.TABLE_NAME) { primary(ctx.primaryExpr()) }
            val name = explicit ?: parts?.last() ?: throw UnsupportedConstructException.at(ctx, "FROM item `${ctx.primaryExpr().text}`")
            return named(ctx, block, source, explicit, name, Binding.Kind.RELATION, table?.columns, if (explicit == null) parts else null)
        }
        val source = inClause(Clause.FROM_SOURCE) { primary(ctx.primaryExpr()) }
        note(ctx.start, "Redshift unnests only arrays; the PartiQL FROM item iterates CASE WHEN x IS LIST THEN x ELSE [] END")
        val name =
            explicit ?: parts?.last()
                ?: throw UnsupportedConstructException.at(ctx, "FROM item `${ctx.primaryExpr().text}` without an alias")
        val ref = named(ctx, block, arrayOnly(source), explicit, name, Binding.Kind.ELEMENT, null)
        return TableRef(ref.ref, ref.bindings, lateral = true)
    }

    /** `CASE WHEN x IS LIST THEN x ELSE [] END` */
    private fun arrayOnly(source: Expr): Expr =
        exprCase(null, listOf(exprCaseBranch(exprIsType(source, DataType.LIST(), false), source)), exprArray(emptyList()))

    /** Registers the FROM item [ctx] with the Redshift [name] and makes its PartiQL FROM expression. */
    private fun named(
        ctx: ParserRuleContext,
        block: Block,
        source: Expr,
        explicit: Identifier.Simple?,
        name: Identifier.Simple,
        kind: Binding.Kind,
        columns: List<String>?,
        table: List<Identifier.Simple>? = null,
    ): TableRef {
        // The PartiQL planner names a FROM item without an alias after its expression, or `_<n>`; this counter is the
        // one of the planner, so that the alias is the same.
        if (explicit == null) block.unaliased++
        val partiqlName = if (explicit != null) explicit else Identifier.Simple.delimited(name.text)
        val alias = analysis.alias(ctx, partiqlName)
        if (alias !== partiqlName) {
            note(
                ctx.start,
                "The FROM alias `${name.text}` was renamed to `${alias.text}`, because a subquery refers to it",
            )
        }
        val binding = Binding(name, alias, kind, columns, block.scope, ctx, table)
        register(binding, ctx)
        return TableRef(fromExpr(source, FromType.SCAN(), alias, null), listOf(binding))
    }

    private fun register(
        binding: Binding,
        ctx: ParserRuleContext,
    ) {
        val scope = binding.scope
        if (scope.bindings.any {
                matches(binding.name, it.name.text) && it.name.text.equals(binding.name.text, ignoreCase = !caseSensitiveIdentifiers)
            }
        ) {
            throw InvalidQueryException.at(ctx, "table name `${binding.name.text}` specified more than once")
        }
        scope.bindings.add(binding)
        analysis.register(binding)
    }

    /**
     * `(subquery) [AS] alias [(column, ...)]`. A subquery in FROM does not see the other FROM items of the same FROM
     * clause, but the FROM items of the enclosing query blocks.
     */
    private fun derivedTable(
        ctx: P.TableSubqueryContext,
        block: Block,
    ): TableRef {
        val scope = block.scope
        val translated = inClause(Clause.OTHER) { query(ctx.query(), scope.parent, scope.commonTables) }
        val aliasCtx = ctx.tableAlias()
        val columns =
            aliasCtx?.columnAliases()?.let { aliases ->
                note(aliases.start, "The column aliases of `${aliasCtx.alias.text}(...)` were applied to the subquery select list")
                renameColumns(translated, aliases.identifier().map(::simple), aliases, "column aliases")
            } ?: translated
        val explicit = aliasCtx?.alias?.let(::simple)
        if (explicit == null) block.unaliased++
        val partiqlName = explicit ?: Identifier.Simple.delimited("_${block.unaliased - 1}")
        val alias = analysis.alias(ctx, partiqlName)
        val binding = Binding(explicit ?: partiqlName, alias, Binding.Kind.RELATION, columns.columns?.map { it.text }, scope, ctx)
        register(binding, ctx)
        return TableRef(fromExpr(columns.expr, FromType.SCAN(), alias, null), listOf(binding))
    }

    /**
     * `UNNEST(x) [AS] alias(column)` is a subquery over the elements of the array `x`:
     * `(SELECT "$e1" AS column FROM CASE WHEN x IS LIST THEN x ELSE [] END AS "$e1") AS alias`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause-unnest-examples.html
     */
    private fun unnest(
        ctx: P.TableUnnestContext,
        block: Block,
    ): TableRef {
        ctx.OFFSET()?.let { throw UnsupportedConstructException.at(ctx.WITH().symbol, "UNNEST ... WITH OFFSET") }
        val aliasCtx = ctx.tableAlias() ?: throw InvalidQueryException.at(ctx, "UNNEST needs an alias and a column name")
        val column =
            aliasCtx.columnAliases()?.identifier()?.singleOrNull()
                ?: throw InvalidQueryException.at(aliasCtx, "UNNEST needs one column name")
        val source = inClause(Clause.FROM_SOURCE) { expr(ctx.expr()) }
        note(ctx.start, "Redshift UNNEST(x) AS a(c) was replaced by a subquery over CASE WHEN x IS LIST THEN x ELSE [] END")
        val element = generated.fresh("\$e")
        val columnName = simple(column)
        val body =
            queryBodySFW(
                selectList(listOf(selectItemExpr(exprVarRef(Identifier.of(element), false), columnName)), null),
                null,
                from(listOf(fromExpr(arrayOnly(source), FromType.SCAN(), element, null))),
            )
        val name = simple(aliasCtx.alias)
        val alias = analysis.alias(ctx, name)
        val binding = Binding(name, alias, Binding.Kind.RELATION, listOf(columnName.text), block.scope, ctx)
        register(binding, ctx)
        return TableRef(fromExpr(exprQuerySet(body, null, null, null), FromType.SCAN(), alias, null), listOf(binding), lateral = true)
    }

    /*
     * Names
     */

    /**
     * Translates a name in an expression, which the expressions translate as a variable: a column, a FROM alias as
     * the root of a path (`t.c`), a SUPER column as the root of a path (`x.attr`), or a select-list alias.
     */
    internal fun variable(ctx: P.AtomVariableContext): Expr {
        val idCtx = ctx.identifier()
        if (idCtx is P.IdentifierNiladicContext) throw UnsupportedConstructException.at(idCtx, "`${idCtx.name.text.uppercase()}`")
        val id = simple(idCtx)
        val block = blocks.lastOrNull() ?: return exprVarRef(Identifier.of(id), false)
        val scope = block.scope
        // A table name; the planner resolves it in the catalog.
        if (clause == Clause.TABLE_NAME) return exprVarRef(Identifier.of(id), false)
        if (clause == Clause.FROM_SOURCE) {
            // A FROM source: the root of a SUPER expression is an earlier FROM item, written `@t` because PartiQL
            // resolves a FROM source name as a catalog table first.
            // A name in FROM is a FROM item (an unnested element in `FROM t, t.x AS a, a AS f`) before a column.
            scope.resolveBinding(id)?.let { return bindingRef(it, scope, qualified = true) }
            return column(ctx, id, block, qualified = true)
        }
        if (isPathRoot(ctx)) {
            scope.resolveBinding(id)?.let { binding ->
                checkColumn(ctx, binding)
                return bindingRef(binding, scope, qualified = false)
            }
            schemaQualified(ctx, id, scope)?.let { return it }
            return column(ctx, id, block)
        }
        return when (clause) {
            Clause.SELECT -> lateralAlias(ctx, id, block) ?: column(ctx, id, block)
            Clause.WHERE, Clause.GROUP_BY, Clause.HAVING, Clause.QUALIFY, Clause.ORDER_BY ->
                clauseAlias(ctx, id, block) ?: column(ctx, id, block)
            else -> column(ctx, id, block)
        }
    }

    /** The variable for the FROM item [binding], from the query block of [scope]. */
    private fun bindingRef(
        binding: Binding,
        scope: Scope,
        qualified: Boolean,
    ): ExprVarRef {
        if (binding.scope !== scope) analysis.referencedFromInnerBlock(binding)
        val ref = exprVarRef(Identifier.of(binding.alias), isQualified = qualified)
        roots[ref] = binding
        return ref
    }

    /** `alias.column`, or `@alias.column` in a FROM source. */
    private fun columnRef(
        binding: Binding,
        column: Identifier.Simple,
        qualified: Boolean = false,
    ): Expr {
        val ref = exprVarRef(Identifier.of(binding.alias), qualified)
        roots[ref] = binding
        return exprPath(ref, listOf(exprPathStepField(column)))
    }

    /** `alias['column']` for a generated column name. */
    private fun field(
        alias: Identifier.Simple,
        column: Identifier.Simple,
    ): Expr = exprPath(exprVarRef(Identifier.of(alias), false), listOf(exprPathStepField(column)))

    /** The FROM item of the variable [ref], if the builder made it. */
    internal fun bindingOf(ref: ExprVarRef): Binding? = roots[ref]

    /**
     * An unqualified column name, resolved as Redshift does; see [Scope.resolveColumn].
     */
    private fun column(
        ctx: P.AtomVariableContext,
        id: Identifier.Simple,
        block: Block,
        qualified: Boolean = false,
    ): Expr {
        val scope = block.scope
        return when (val resolution = scope.resolveColumn(id)) {
            is ColumnResolution.Column -> {
                val binding = resolution.binding
                if (binding.scope !== scope) analysis.referencedFromInnerBlock(binding)
                if (scope.allVisibleBindings().any { matches(id, it.name.text) } || catalog.table(listOf(id)) != null) {
                    note(
                        ctx.start,
                        "The column reference `${id.text}` was qualified with `${binding.alias.text}`, " +
                            "because PartiQL reads `${id.text}` as a FROM item or a table",
                    )
                }
                columnRef(binding, id, qualified)
            }
            is ColumnResolution.Element -> bindingRef(resolution.binding, scope, qualified)
            is ColumnResolution.Using -> resolution.column.expr()
            is ColumnResolution.Ambiguous -> throw InvalidQueryException.at(ctx, "column reference `${id.text}` is ambiguous")
            is ColumnResolution.Unknown -> throw UnsupportedConstructException.at(
                ctx,
                "column `${id.text}`, which can be a column of more than one FROM item with unknown columns",
            )
            is ColumnResolution.NotFound -> {
                if (scope.resolveBinding(id) != null) {
                    throw UnsupportedConstructException.at(
                        ctx,
                        "reference to the FROM item `${id.text}` as a value",
                    )
                }
                throw InvalidQueryException.at(ctx, "column `${id.text}` does not exist")
            }
        }
    }

    /**
     * A lateral column alias: a name in the select list that is the alias of an earlier item is that item's expression,
     * unless a FROM item has a column of that name.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_list.html
     */
    private fun lateralAlias(
        ctx: P.AtomVariableContext,
        id: Identifier.Simple,
        block: Block,
    ): Expr? {
        val candidates = block.items.filter { it.isAlias && matches(id, it.name.text) }
        if (candidates.isEmpty() || isColumn(id, block)) return null
        val item =
            candidates.singleOrNull()
                ?: throw UnsupportedConstructException.at(ctx, "reference to the repeated select-list alias `${id.text}`")
        // A window function computed twice may number rows with ties differently.
        if (item.hasWindow) {
            throw UnsupportedConstructException.at(
                ctx,
                "reference to the alias `${id.text}` of a window function in the select list",
            )
        }
        note(ctx.start, "The reference to the select-list alias `${id.text}` was replaced by its expression")
        return item.expr
    }

    /**
     * A select-list alias in WHERE, GROUP BY, HAVING, QUALIFY, or an ORDER BY expression. Redshift documents aliases in
     * these clauses, but not which name wins when a FROM item also has a column of that name, so that case is
     * unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WHERE_clause.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_HAVING_clause.html
     */
    private fun clauseAlias(
        ctx: P.AtomVariableContext,
        id: Identifier.Simple,
        block: Block,
    ): Expr? {
        val candidates = block.items.filter { it.isAlias && matches(id, it.name.text) }
        if (candidates.isEmpty()) return null
        val clauseName =
            when (clause) {
                Clause.WHERE -> "WHERE"
                Clause.GROUP_BY -> "GROUP BY"
                Clause.HAVING -> "HAVING"
                Clause.QUALIFY -> "QUALIFY"
                else -> "ORDER BY"
            }
        if (isColumn(id, block)) {
            // The same column under its own name, `SELECT c AS c ... GROUP BY c`, is not a conflict.
            val column = (block.scope.resolveColumn(id) as? ColumnResolution.Column)?.binding
            if (column != null && candidates.all { it.expr == columnRef(column, id) }) return null
            throw UnsupportedConstructException.at(ctx, "`${id.text}` in $clauseName, which is both a column and a select-list alias")
        }
        val item =
            candidates.singleOrNull()
                ?: throw UnsupportedConstructException.at(ctx, "reference to the repeated select-list alias `${id.text}`")
        if ((clause == Clause.WHERE || clause == Clause.GROUP_BY) && (item.hasAggregate || item.hasWindow)) {
            throw InvalidQueryException.at(ctx, "aggregate or window function of the alias `${id.text}` in $clauseName")
        }
        if (clause == Clause.HAVING && item.hasWindow) {
            throw InvalidQueryException.at(
                ctx,
                "window function of the alias `${id.text}` in HAVING",
            )
        }
        // A window function computed twice may number rows with ties differently; QUALIFY refers to the item itself.
        if (clause == Clause.ORDER_BY && item.hasWindow) {
            throw UnsupportedConstructException.at(ctx, "ORDER BY expression with the alias `${id.text}` of a window function")
        }
        // In QUALIFY, the reference becomes the column of the item in a subquery; see RedshiftQueryBlocks.
        if (clause != Clause.QUALIFY) note(ctx.start, "The reference to the select-list alias `${id.text}` was replaced by its expression")
        return item.expr
    }

    /** True if a FROM item of the block or of an enclosing block can have the column [id]. */
    private fun isColumn(
        id: Identifier.Simple,
        block: Block,
    ): Boolean =
        when (block.scope.resolveColumn(id)) {
            is ColumnResolution.NotFound -> false
            else -> true
        }

    /**
     * `schema.table.column`: a column of an unaliased FROM table written with the schema of its FROM clause. The
     * qualifier before the table name is dropped (see [Paths.flatten]), because PartiQL reads `schema.table` in an
     * expression as the catalog table.
     */
    private fun schemaQualified(
        ctx: P.AtomVariableContext,
        id: Identifier.Simple,
        scope: Scope,
    ): Expr? {
        // The names of the path: `id`, then the field steps after it.
        val parts = mutableListOf(id)
        var node: ParserRuleContext = ctx.parent as ParserRuleContext
        while (true) {
            val path = node.parent as? P.PathFieldContext ?: break
            if (path.primaryExpr() !== node) break
            parts.add(simple(path.identifier()))
            node = path
        }
        val binding =
            scope.allVisibleBindings().firstOrNull { binding ->
                val table = binding.table ?: return@firstOrNull false
                table.size >= 2 && parts.size > table.size && table.indices.all { matches(parts[it], table[it].text) }
            } ?: return null
        val ref = bindingRef(binding, scope, qualified = false)
        paths.skip(ref, binding.table!!.size - 1)
        note(
            ctx.start,
            "The qualifier `${parts.take(binding.table.size - 1).joinToString(".") { it.text }}.` of the column reference was removed",
        )
        return ref
    }

    /** Checks that `t.c` names a column of the FROM item `t`, if its columns are known. */
    private fun checkColumn(
        ctx: P.AtomVariableContext,
        binding: Binding,
    ) {
        if (binding.kind != Binding.Kind.RELATION || binding.columns == null) return
        when (val path = ctx.parent.parent) {
            is P.PathFieldContext -> {
                val column = simple(path.identifier())
                if (!binding.hasColumn(
                        column,
                    )
                ) {
                    throw InvalidQueryException.at(path, "column `${binding.name.text}.${column.text}` does not exist")
                }
            }
            is P.PathElementContext -> throw InvalidQueryException.at(path, "`[...]` after the FROM item `${binding.name.text}`")
        }
    }

    /** True if the variable [ctx] is the root of a path: `t` in `t.c` or `t[0]`. */
    private fun isPathRoot(ctx: P.AtomVariableContext): Boolean {
        val primary = ctx.parent as? P.PrimaryAtomContext ?: return false
        return when (val path = primary.parent) {
            is P.PathFieldContext -> path.primaryExpr() === primary
            is P.PathElementContext -> path.primaryExpr() === primary
            else -> false
        }
    }

    /** The identifiers of a name `a.b.c`, or null if [ctx] is not a name. */
    private fun nameParts(ctx: P.PrimaryExprContext): List<Identifier.Simple>? =
        when (ctx) {
            is P.PathFieldContext -> nameParts(ctx.primaryExpr())?.plus(simple(ctx.identifier()))
            is P.PrimaryAtomContext -> (ctx.atom() as? P.AtomVariableContext)?.let { listOf(simple(it.identifier())) }
            else -> null
        }

    /** The identifier at the root of the path [ctx], if it is a name. */
    private fun bareRoot(ctx: P.PrimaryExprContext): Identifier.Simple? =
        when (ctx) {
            is P.PathFieldContext -> bareRoot(ctx.primaryExpr())
            is P.PathElementContext -> bareRoot(ctx.primaryExpr())
            is P.PrimaryAtomContext ->
                (ctx.atom() as? P.AtomVariableContext)?.identifier()?.let {
                    if (it is P.IdentifierNiladicContext) null else simple(it)
                }
            else -> null
        }

    /** The name of [ctx] if the expression is only a name, for example `c`. */
    private fun bareName(ctx: P.ExprContext): Identifier.Simple? {
        val primary = (valueOf(ctx) as? P.ValuePrimaryContext)?.primaryExpr() as? P.PrimaryAtomContext ?: return null
        val variable = primary.atom() as? P.AtomVariableContext ?: return null
        if (variable.identifier() is P.IdentifierNiladicContext) return null
        return simple(variable.identifier())
    }

    /** The value of a positive integer literal, if the expression is one, for a column position. */
    private fun position(ctx: P.ExprContext): Int? {
        val primary = (valueOf(ctx) as? P.ValuePrimaryContext)?.primaryExpr() as? P.PrimaryAtomContext ?: return null
        val literal = (primary.atom() as? P.AtomLiteralContext)?.literal() as? P.LiteralIntegerContext ?: return null
        return literal.text.toBigInteger().let { if (it > Int.MAX_VALUE.toBigInteger()) Int.MAX_VALUE else it.toInt() }
    }

    /*
     * GROUP BY
     */

    /**
     * `GROUP BY expr, ...`, `GROUP BY <position>`, `GROUP BY <alias>`, and `GROUP BY ALL`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_GROUP_BY_clause.html
     */
    private fun groupByKeys(
        ctx: P.GroupByClauseContext,
        block: Block,
    ): List<Expr> =
        inClause(Clause.GROUP_BY) {
            when (ctx) {
                is P.GroupByAllContext -> groupByAll(ctx, block)
                is P.GroupByListContext ->
                    ctx.groupingElement().map { element ->
                        when (element) {
                            is P.GroupingRollupContext -> throw UnsupportedConstructException.at(element, "GROUP BY ROLLUP")
                            is P.GroupingCubeContext -> throw UnsupportedConstructException.at(element, "GROUP BY CUBE")
                            is P.GroupingSetsContext -> throw UnsupportedConstructException.at(element, "GROUP BY GROUPING SETS")
                            is P.GroupingEmptyContext -> throw UnsupportedConstructException.at(element, "GROUP BY ()")
                            is P.GroupingExprContext -> groupingExpr(element.expr(), block)
                            else -> unexpected(element)
                        }
                    }
                else -> unexpected(ctx)
            }
        }

    private fun groupingExpr(
        ctx: P.ExprContext,
        block: Block,
    ): Expr {
        val k = position(ctx) ?: return expr(ctx)
        if (k < 1 || k > block.items.size) throw InvalidQueryException.at(ctx, "GROUP BY position $k is not in the select list")
        val item = block.items[k - 1]
        if (item.hasAggregate || item.hasWindow) {
            throw InvalidQueryException.at(
                ctx,
                "GROUP BY position $k refers to an aggregate or window function",
            )
        }
        val expr =
            item.expr ?: throw UnsupportedConstructException.at(ctx, "GROUP BY position of a `*` of a FROM item with unknown columns")
        note(ctx.start, "Redshift `GROUP BY <position>` was replaced by the select-list expression")
        return expr
    }

    /**
     * `GROUP BY ALL` groups by the select items without an aggregate. An item that is a constant, or that mixes an
     * aggregate with a column, is unsupported: Redshift does not document how it groups them.
     */
    private fun groupByAll(
        ctx: P.GroupByAllContext,
        block: Block,
    ): List<Expr> {
        val keys = mutableListOf<Expr>()
        for (item in block.items) {
            if (item.hasWindow) throw UnsupportedConstructException.at(ctx.ALL().symbol, "GROUP BY ALL with a window function")
            val expr =
                item.expr
                    ?: throw UnsupportedConstructException.at(ctx.ALL().symbol, "GROUP BY ALL with `*` of a FROM item with unknown columns")
            val exprCtx = (item.ctx as? P.SelectItemExprContext)?.expr()
            val hasColumn = exprCtx == null || containsColumn(exprCtx)
            when {
                item.hasAggregate && hasColumn && exprCtx != null && mixesColumnAndAggregate(exprCtx) ->
                    throw UnsupportedConstructException.at(item.ctx, "GROUP BY ALL with an item that mixes a column and an aggregate")
                item.hasAggregate -> {}
                !hasColumn -> throw UnsupportedConstructException.at(item.ctx, "GROUP BY ALL with a constant select item")
                else -> keys.add(expr)
            }
        }
        if (keys.isEmpty()) throw UnsupportedConstructException.at(ctx.ALL().symbol, "GROUP BY ALL without a column")
        note(ctx.ALL().symbol, "Redshift `GROUP BY ALL` was replaced by the select-list expressions without an aggregate")
        return keys
    }

    /*
     * ORDER BY
     */

    /** A sort item, and the select item that it refers to by position or name. */
    internal class SortKey(
        val sort: Sort,
        val item: Int?,
        val ctx: P.SortItemContext,
    )

    /**
     * A sort item of a query block: an expression, an output column name, or a position. Redshift sorts NULL values last
     * for ASC and first for DESC; the translator always writes the null order.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ORDER_BY_clause.html
     */
    private fun sortItem(
        ctx: P.SortItemContext,
        block: Block,
    ): SortKey {
        position(ctx.expr())?.let { k ->
            if (k < 1 || k > block.items.size) throw InvalidQueryException.at(ctx, "ORDER BY position $k is not in the select list")
            val item = block.items[k - 1]
            val expr =
                item.expr ?: throw UnsupportedConstructException.at(ctx, "ORDER BY position of a `*` of a FROM item with unknown columns")
            note(ctx.start, "Redshift `ORDER BY <position>` was replaced by the select-list expression")
            return SortKey(sortOf(ctx, expr), k - 1, ctx)
        }
        bareName(ctx.expr())?.let { name ->
            val matching = block.items.indices.filter { matches(name, block.items[it].name.text) }
            if (matching.size > 1 && matching.map { block.items[it].expr }.toSet().size > 1) {
                throw InvalidQueryException.at(ctx, "ORDER BY `${name.text}` is ambiguous")
            }
            matching.firstOrNull()?.let { i ->
                val expr =
                    block.items[i].expr
                        ?: throw UnsupportedConstructException.at(ctx, "ORDER BY of a `*` of a FROM item with unknown columns")
                return SortKey(sortOf(ctx, expr), i, ctx)
            }
        }
        val translated = inClause(Clause.ORDER_BY) { expr(ctx.expr()) }
        return SortKey(sortOf(ctx, translated), null, ctx)
    }

    /**
     * A sort item of a window specification. Redshift does not allow constants in a window ORDER BY.
     */
    internal fun windowSortItem(ctx: P.SortItemContext): Sort {
        if (position(ctx.expr()) != null) throw InvalidQueryException.at(ctx, "constant in a window ORDER BY")
        return sortOf(ctx, inClause(Clause.WINDOW) { expr(ctx.expr()) })
    }

    /** Translates an expression of a window specification (PARTITION BY). */
    internal fun windowExpr(ctx: P.ExprContext): Expr = inClause(Clause.WINDOW) { expr(ctx) }

    private fun sortOf(
        ctx: P.SortItemContext,
        expr: Expr,
    ): Sort {
        val descending = ctx.dir?.type == P.DESC
        val nullsFirst =
            when (ctx.nulls?.type) {
                P.FIRST -> true
                P.LAST -> false
                else -> descending
            }
        return sort(
            expr,
            if (descending) Order.DESC() else Order.ASC(),
            if (nullsFirst) Nulls.FIRST() else Nulls.LAST(),
        )
    }

    /*
     * LIMIT and OFFSET
     */

    /**
     * `LIMIT { count | ALL }` and `OFFSET start`. The values must be integer constants: Redshift rejects a column
     * reference or a negative value. `LIMIT ALL` is no limit.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ORDER_BY_clause.html
     */
    private fun limitOffset(ctx: P.LimitClauseContext?): Pair<Expr?, Expr?> {
        if (ctx == null) return null to null
        val limit =
            ctx.limit?.let { value ->
                if (value.ALL() != null) {
                    note(value.start, "Redshift `LIMIT ALL` was removed")
                    null
                } else {
                    integerConstant(value.valueExpr(), "LIMIT")
                }
            }
        val offset = ctx.offset?.let { integerConstant(it, "OFFSET") }
        return limit to offset
    }

    private fun integerConstant(
        ctx: P.ValueExprContext,
        clause: String,
    ): Expr {
        val primary = (ctx as? P.ValuePrimaryContext)?.primaryExpr() as? P.PrimaryAtomContext
        val literal = (primary?.atom() as? P.AtomLiteralContext)?.literal() as? P.LiteralIntegerContext
        if (literal != null) {
            if (literal.text.toBigInteger() > Int.MAX_VALUE.toBigInteger()) {
                throw InvalidQueryException.at(ctx, "$clause ${literal.text} is greater than 2147483647")
            }
            return exprLit(Literal.intNum(literal.text))
        }
        if (ctx is P.ValueSignedContext && ctx.sign.type == P.MINUS) throw InvalidQueryException.at(ctx, "$clause must not be negative")
        if (containsColumn(ctx)) throw InvalidQueryException.at(ctx, "$clause must not refer to a column")
        throw UnsupportedConstructException.at(ctx, "$clause expression")
    }

    /*
     * Window functions
     */

    /**
     * Translates a window function: a function call with an OVER clause or a null treatment, or `COUNT(*) OVER`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Window_functions.html
     */
    internal fun window(ctx: P.AtomContext): Expr {
        // Window functions are allowed only in the select list, ORDER BY, and QUALIFY.
        val name =
            when (clause) {
                Clause.SELECT, Clause.ORDER_BY, Clause.QUALIFY -> null
                Clause.WHERE -> "WHERE"
                Clause.GROUP_BY -> "GROUP BY"
                Clause.HAVING -> "HAVING"
                Clause.JOIN_CONDITION -> "a JOIN condition"
                Clause.WINDOW -> "a window specification"
                else -> "the FROM clause"
            }
        if (name != null) throw InvalidQueryException.at(ctx, "window function in $name")
        return windows.window(ctx)
    }

    /** Translates an expression of a clause; see [Paths]. */
    internal fun expr(ctx: P.ExprContext): Expr = paths.flatten(exprs.expr(ctx))

    private fun primary(ctx: P.PrimaryExprContext): Expr = paths.flatten(exprs.primaryExpr(ctx))

    /*
     * Identifiers
     */

    /**
     * Redshift folds identifiers to lower case, and the folded name is also the output column name. The translator
     * folds the same way, so that derived PartiQL column names agree with Redshift column names. Redshift folds only
     * the ASCII letters A to Z.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
     */
    internal fun simple(ctx: P.IdentifierContext): Identifier.Simple =
        when (ctx) {
            is P.IdentifierUnquotedContext, is P.IdentifierNonReservedContext -> Identifier.Simple.regular(foldAscii(ctx.text))
            is P.IdentifierQuotedContext -> {
                val text = ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
                if (caseSensitiveIdentifiers) Identifier.Simple.delimited(text) else Identifier.Simple.regular(foldAscii(text))
            }
            is P.IdentifierNiladicContext -> throw InvalidQueryException.at(
                ctx,
                "`${ctx.text}` is a reserved word; use a delimited identifier",
            )
            else -> unexpected(ctx)
        }

    internal fun setQuantifier(ctx: P.SetQuantifierContext?): SetQuantifier? =
        when {
            ctx == null -> null
            ctx.DISTINCT() != null -> SetQuantifier.DISTINCT()
            else -> SetQuantifier.ALL()
        }

    internal fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }

    internal fun unexpected(ctx: ParserRuleContext): Nothing = throw UnsupportedConstructException.at(ctx, "`${ctx.text}`")

    internal companion object {
        /** Folds the ASCII letters to lower case, as Redshift folds identifiers. */
        fun foldAscii(text: String): String =
            buildString(text.length) {
                for (c in text) append(if (c in 'A'..'Z') c + ('a' - 'A') else c)
            }

        /** Redshift aggregate functions: an item with one of them is not a GROUP BY key. */
        val AGGREGATES =
            setOf(
                "count", "sum", "avg", "min", "max", "listagg", "median", "stddev", "stddev_samp", "stddev_pop", "variance",
                "var_samp", "var_pop", "bool_and", "bool_or", "bit_and", "bit_or", "any_value", "percentile_cont",
                "percentile_disc", "approximate", "st_collect", "st_extent",
            )

        /** The value expression of [ctx], if it is not a boolean expression or a predicate. */
        fun valueOf(ctx: P.ExprContext): P.ValueExprContext? {
            val or = ctx.exprOr() as? P.OrBaseContext ?: return null
            val and = or.exprAnd() as? P.AndBaseContext ?: return null
            val not = and.exprNot() as? P.NotBaseContext ?: return null
            val predicate = not.predicate() as? P.PredicateBaseContext ?: return null
            return predicate.valueExpr()
        }

        /** True if [ctx] contains an aggregate call of its own query block. */
        fun containsAggregate(ctx: ParserRuleContext): Boolean =
            any(ctx) {
                when (it) {
                    is P.AtomFunctionCallContext -> it.overClause() == null && foldAscii(it.name.text) in AGGREGATES
                    is P.AtomCountStarContext -> it.overClause() == null
                    is P.AtomApproximateContext -> true
                    else -> false
                }
            }

        /** True if [ctx] contains a window function of its own query block. */
        fun containsWindow(ctx: ParserRuleContext): Boolean =
            any(ctx) {
                when (it) {
                    is P.AtomFunctionCallContext -> it.overClause() != null
                    is P.AtomCountStarContext -> it.overClause() != null
                    else -> false
                }
            }

        /** True if [ctx] contains a name (a column reference) of its own query block. */
        fun containsColumn(ctx: ParserRuleContext): Boolean = any(ctx) { it is P.AtomVariableContext }

        /** True if [ctx] has a column reference outside the arguments of its aggregate calls. */
        fun mixesColumnAndAggregate(ctx: ParserRuleContext): Boolean {
            fun visit(node: ParserRuleContext): Boolean {
                if (node is P.QueryContext) return false
                if (node is P.AtomFunctionCallContext && node.overClause() == null && foldAscii(node.name.text) in AGGREGATES) return false
                if (node is P.AtomCountStarContext || node is P.AtomApproximateContext) return false
                if (node is P.AtomVariableContext) return true
                return (0 until node.childCount).any { i -> (node.getChild(i) as? ParserRuleContext)?.let(::visit) == true }
            }
            return visit(ctx)
        }

        /** True if a node of [ctx], outside its subqueries, satisfies [predicate]. */
        private fun any(
            ctx: ParserRuleContext,
            predicate: (ParserRuleContext) -> Boolean,
        ): Boolean {
            if (ctx is P.QueryContext) return false
            if (predicate(ctx)) return true
            for (i in 0 until ctx.childCount) {
                val child = ctx.getChild(i) as? ParserRuleContext ?: continue
                if (any(child, predicate)) return true
            }
            return false
        }

        /** Rebuilds the SFW [body] with a new select clause. */
        fun sfw(
            body: QueryBody.SFW,
            select: Select = body.select,
        ): QueryBody.SFW = QueryBody.SFW(select, body.exclude, body.from, body.let, body.where, body.groupBy, body.having, body.window)

        fun keyOf(
            expr: Expr,
            alias: Identifier.Simple,
        ) = groupByKey(expr, alias)

        fun groupByOf(keys: List<org.partiql.ast.GroupBy.Key>) = groupBy(GroupByStrategy.FULL(), keys, null)
    }
}
