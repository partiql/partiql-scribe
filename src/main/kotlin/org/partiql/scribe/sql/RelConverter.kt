package org.partiql.scribe.sql

import org.partiql.ast.Ast
import org.partiql.ast.Ast.exclude
import org.partiql.ast.Ast.excludePath
import org.partiql.ast.Ast.excludeStepCollIndex
import org.partiql.ast.Ast.excludeStepCollWildcard
import org.partiql.ast.Ast.excludeStepStructField
import org.partiql.ast.Ast.excludeStepStructWildcard
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprWindowFunction
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
import org.partiql.ast.Ast.windowClause
import org.partiql.ast.Ast.windowClauseDefinition
import org.partiql.ast.Ast.windowPartition
import org.partiql.ast.Ast.windowSpecification
import org.partiql.ast.Ast.withListElement
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
import org.partiql.ast.WindowClause
import org.partiql.ast.WindowFunctionNullTreatment
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.WindowSpecification
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
import org.partiql.plan.WindowFunctionNode
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
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rel.RelWith
import org.partiql.plan.rex.RexLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.toIdentifier

/**
 * Data class to hold the state to create an [ExprQuerySet].
 */
public data class ExprQuerySetFactory(
    public var queryBody: QueryBodyFactory,
    public var limit: Expr? = null,
    public var offset: Expr? = null,
    public var orderBy: OrderBy? = null,
    public var with: With? = null,
) {
    /**
     * Converts the [ExprQuerySetFactory] to an [ExprQuerySet].
     */
    public fun toExprQuerySet(): ExprQuerySet {
        return ExprQuerySet.builder()
            .body(queryBody.toQueryBody())
            .limit(limit)
            .offset(offset)
            .orderBy(orderBy)
            .with(with)
            .build()
    }
}

/**
 * Holds the state to create a [QueryBody].
 */
public interface QueryBodyFactory {
    /**
     * Converts the [QueryBodyFactory] to a [QueryBody].
     */
    public fun toQueryBody(): QueryBody
}

/**
 * Holds the state to create a [QueryBody.SFW].
 */
internal data class QueryBodySFWFactory(
    var select: Select? = null,
    var exclude: Exclude? = null,
    var from: From? = null,
    var let: Let? = null,
    var where: Expr? = null,
    var groupBy: GroupBy? = null,
    var having: Expr? = null,
    var aggregations: List<Expr>? = null,
    var windowFunctions: MutableList<Expr>? = null,
    var windowDefinitions: MutableList<WindowClause.Definition>? = null,
) : QueryBodyFactory {
    override fun toQueryBody(): QueryBody {
        val window = windowDefinitions?.let { windowClause(windowDefinitions!!) }
        return QueryBody.SFW.builder()
            .select(select!!)
            .exclude(exclude)
            .from(from!!)
            .let(let)
            .where(where)
            .groupBy(groupBy)
            .having(having)
            .windowClause(window)
            .build()
    }
}

/**
 * Holds the state to create a [QueryBody.SetOp].
 */
internal class QueryBodySetOpFactory(
    var lhs: ExprQuerySet,
    var rhs: ExprQuerySet,
    var setOp: SetOp,
) : QueryBodyFactory {
    override fun toQueryBody(): QueryBody {
        return QueryBody.SetOp.builder()
            .lhs(lhs)
            .rhs(rhs)
            .type(setOp)
            .build()
    }
}

/**
 * Converts a [Rel] to an [ExprQuerySetFactory]. Use this class to handle any [Rel]s that need to be converted in the
 * plan to AST transformation.
 */
public open class RelConverter(
    internal val transform: PlanToAst,
    internal val context: ScribeContext,
) : OperatorVisitor<ExprQuerySetFactory, Unit> {
    internal val listener = context.getProblemListener()

    /**
     * Converts a [Rel] to an [ExprQuerySetFactory].
     */
    public fun apply(
        rel: Rel,
        ctx: Unit,
    ): ExprQuerySetFactory {
        return visit(rel, ctx)
    }

    override fun defaultReturn(
        operator: Operator,
        ctx: Unit,
    ): ExprQuerySetFactory {
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
    ): ExprQuerySetFactory {
        return defaultReturn(operator, ctx)
    }

    internal fun visitRelSFW(
        node: Rel,
        ctx: Unit,
    ): QueryBodySFWFactory {
        val exprQuerySetBuilder = visit(node, ctx)
        return exprQuerySetBuilder.queryBody as QueryBodySFWFactory
    }

    // --[Rel]-----------------------------------------------------------------------------------------------------------
    override fun visitAggregate(
        rel: RelAggregate,
        ctx: Unit,
    ): ExprQuerySetFactory {
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
        return ExprQuerySetFactory(
            queryBody = sfw,
        )
    }

    override fun visitCorrelate(
        rel: RelCorrelate,
        ctx: Unit,
    ): ExprQuerySetFactory {
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
    ): ExprQuerySetFactory {
        val inputQuerySet = visit(rel.input, ctx)
        val sfw = inputQuerySet.queryBody as QueryBodySFWFactory
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
    ): ExprQuerySetFactory {
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
    ): ExprQuerySetFactory {
        val relCtx = visitRelSFW(rel.input, ctx)
        val rexToSql = transform.getRexConverter(Locals(rel.type.fields.toList()))
        relCtx.exclude =
            exclude(
                rel.exclusions.flatMap { exclusion ->
                    exclusionToExcludePaths(exclusion, rexToSql)
                },
            )
        return ExprQuerySetFactory(
            queryBody = relCtx,
        )
    }

    override fun visitFilter(
        rel: RelFilter,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val sfw = visitRelSFW(rel.input, ctx)
        if (rel.input is RelAggregate) {
            val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList(), sfw.aggregations ?: emptyList()))
            sfw.having = rexConverter.apply(rel.predicate)
        } else {
            val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
            sfw.where = rexConverter.apply(rel.predicate)
        }
        return ExprQuerySetFactory(
            queryBody = sfw,
        )
    }

    override fun visitIntersect(
        rel: RelIntersect,
        ctx: Unit,
    ): ExprQuerySetFactory {
        return visitSetOp(rel.left, rel.right, rel.isAll, SetOpType.INTERSECT())
    }

    override fun visitIterate(
        rel: RelIterate,
        ctx: Unit,
    ): ExprQuerySetFactory {
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
    ): ExprQuerySetFactory {
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
        return ExprQuerySetFactory(
            queryBody = lhs,
        )
    }

    override fun visitLimit(
        rel: RelLimit,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val input = visit(rel.input, Unit)
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        return input.copy(
            limit = rexConverter.visit(rel.limit, Unit),
        )
    }

    override fun visitOffset(
        rel: RelOffset,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val input = visit(rel.input, Unit)
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        return input.copy(
            offset = rexConverter.visit(rel.offset, Unit),
        )
    }

    override fun visitProject(
        rel: RelProject,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val input = rel.input
        val projections = rel.projections
        val inputQuerySet = visit(rel.input, ctx)
        val sfw = inputQuerySet.queryBody as QueryBodySFWFactory

        // Group by keys needs to be added to aggregations to match the original column reference.
        val groupByExprs = sfw.groupBy?.keys?.map { it.expr } ?: emptyList()
        val allAggregations = (sfw.aggregations ?: emptyList()) + groupByExprs

        val locals =
            Locals(
                env = input.type.fields.toList(),
                aggregations = allAggregations,
                windowFunctions = sfw.windowFunctions ?: emptyList(),
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
    ): ExprQuerySetFactory {
        val fromRex = rel.rex

        // Init a new SFW context
        val sfw = QueryBodySFWFactory()
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
        return ExprQuerySetFactory(
            queryBody = sfw,
        )
    }

    internal fun convertCollationOrder(order: Collation.Order): Order {
        return when (order.code()) {
            Collation.Order.ASC -> Order.ASC()
            Collation.Order.DESC -> Order.DESC()
            else -> {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                        message = "Unsupported ORDER BY order direction $order",
                    ),
                )
            }
        }
    }

    internal fun convertCollationNulls(nulls: Collation.Nulls): Nulls {
        return when (nulls.code()) {
            Collation.Nulls.FIRST -> Nulls.FIRST()
            Collation.Nulls.LAST -> Nulls.LAST()
            else -> {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                        message = "Unsupported ORDER BY null ordering $nulls",
                    ),
                )
            }
        }
    }

    override fun visitSort(
        rel: RelSort,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val input = visit(rel.input, Unit)

        val locals =
            when (input.queryBody) {
                is QueryBodySFWFactory -> {
                    val sfw = input.queryBody as QueryBodySFWFactory

                    Locals(
                        env = rel.type.fields.toList(),
                        // OrderBy may contain aggregation function or alias from select
                        aggregations = sfw.aggregations ?: emptyList(),
                    )
                }
                is QueryBodySetOpFactory -> {
                    Locals(
                        env = rel.type.fields.toList(),
                    )
                }

                else ->
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                            message = "Unsupported query body type for ORDER BY: ${input.queryBody::class.simpleName}.",
                        ),
                    )
            }

        val rexConverter = transform.getRexConverter(locals)
        val sorts =
            rel.collations.map { collation ->
                val orderByField = rexConverter.apply(collation.column)
                val order = convertCollationOrder(collation.order)
                val nullOrder = convertCollationNulls(collation.nulls)
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
    ): ExprQuerySetFactory {
        val lhs = visit(left, Unit) // ctx as second arg
        val rhs = visit(right, Unit) // ctx as second arg
        val setq =
            when (isAll) {
                true -> SetQuantifier.ALL()
                false -> SetQuantifier.DISTINCT()
            }
        return ExprQuerySetFactory(
            QueryBodySetOpFactory(
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
    ): ExprQuerySetFactory {
        return visitSetOp(rel.left, rel.right, rel.isAll, SetOpType.UNION())
    }

    override fun visitUnpivot(
        rel: RelUnpivot,
        ctx: Unit,
    ): ExprQuerySetFactory {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "UNPIVOT is not yet supported",
            ),
        )
    }

    override fun visitWith(
        rel: RelWith,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        val querySet = visit(rel.input, ctx)
        val withElements =
            rel.elements.map { element ->
                val name = element.name
                val repr = rexConverter.apply(element.representation)
                withListElement(
                    queryName = Identifier.Simple.delimited(name),
                    asQuery = repr as ExprQuerySet,
                    columnList = null,
                )
            }
        querySet.with =
            Ast.with(
                elements = withElements,
                isRecursive = false,
            )
        return querySet
    }

    override fun visitWindow(
        rel: RelWindow,
        ctx: Unit,
    ): ExprQuerySetFactory {
        val sfw = visitRelSFW(rel.input, ctx)
        val rexConverter = transform.getRexConverter(Locals(rel.input.type.fields.toList()))

        // Convert window functions to AST expressions
        val windowFunctionExprs =
            rel.windowFunctions.map { windowFunction ->
                convertWindowFunctionToExpr(windowFunction, rel, rexConverter)
            }

        // Store window functions for projection reference
        // Window functions are appended after input fields in the output schema
        if (windowFunctionExprs.isNotEmpty()) {
            sfw.windowFunctions =
                sfw.windowFunctions?.apply { addAll(windowFunctionExprs) } ?: windowFunctionExprs.toMutableList()
        }

        // Create named window definitions if needed
        val namedWindow = createNamedWindowDefinition(rel, rexConverter)
        if (namedWindow != null) {
            // When we create relWindow, we visit each window sequentially.
            // However, we should reversely add to the window list when we reconstruct the window clause.
            sfw.windowDefinitions =
                sfw.windowDefinitions?.apply { add(0, namedWindow) } ?: mutableListOf(namedWindow)
        }
        return ExprQuerySetFactory(
            queryBody = sfw,
        )
    }

    private fun convertWindowFunctionToExpr(
        windowFunction: WindowFunctionNode,
        rel: RelWindow,
        rexConverter: RexConverter,
    ): Expr {
        val windowType = createWindowFunctionType(windowFunction, rexConverter)

        val windowSpec =
            if (rel.name != null) {
                // Named window - reference by name
                windowSpecification(
                    existingName = Identifier.Simple.delimited(rel.name),
                    partitionClause = null,
                    orderByClause = null,
                )
            } else {
                // Inline window specification
                createInlineWindowSpecification(rel, rexConverter)
            }

        return exprWindowFunction(windowType, windowSpec)
    }

    internal fun createWindowFunctionType(
        windowFunction: WindowFunctionNode,
        rexConverter: RexConverter,
    ): WindowFunctionType {
        val functionName = windowFunction.signature.name
        val arguments = windowFunction.arguments

        val nullTreatment =
            if (windowFunction.signature.isIgnoreNulls) {
                WindowFunctionNullTreatment.IGNORE_NULLS()
            } else {
                WindowFunctionNullTreatment.RESPECT_NULLS()
            }

        return when (functionName.uppercase()) {
            "ROW_NUMBER" -> WindowFunctionType.RowNumber()
            "RANK" -> WindowFunctionType.Rank()
            "DENSE_RANK" -> WindowFunctionType.DenseRank()
            "PERCENT_RANK" -> WindowFunctionType.PercentRank()
            "CUME_DIST" -> WindowFunctionType.CumeDist()
            "LAG", "LEAD" -> {
                val expr = rexConverter.apply(arguments[0])
                val offset =
                    if (arguments[1] is RexLit) {
                        (arguments[1] as RexLit).datum.long
                    } else {
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                                message = "Unexpected offset type: ${arguments[1]}",
                            ),
                        )
                    }
                val default = rexConverter.apply(arguments[2])

                if (functionName.uppercase() == "LAG") {
                    WindowFunctionType.Lag(
                        expr,
                        offset,
                        default,
                        nullTreatment,
                    )
                } else {
                    WindowFunctionType.Lead(
                        expr,
                        offset,
                        default,
                        nullTreatment,
                    )
                }
            }
            else -> {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                        message = "Unsupported window function: $functionName",
                    ),
                )
            }
        }
    }

    private fun createNamedWindowDefinition(
        rel: RelWindow,
        rexConverter: RexConverter,
    ): WindowClause.Definition? {
        return if (rel.name != null) {
            val windowSpec = createInlineWindowSpecification(rel, rexConverter)
            // Create window definition for named window
            windowClauseDefinition(
                name = Identifier.Simple.delimited(rel.name),
                spec = windowSpec,
            )
        } else {
            null
        }
    }

    internal fun createInlineWindowSpecification(
        rel: RelWindow,
        rexConverter: RexConverter,
    ): WindowSpecification {
        val partitionClause =
            if (rel.partitions.isNotEmpty()) {
                rel.partitions.map { partition ->
                    val columnReference = rexConverter.apply(partition).toIdentifier()
                    if (columnReference == null) {
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                                message = "Unsupported partition key: $partition",
                            ),
                        )
                    }
                    windowPartition(columnReference)
                }
            } else {
                emptyList()
            }

        val orderClause =
            if (rel.collations.isNotEmpty()) {
                val sorts =
                    rel.collations.map { collation ->
                        val orderByField = rexConverter.apply(collation.column)
                        val order = convertCollationOrder(collation.order)
                        val nullOrder = convertCollationNulls(collation.nulls)
                        sort(orderByField, order, nullOrder)
                    }
                orderBy(sorts)
            } else {
                null
            }

        return windowSpecification(
            existingName = null,
            partitionClause = partitionClause,
            orderByClause = orderClause,
        )
    }

    // Private helpers
    private fun RelType.dump(): String {
        if (this.fields.isEmpty()) return "< empty >"
        val pairs = this.fields.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }
}
