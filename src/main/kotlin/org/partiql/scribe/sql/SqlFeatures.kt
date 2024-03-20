package org.partiql.scribe.sql

import org.partiql.plan.Agg
import org.partiql.plan.Catalog
import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error

/**
 * The [SqlFeatures], by default, acts as a feature opt-in enforcer. It invokes the [ctx.onProblem]
 * callback for **ALL** node visits using [defaultReturn]. This is specifically aimed at enforcing that all
 * implementers of [SqlTarget] opt in to each feature.
 *
 * If extending the [SqlFeatures.Defensive] and you'd like to support a specific type of node visit, please manually
 * visit its children without visiting the node itself. See the following example:
 * ```
 * override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Context) = allowedVisit(node, ctx)
 * ```
 *
 * If you'd like to allow **all** features, or if you'd like to modify the error thrown for an unsupported feature,
 * please modify the definition of [defaultReturn]. See the following example:
 * ```
 * // To allow all nodes
 * override fun defaultReturn(node: PlanNode, ctx: Context) = allowedVisit(node, ctx)
 *
 * // To modify the error message
 * override fun defaultReturn(node: PlanNode, ctx: Context) {
 *     onProblem.error("Semantic feature (${node.javaClass.simpleName}) is not supported.")
 * }
 * ```
 *
 * @see [validate]
 * @see [visitChildren]
 */
abstract class SqlFeatures : PlanBaseVisitor<Unit, ProblemCallback>() {

    /**
     * Entry-point to the [SqlFeatures]
     *
     * @param node
     * @param onProblem
     */
    public fun validate(node: PartiQLPlan, onProblem: ProblemCallback) {
        return visitPartiQLPlan(node, onProblem)
    }

    override fun visitAgg(node: Agg, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitCatalog(node: Catalog, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitCatalogSymbol(node: Catalog.Symbol, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitCatalogSymbolRef(node: Catalog.Symbol.Ref, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitFn(node: Fn, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitIdentifier(node: Identifier, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRel(node: Rel, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRelBinding(node: Rel.Binding, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRelType(node: Rel.Type, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRex(node: Rex, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitStatement(node: Statement, ctx: ProblemCallback)  = visitChildren(node, ctx)

    override fun visitStatementQuery(node: Statement.Query, ctx: ProblemCallback)  = visitChildren(node, ctx)

    /**
     * This visits all of the [node]'s children without invoking the [defaultReturn] for the [node].
     */
    public open fun visitChildren(node: PlanNode, ctx: ProblemCallback) {
        node.children.forEach { it.accept(this, ctx) }
    }

    /**
     * An [SqlFeatures] which denies all features (rel.op and rex.op) by default; thereby requiring explicit opt-in.
     */
    public open class Defensive : SqlFeatures() {

        open val allow: Set<Class<*>> = emptySet()

        override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) {
            if (!allow.contains(node::class.java)) {
                ctx.error("PartiQL feature (${feature(node)}) is not supported.")
            }
        }
    }

    /**
     * An [SqlFeatures] which allows all features by default.
     */
    public open class Permissive : SqlFeatures() {

        /**
         * Allow everything
         *
         * @param node
         * @param ctx
         */
        override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) = Unit
    }

    /**
     * Not an API, simply generate a human-readable feature name for error messaging.
     *
     * @param node
     * @return
     */
    internal fun feature(node: PlanNode): String {
        return node.javaClass.typeName.let {
            it.substring(it.lastIndexOf(".") + 1)
        }.replace('$', '.')
    }
}
