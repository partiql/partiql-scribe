package org.partiql.scribe.targets.redshift

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.asNonNullable
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.SingleType
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Experimental Redshift SQL transpilation target.
 */
public object RedshiftTarget : SqlTarget() {

    override val target: String = "Redshift"

    override val version: String = "0"

    /**
     * Wire the Redshift call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = RedshiftCalls(onProblem)

    /**
     * Redshift feature set allow list.
     */
    override val features: SqlFeatures = RedshiftFeatures

    /**
     * At this point, no plan rewriting.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) =
        RedshiftRewriter(onProblem).visitPartiQLPlan(plan, Unit) as PartiQLPlan

    private class RedshiftRewriter(val onProblem: ProblemCallback) : PlanRewriter<Unit>() {
        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): PlanNode {
            // Make sure that the output type is homogeneous
            node.projections.forEachIndexed { index, projection ->
                val type = projection.type.asNonNullable().flatten()
                if (type !is SingleType) {
                    onProblem(
                        ScribeProblem(
                            ScribeProblem.Level.ERROR,
                            "Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type."
                        )
                    )
                }
            }
            return super.visitRelOpProject(node, ctx)
        }

        /**
         * From Redshift docs,
         *
         * By default, navigation operations on SUPER values return null instead of returning an error out when the
         * navigation is invalid. Object navigation is invalid if the SUPER value is not an object or if the SUPER value
         * is an object but doesn't contain the attribute name used in the query.
         *
         * Array navigation returns null if the SUPER value is not an array or the array index is out of bounds.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: Unit): PlanNode {
            // expression will be typed as SUPER
            return super.visitRexOpPath(node, ctx)
        }

        /**
         * For now, let's only allow integer literals. The Redshift documentation doesn't make it clear whether
         * expressions are allowed. Also, expressions in index path steps aren't required at the moment.
         *
         * @param node
         * @param ctx
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: Unit): PlanNode {
            val op = node.key.op
            if (op !is Rex.Op.Lit) {
                error("Redshift path step must an integer literal, e.g. x[0].")
            } else {
                when (op.value) {
                    is Int8Value,
                    is Int16Value,
                    is Int32Value,
                    is Int64Value,
                    is IntValue,
                    -> {
                        // allow `[<int>]` path expressions without err
                    }
                    else -> error("Redshift path step must an integer literal, e.g. x[0].")
                }
            }
            return super.visitRexOpPathStepIndex(node, ctx)
        }

        override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: Unit): PlanNode {
            error("Redshift does not support unpivot path steps, e.g. `x.y.*`")
            return super.visitRexOpPathStepUnpivot(node, ctx)
        }

        override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: Unit): PlanNode {
            error("Redshift does not support wildcard path steps, e.g. `x.y[*]`")
            return super.visitRexOpPathStepWildcard(node, ctx)
        }

        private fun error(message: String) {
            onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
        }
    }
}
