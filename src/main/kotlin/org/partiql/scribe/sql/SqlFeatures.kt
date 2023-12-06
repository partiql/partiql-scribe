package org.partiql.scribe.sql

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
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

    /**
     * This visits all of the [node]'s children without invoking the [defaultReturn] for the [node].
     */
    public open fun visitChildren(node: PlanNode, ctx: ProblemCallback) {
        node.children.forEach { it.accept(this, ctx) }
    }

    /**
     * An [SqlFeatures] which denies all features by default; thereby requiring explicit opt-in.
     */
    public open class Defensive : SqlFeatures() {

        override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) {
            val name = feature(node)
            ctx.error("PartiQL feature ($name) is not supported.")
        }
    }

    /**
     * An [SqlFeatures] which allows all features by default.
     */
    public open class Permissive : SqlFeatures() {

        override fun defaultReturn(node: PlanNode, ctx: ProblemCallback) {
            // allow everything
        }
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
