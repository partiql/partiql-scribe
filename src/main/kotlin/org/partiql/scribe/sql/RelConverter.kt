package org.partiql.scribe.sql

import org.partiql.ast.Ast.exclude
import org.partiql.ast.Ast.excludePath
import org.partiql.ast.Ast.excludeStepCollIndex
import org.partiql.ast.Ast.excludeStepCollWildcard
import org.partiql.ast.Ast.excludeStepStructField
import org.partiql.ast.Ast.excludeStepStructWildcard
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.fromJoin
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.Ast.sort
import org.partiql.ast.Exclude
import org.partiql.ast.ExcludePath
import org.partiql.ast.ExcludeStep
import org.partiql.ast.From
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SelectList
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.SetOp
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprVarRef
import org.partiql.plan.Collation
import org.partiql.plan.Exclusion
import org.partiql.plan.JoinType
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelDistinct
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelType
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rel.RelUnpivot
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem

public open class RelConverter(
    private val transform: PlanToAst,
    private val context: ScribeContext,
) : OperatorVisitor<RelConverter.ExprQuerySetBuilder, Unit> {
    private val listener = context.getProblemListener()

    // TODO ALAN make this internal and make the `RelConverter` return an interface that implements `toExprQuerySet`
    public data class ExprQuerySetBuilder(
        /*
        QueryBody
        - Set
            - LHS (ExprQuerySet | Expr)
            - RHS (ExprQuerySet | Expr)
        - SFW
            - SELECT
            - FROM
            - WHERE
            ...
         */
        public var queryBody: QueryBodyBuilder,
        public val limit: Expr? = null,
        public val offset: Expr? = null,
        public val orderBy: OrderBy? = null,
        public val with: With? = null,
    ) {
        internal fun toExprQuerySet(): ExprQuerySet {
            return ExprQuerySet.builder()
                .body(queryBody.toQueryBody())
                .limit(limit)
                .offset(offset)
                .orderBy(orderBy)
                .with(with)
                .build()
        }
    }

    // QueryBodyBuilder
    public abstract class QueryBodyBuilder() {
        public abstract fun toQueryBody(): QueryBody
    }

    internal data class QueryBodyBuilderSFW(
        public var select: Select? = null,
        public var exclude: Exclude? = null,
        public var from: From? = null,
        public var let: Let? = null,
        public var where: Expr? = null,
        public var groupBy: GroupBy? = null,
        public var having: Expr? = null,
        public var aggregations: List<Expr>? = null,
    ) : QueryBodyBuilder() {
        override fun toQueryBody(): QueryBody {
            return QueryBody.SFW.builder()
                .select(select!!)
                .exclude(exclude)
                .from(from!!)
                .let(let)
                .where(where)
                .groupBy(groupBy)
                .having(having)
                .build()
        }
    }

    internal class QueryBodyBuilderSetOp(
        public var lhs: ExprQuerySet,
        public var rhs: ExprQuerySet,
        public var setOp: SetOp,
    ) : QueryBodyBuilder() {
        override fun toQueryBody(): QueryBody {
            return QueryBody.SetOp.builder()
                .lhs(lhs)
                .rhs(rhs)
                .type(setOp)
                .build()
        }
    }

    public fun apply(
        rel: Rel,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        return visit(rel, ctx)
    }

    override fun defaultReturn(
        operator: Operator,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "$operator is not yet supported",
            ),
        )
    }

    override fun defaultVisit(
        operator: Operator,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        return defaultReturn(operator, ctx)
    }

    private fun visitRelSFW(
        node: Rel,
        ctx: Unit,
    ): QueryBodyBuilderSFW {
        val exprQuerySetBuilder = visit(node, ctx)
        return exprQuerySetBuilder.queryBody as QueryBodyBuilderSFW
    }

    // --[Rel]-----------------------------------------------------------------------------------------------------------
    override fun visitAggregate(
        rel: RelAggregate,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val sfw = visitRelSFW(rel.input, ctx)
        val rexToSql = transform.getRexConverter(Locals(rel.input.type.fields.toList()))
        if (rel.groups.isNotEmpty()) {
            sfw.groupBy =
                groupBy(
                    strategy = GroupByStrategy.FULL(),
                    keys =
                        rel.groups.mapIndexed { i, rex ->
                            groupByKey(
                                expr = rexToSql.apply(rex),
                            )
                        },
                    asAlias = null,
                )
        }
        val aggregations =
            rel.measures.map { agg ->
                val args = agg.args.map { rexToSql.apply(it) }
                exprCall(
                    function = Identifier.regular(agg.agg.signature.name),
                    args = args,
                    setq =
                        when (agg.isDistinct) {
                            true -> SetQuantifier.DISTINCT()
                            false -> null
                        },
                )
            }
        sfw.aggregations = aggregations
        return ExprQuerySetBuilder(
            queryBody = sfw,
        )
    }

    override fun visitCorrelate(
        rel: RelCorrelate,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "CORRELATE is not yet supported",
            ),
        )
    }

    override fun visitDistinct(
        rel: RelDistinct,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val inputQuerySet = visit(rel.input, ctx)
        val sfw = inputQuerySet.queryBody as QueryBodyBuilderSFW
        sfw.select =
            when (val initSelect = sfw.select) {
                is SelectValue -> selectValue(constructor = initSelect.constructor, setq = SetQuantifier.DISTINCT())
                is SelectList -> selectList(items = initSelect.items, setq = SetQuantifier.DISTINCT())
                is SelectStar -> selectStar(setq = SetQuantifier.DISTINCT())
                else ->
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                            message = "Unsupported select node for DISTINCT ${rel.input}",
                        ),
                    )
            }
        return inputQuerySet
    }

    override fun visitExcept(
        rel: RelExcept,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        return visitSetOp(rel.left, rel.right, rel.isAll, SetOpType.EXCEPT())
    }

    private fun Exclusion.Item.toExcludeStep(): ExcludeStep {
        return when (this) {
            is Exclusion.CollWildcard -> excludeStepCollWildcard()
            is Exclusion.CollIndex -> excludeStepCollIndex(this.getIndex())
            is Exclusion.StructKey -> excludeStepStructField(Identifier.Simple.delimited(this.getKey()))
            is Exclusion.StructSymbol -> excludeStepStructField(Identifier.Simple.regular(this.getSymbol()))
            is Exclusion.StructWildcard -> excludeStepStructWildcard()
            else -> {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_OPERATION,
                        message = "Unexpected exclusion item: $this",
                    ),
                )
            }
        }
    }

    private fun exclusionToExcludePaths(
        exclusion: Exclusion,
        rexToSql: RexConverter,
    ): List<ExcludePath> {
        val res = mutableListOf<ExcludePath>()
        val root = rexToSql.visitRex(exclusion.getVar(), Unit) as ExprVarRef
        val stack = ArrayDeque<Pair<Exclusion.Item, List<ExcludeStep>>>()
        exclusion.getItems().forEach { item ->
            stack.addLast(item to listOf(item.toExcludeStep()))
        }
        while (stack.isNotEmpty()) {
            val (item, curSteps) = stack.removeLast()
            if (!item.hasItems()) {
                res.add(
                    excludePath(
                        varRef = root,
                        excludeSteps = curSteps,
                    ),
                )
            } else {
                // Use `.reversed()` here to preserve original order
                item.getItems().reversed().forEach { subItem ->
                    stack.addLast(
                        subItem to curSteps + subItem.toExcludeStep(),
                    )
                }
            }
        }
        return res
    }

    override fun visitExclude(
        rel: RelExclude,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val relCtx = visitRelSFW(rel.input, ctx)
        val rexToSql = transform.getRexConverter(Locals(rel.type.fields.toList()))
        relCtx.exclude =
            exclude(
                rel.exclusions.flatMap { exclusion ->
                    exclusionToExcludePaths(exclusion, rexToSql)
                },
            )
        return ExprQuerySetBuilder(
            queryBody = relCtx,
        )
    }

    override fun visitFilter(
        rel: RelFilter,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val sfw = visitRelSFW(rel.input, ctx)
        if (rel.input is RelAggregate) {
            val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList(), sfw.aggregations ?: emptyList()))
            sfw.having = rexConverter.apply(rel.predicate)
        } else {
            val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
            sfw.where = rexConverter.apply(rel.predicate)
        }
        return ExprQuerySetBuilder(
            queryBody = sfw,
        )
    }

    override fun visitIntersect(
        rel: RelIntersect,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        return visitSetOp(rel.left, rel.right, rel.isAll, SetOpType.INTERSECT())
    }

    override fun visitIterate(
        rel: RelIterate,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "ITERATE is not yet supported",
            ),
        )
    }

    private fun <T> assertNotNull(v: T?): T {
        if (v == null) {
            listener.report(
                ScribeProblem.simpleError(
                    code = ScribeProblem.INTERNAL_ERROR,
                    "Expect value to not be `null`",
                ),
            )
        }
        return v!!
    }

    override fun visitJoin(
        rel: RelJoin,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val lhs = visitRelSFW(rel.left, ctx)
        val lhsFrom = assertNotNull(lhs.from)
        assert(lhsFrom.tableRefs.size == 1)
        val rhs = visitRelSFW(rel.right, ctx)
        val rhsFrom = assertNotNull(rhs.from)
        assert(rhsFrom.tableRefs.size == 1)
        val locals = Locals(rel.left.type.fields.toList() + rel.right.type.fields.toList())
        val condition = transform.getRexConverter(locals).apply(rel.condition)
        val joinType =
            when (rel.joinType.code()) {
                JoinType.LEFT -> org.partiql.ast.JoinType.LEFT()
                JoinType.RIGHT -> org.partiql.ast.JoinType.RIGHT()
                JoinType.FULL -> org.partiql.ast.JoinType.FULL()
                JoinType.INNER -> org.partiql.ast.JoinType.INNER()
                else -> {
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.INTERNAL_ERROR,
                            "Unsupported join type: ${rel.joinType}",
                        ),
                    )
                }
            }
        lhs.from =
            from(
                tableRefs =
                    listOf(
                        fromJoin(
                            lhs = lhsFrom.tableRefs.first(),
                            rhs = rhsFrom.tableRefs.first(),
                            joinType = joinType,
                            condition = condition,
                        ),
                    ),
            )
        return ExprQuerySetBuilder(
            queryBody = lhs,
        )
    }

    override fun visitLimit(
        rel: RelLimit,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val input = visit(rel.input, Unit)
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        return input.copy(
            limit = rexConverter.visit(rel.limit, Unit),
        )
    }

    override fun visitOffset(
        rel: RelOffset,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val input = visit(rel.input, Unit)
        return input.copy(
            offset = transform.getRexConverter(Locals.EMPTY).visit(rel.offset, Unit),
        )
    }

    override fun visitProject(
        rel: RelProject,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val input = rel.input
        val projections = rel.projections
        val inputQuerySet = visit(rel.input, ctx)
        val sfw = inputQuerySet.queryBody as QueryBodyBuilderSFW

        val locals =
            Locals(
                env = input.type.fields.toList(),
                aggregations = sfw.aggregations ?: emptyList(),
            )

        val rexConverter = transform.getRexConverter(locals)
        val type = rel.type
        if (type.fields.size != projections.size) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.INVALID_PLAN,
                    message = "Malformed plan, relation output types does not match projections",
                ),
            )
        }
        val selectValue =
            selectValue(
                constructor = rexConverter.apply(projections.first()),
                setq = null,
            )
        val rewrittenSelect = convertSelectValueToSqlSelect(selectValue)
        sfw.select = rewrittenSelect
        return inputQuerySet
    }

    private fun convertSelectValueToSqlSelect(select: Select): Select {
        return when (select) {
            is SelectValue -> {
                val constructor = select.constructor
                val projectionItems =
                    if (constructor is ExprStruct) {
                        constructor.fields.map { field ->
                            val key = field.name
                            val value = field.value
                            if (key !is ExprLit || key.lit.code() != Literal.STRING) {
                                return select
                            }
                            val keyName = key.lit.stringValue()
                            // valid key-value pair
                            selectItemExpr(
                                expr = value,
                                asAlias = Identifier.Simple.delimited(keyName),
                            )
                        }
                    } else {
                        return select
                    }
                selectList(
                    items = projectionItems,
                    setq = select.setq,
                )
            }
            else -> select
        }
    }

    override fun visitScan(
        rel: RelScan,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val fromRex = rel.rex

        // Init a new SFW context
        val sfw = QueryBodyBuilderSFW()
        // Validate scan type
        val type = rel.type
        if (type.fields.size != 1) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    code = ScribeProblem.INVALID_PLAN,
                    message = "Invalid SCAN schema, expected a single PTypeField but found ${type.dump()}",
                ),
            )
        }
        val rexConverter =
            transform.getRexConverter(
                Locals(
                    env = type.fields.toList(),
                    aggregations = emptyList(),
                    // no projections
                ),
            )
        // convert to FROM clause
        sfw.from =
            from(
                tableRefs =
                    listOf(
                        fromExpr(
                            expr = rexConverter.apply(fromRex),
                            // FROM <rex>
                            fromType = FromType.SCAN(),
                            asAlias = Identifier.Simple.delimited(type.fields[0].name),
                        ),
                    ),
            )
        return ExprQuerySetBuilder(
            queryBody = sfw,
        )
    }

    override fun visitSort(
        rel: RelSort,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        val input = visit(rel.input, Unit)
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        val sorts =
            rel.collations.map { collation ->
                val orderByField = rexConverter.apply(collation.column)
                val order =
                    when (collation.order.code()) {
                        Collation.Order.ASC -> Order.ASC()
                        Collation.Order.DESC -> Order.DESC()
                        else ->
                            listener.reportAndThrow(
                                ScribeProblem.simpleError(
                                    code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                                    message = "Unsupported ORDER BY order direction ${collation.order}",
                                ),
                            )
                    }
                val nullOrder =
                    when (collation.nulls.code()) {
                        Collation.Nulls.FIRST -> Nulls.FIRST()
                        Collation.Nulls.LAST -> Nulls.LAST()
                        else ->
                            listener.reportAndThrow(
                                ScribeProblem.simpleError(
                                    code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                                    message = "Unsupported ORDER BY null ordering ${collation.nulls}",
                                ),
                            )
                    }
                sort(orderByField, order, nullOrder)
            }
        return input.copy(
            orderBy = orderBy(sorts),
        )
    }

    private fun visitSetOp(
        left: Rel,
        right: Rel,
        isAll: Boolean,
        setOpType: SetOpType,
    ): ExprQuerySetBuilder {
        val lhs = visit(left, Unit) // ctx as second arg
        val rhs = visit(right, Unit) // ctx as second arg
        val setq =
            when (isAll) {
                true -> SetQuantifier.ALL()
                false -> SetQuantifier.DISTINCT()
            }
        return ExprQuerySetBuilder(
            QueryBodyBuilderSetOp(
                lhs = lhs.toExprQuerySet(),
                rhs = rhs.toExprQuerySet(),
                setOp =
                    SetOp.builder()
                        .setOpType(setOpType)
                        .setq(setq)
                        .build(),
            ),
            limit = null,
            offset = null,
            orderBy = null,
            with = null,
        )
    }

    override fun visitUnion(
        rel: RelUnion,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        return visitSetOp(rel.left, rel.right, rel.isAll, SetOpType.UNION())
    }

    override fun visitUnpivot(
        rel: RelUnpivot,
        ctx: Unit,
    ): ExprQuerySetBuilder {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "UNPIVOT is not yet supported",
            ),
        )
    }

    // Private helpers
    private fun RelType.dump(): String {
        if (this.fields.isEmpty()) return "< empty >"
        val pairs = this.fields.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }
}
