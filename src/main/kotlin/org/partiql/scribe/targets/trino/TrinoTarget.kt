package org.partiql.scribe.targets.trino

import org.partiql.plan.*
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.asNonNullable
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.ListType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value

/**
 * Experimental Trino SQL transpilation target.
 */
public object TrinoTarget : SqlTarget() {

    override val dialect = TrinoDialect

    override val target: String = "Trino"

    override val version: String = "3"

    /**
     * Wire the Trino call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = TrinoCalls(onProblem)

    /**
     * Trino feature set allow list.
     */
    override val features: SqlFeatures = TrinoFeatures

    /**
     * At this point, no plan rewriting.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) =
        TrinoRewriter(onProblem).visitPartiQLPlan(plan, Unit) as PartiQLPlan

    private class TrinoRewriter(val onProblem: ProblemCallback) : PlanRewriter<Unit>() {

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
         * At the moment
         */
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: Unit): PlanNode {
            // expression will be typed as SUPER
            return super.visitRexOpPath(node, ctx)
        }

        /**
         * From Trino docs,
         *
         * "The [] operator is used to access an element of an array and is indexed starting from one".
         *
         * @param node
         * @param ctx
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: Unit): PlanNode {
            val op = node.key.op
            val type = node.key.type
            if (type !is ListType || type.asNonNullable() !is ListType) {
                error("Trino only supports indexing on `array` type data; found $type")
            }
            if (op !is Rex.Op.Lit) {
                error("Trino array indexing only supports integer literals, e.g. x[1].")
                return super.visitRexOpPathStepIndex(node, ctx)
            }
            val i = when (val v = op.value) {
                is Int8Value -> v.int
                is Int16Value -> v.int
                is Int32Value -> v.int
                is Int64Value -> v.int
                is IntValue -> v.int
                else -> null
            }
            //
            if (i == null) {
                error("Trino array index must be a non-null integer, e.g. x[1].")
                return super.visitRexOpPathStepIndex(node, ctx)
            }
            // rewrite to be 1-indexed
            val index = i + 1
            val key = rex(StaticType.INT, rexOpLit(int32Value(index)))
            return rexOpPathStepIndex(key)
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
