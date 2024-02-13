package org.partiql.scribe.sql

import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.builder.ExprSfwBuilder
import org.partiql.ast.exclude
import org.partiql.ast.excludeItem
import org.partiql.ast.excludeStepCollIndex
import org.partiql.ast.excludeStepCollWildcard
import org.partiql.ast.excludeStepStructField
import org.partiql.ast.excludeStepStructWildcard
import org.partiql.ast.exprAgg
import org.partiql.ast.fromJoin
import org.partiql.ast.fromValue
import org.partiql.ast.groupBy
import org.partiql.ast.groupByKey
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProject
import org.partiql.ast.selectProjectItemExpression
import org.partiql.plan.Agg
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.types.StaticType

/**
 * This class transforms a relational expression tree to a PartiQL [Expr.SFW].
 *
 * !!! IMPORTANT !!!
 *
 * TODO This is naive and simple.
 * TODO This only targets the basic SFW; so we assume the Plan of the form: SCAN -> FILTER -> PROJECT.
 * TODO This will require non-trivial rework to handle arbitrary plans.
 * TODO See Calcite's RelToSql and SqlImplementor
 * TODO https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/rel/rel2sql/RelToSqlConverter.java
 *
 * !!! IMPORTANT !!!
 */
open class RelToSql(
    private val transform: SqlTransform,
) : PlanBaseVisitor<ExprSfwBuilder, Rel.Type?>() {

    /**
     * This MUST return a mutable SFW builder because we'll need to inspect and/or replace the SELECT.
     */
    public fun apply(rel: Rel): ExprSfwBuilder {
        // assertClauses(rel)
        return visitRel(rel, null)
    }

    /**
     * TODO TEMPORARY — REMOVE ME, this could be made generic but honestly this is fine.
     */
    private fun assertClauses(rel: Rel) {
        val op1 = rel.op
        if (op1 !is Rel.Op.Project) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Project but found $op1")
        }
        val op2 = op1.input.op
        if (op2 is Rel.Op.Scan) {
            return // done
        }
        if (op2 is Rel.Op.Join) {
            return
        }
        if (op2 !is Rel.Op.Filter) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Filter but found $op2")
        }
        val op3 = op2.input.op
        if (op3 !is Rel.Op.Scan) {
            error("Invalid SELECT-FROM-WHERE, expected Rel.Op.Scan but found $op3")
        }
    }

    /**
     * Default behavior is considered unsupported.
     */
    override fun defaultReturn(node: PlanNode, ctx: Rel.Type?) =
        throw UnsupportedOperationException("Cannot translate rel $node")

    override fun defaultVisit(node: PlanNode, ctx: Rel.Type?) = defaultReturn(node, ctx)

    /**
     * Pass along the Rel.Type?.
     */
    override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type)

    /**
     * Logical Scan -> FROM Clause
     */
    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = ExprSfwBuilder()
        // validate scan type
        val type = ctx!!
        assert(type.schema.size == 1) { "Invalid SCAN schema, expected a single binding but found ${ctx.dump()}" }
        val rexToSql = RexToSql(transform, Locals(type.schema))
        // unpack to FROM clause
        sfw.from = fromValue(
            expr = rexToSql.apply(node.rex), // FROM <rex>,
            type = From.Value.Type.SCAN,
            asAlias = binder(type.schema[0].name),
            atAlias = null,
            byAlias = null,
        )
        return sfw
    }

    /**
     * Filter -> WHERE or HAVING Clause
     *
     * TODO this assumes WHERE!
     */
    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = visitRel(node.input, ctx)
        // validate filter type
        val type = ctx!!
        // translate to AST
        val rexToSql = RexToSql(transform, Locals(type.schema))
        sfw.where = rexToSql.apply(node.predicate)
        return sfw
    }

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = visitRel(node.input, ctx)
        // validate exclude type
        val type = ctx!!
        // translate to AST
        val rexToSql = RexToSql(transform, Locals(type.schema))
        sfw.exclude = exclude(
            node.items.map { item ->
                excludeItem(
                    root = rexToSql.visitRexOpVar(item.root, StaticType.ANY) as Expr.Var,
                    steps = item.steps.map { step ->
                        when (step) {
                            is Rel.Op.Exclude.Step.CollWildcard -> excludeStepCollWildcard()
                            is Rel.Op.Exclude.Step.StructField -> {
                                val case = when (step.symbol.caseSensitivity) {
                                    org.partiql.plan.Identifier.CaseSensitivity.SENSITIVE -> Identifier.CaseSensitivity.SENSITIVE
                                    org.partiql.plan.Identifier.CaseSensitivity.INSENSITIVE -> Identifier.CaseSensitivity.INSENSITIVE
                                }
                                excludeStepStructField(
                                    Identifier.Symbol(
                                        symbol = step.symbol.symbol,
                                        caseSensitivity = case
                                    )
                                )
                            }
                            is Rel.Op.Exclude.Step.CollIndex -> excludeStepCollIndex(step.index)
                            is Rel.Op.Exclude.Step.StructWildcard -> excludeStepStructWildcard()
                        }
                    }
                )
            }
        )
        return sfw
    }

    /**
     * Project -> SELECT Clause
     */
    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = visitRel(node.input, null)

        // we had projections from the aggregation


        // we want to replace all existing vars with what's in sfw.select
        // HACK!!!
        val projections = sfw.select?.let { select ->
            (select as Select.Project).items.map {
                when (it) {
                    is Select.Project.Item.All -> TODO()
                    is Select.Project.Item.Expression -> it.expr
                }
            }
        } ?: emptyList()
        val locals = Locals(
            env = node.input.type.schema,
            projections = projections,
        )

        val rexToSql = RexToSql(transform, locals)
        val type = ctx!!
        assert(type.schema.size == node.projections.size) { "Malformed plan, relation output type does not match projections" }

        sfw.select = selectProject(
            items = node.projections.mapIndexed { i, rex ->
                selectProjectItemExpression(
                    expr = rexToSql.apply(rex),
                    asAlias = identifierSymbol(type.schema[i].name, Identifier.CaseSensitivity.SENSITIVE),
                )
            },
            setq = null,
        )
        return sfw
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type?): ExprSfwBuilder {
        val lhs = visitRel(node.lhs, null)
        val rhs = visitRel(node.rhs, null)
        val schema = Locals(node.lhs.type.schema + node.rhs.type.schema)
        val condition = RexToSql(transform, schema).apply(node.rex)
        val type = when (node.type) {
            Rel.Op.Join.Type.INNER -> From.Join.Type.INNER
            Rel.Op.Join.Type.LEFT -> From.Join.Type.LEFT_OUTER
            Rel.Op.Join.Type.RIGHT -> From.Join.Type.RIGHT_OUTER
            Rel.Op.Join.Type.FULL -> From.Join.Type.FULL_OUTER
        }
        lhs.from = fromJoin(
            lhs = lhs.from!!,
            rhs = rhs.from!!,
            type = type,
            condition = condition
        )
        return lhs
    }

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): ExprSfwBuilder {
        val sfw = visitRel(node.input, null)
        val rexToSql = RexToSql(transform, Locals(node.input.type.schema))
        if (node.groups.isNotEmpty()) {
            sfw.groupBy = groupBy(
                strategy = GroupBy.Strategy.FULL,
                keys = node.groups.mapIndexed { i, rex ->
                    groupByKey(
                        expr = rexToSql.apply(rex),
                        asAlias = null,
                    )
                },
                asAlias = null,
            )
        }
        // Aggregations hack!!
        sfw.select = selectProject(
            setq = null,
            items = node.calls.map {
                selectProjectItemExpression(
                    expr = visitAgg(rexToSql, it),
                    asAlias = null,
                )
            }
        )
        return sfw
    }

    private fun visitAgg(transform: RexToSql, agg: Rel.Op.Aggregate.Call): Expr.Agg {
        val args = agg.args.map { transform.apply(it) }
        val call = agg.agg
        return exprAgg(
            function = aggCallName(call),
            args = args,
            setq = null,
        )
    }

    private fun aggCallName(call: Agg): Identifier {
        return when (call.signature.name) {
            "count_star" -> id("COUNT_STAR")
            else -> id(call.signature.name)
        }
    }

    private fun id(symbol: String): Identifier.Symbol = identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
    )

    private fun binder(symbol: String): Identifier.Symbol = identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
    )

    private fun Rel.Type.dump(): String {
        if (this.schema.isEmpty()) return "< empty >"
        val pairs = this.schema.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }
}
