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
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.Exclude
import org.partiql.ast.ExcludePath
import org.partiql.ast.ExcludeStep
import org.partiql.ast.From
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprVarRef
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

// TODO move to utils
public fun RelConverter.RelContext.toQueryBodySFW(): QueryBody.SFW {
    assert(this.select != null)
    return queryBodySFW(
        select = this.select!!,
        exclude = this.exclude,
        from = this.from!!,
        let = this.let,
        where = this.where,
        groupBy = this.groupBy,
        having = this.having,
    )
}

public open class RelConverter(
    private val transform: PlanToAst,
    private val context: ScribeContext,
) : OperatorVisitor<RelConverter.RelContext, Unit> {
    private val listener = context.getErrorListener()

    public class RelContext(
        public var select: Select? = null,
        public var exclude: Exclude? = null,
        public var from: From? = null,
        public var let: Let? = null,
        public var where: Expr? = null,
        public var groupBy: GroupBy? = null,
        public var having: Expr? = null,
        public var aggregations: List<Expr>? = null,
    )

    public fun apply(
        rel: Rel,
        ctx: Unit,
    ): RelContext {
        return visitRel(rel, ctx)
    }

    override fun defaultReturn(
        operator: Operator,
        ctx: Unit,
    ): RelContext {
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
    ): RelContext {
        return defaultReturn(operator, ctx)
    }

    public fun visitRel(
        node: Rel,
        ctx: Unit,
    ): RelContext = visit(node, ctx)

    // --[Rel]-----------------------------------------------------------------------------------------------------------
    override fun visitAggregate(
        rel: RelAggregate,
        ctx: Unit,
    ): RelContext {
        val sfw = visitRel(rel.input, ctx)
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
        return sfw
    }

    override fun visitCorrelate(
        rel: RelCorrelate,
        ctx: Unit,
    ): RelContext {
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
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "DISTINCT is not yet supported",
            ),
        )
    }

    override fun visitExcept(
        rel: RelExcept,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "EXCEPT is not yet supported",
            ),
        )
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
    ): RelContext {
        val relCtx = visitRel(rel.input, ctx)
        val rexToSql = transform.getRexConverter(Locals(rel.type.fields.toList()))
        relCtx.exclude =
            exclude(
                rel.exclusions.flatMap { exclusion ->
                    exclusionToExcludePaths(exclusion, rexToSql)
                },
            )
        return relCtx
    }

    override fun visitFilter(
        rel: RelFilter,
        ctx: Unit,
    ): RelContext {
        val sfw = visitRel(rel.input, ctx)
        // validate filter type
        val rexConverter = transform.getRexConverter(Locals(rel.type.fields.toList()))
        sfw.where = rexConverter.apply(rel.predicate)
        return sfw
    }

    override fun visitIntersect(
        rel: RelIntersect,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "INTERSECT is not yet supported",
            ),
        )
    }

    override fun visitIterate(
        rel: RelIterate,
        ctx: Unit,
    ): RelContext {
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
    ): RelContext {
        val lhs = visitRel(rel.left, ctx)
        val lhsFrom = assertNotNull(lhs.from)
        assert(lhsFrom.tableRefs.size == 1)
        val rhs = visitRel(rel.right, ctx)
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
        return lhs
    }

    override fun visitLimit(
        rel: RelLimit,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "Limit is not yet supported",
            ),
        )
    }

    override fun visitOffset(
        rel: RelOffset,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "Offset is not yet supported",
            ),
        )
    }

    override fun visitProject(
        rel: RelProject,
        ctx: Unit,
    ): RelContext {
        val input = rel.input
        val projections = rel.projections
        val sfw = visitRel(rel.input, ctx)

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
        sfw.select =
            selectValue(
                constructor = rexConverter.apply(projections.first()),
                setq = null,
            )
        return sfw
    }

    override fun visitScan(
        rel: RelScan,
        ctx: Unit,
    ): RelContext {
        val fromRex = rel.rex

        // Init a new SFW context
        val sfw = RelContext()
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
        return sfw
    }

    override fun visitSort(
        rel: RelSort,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SORT is not yet supported",
            ),
        )
    }

    override fun visitUnion(
        rel: RelUnion,
        ctx: Unit,
    ): RelContext {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "UNION is not yet supported",
            ),
        )
    }

    override fun visitUnpivot(
        rel: RelUnpivot,
        ctx: Unit,
    ): RelContext {
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
