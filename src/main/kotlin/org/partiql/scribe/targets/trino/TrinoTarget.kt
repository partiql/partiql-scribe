package org.partiql.scribe.targets.trino

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathIndex
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
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
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
public open class TrinoTarget : SqlTarget() {

    override val target: String = "Trino"

    override val version: String = "3"

    companion object {

        @JvmStatic
        public val DEFAULT = TrinoTarget()
    }

    /**
     * Trino SQL dialect.
     */
    override val dialect = TrinoDialect()

    /**
     * Wire the Trino call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = TrinoCalls(onProblem)

    /**
     * Trino feature set allow list.
     */
    override val features: SqlFeatures = TrinoFeatures()

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
                    error("Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type.")
                }
            }
            return super.visitRelOpProject(node, ctx)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): PlanNode {
            when (val type = node.constructor.type) {
                is StructType -> {
                    val open = !(type.contentClosed && type.constraints.contains(TupleConstraint.Open(false)))
                    val unordered = !type.constraints.contains(TupleConstraint.Ordered)
                    if (open || unordered) {
                        error("SELECT VALUE of open, unordered structs is NOT supported.")
                    }
                }
                else -> error("SELECT VALUE is NOT supported.")
            }
            return super.visitRexOpSelect(node, ctx)
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Unit): PlanNode {
            if (node.root.op !is Rex.Op.Var) {
                error("Trino does not support path expressions on non-variable values")
            }
            if (node.key.op !is Rex.Op.Lit) {
                error("Trino does not support path non-literal path expressions, found ${node.key.op}")
            }
            if (node.key.type !is StringType) {
                error("Trino path expression must be a string literal.")
            }
            return super.visitRexOpPathKey(node, ctx)
        }

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: Unit): PlanNode {
            if (node.root.op !is Rex.Op.Var) {
                error("Trino does not support path expressions on non-variable values")
            }
            return super.visitRexOpPathSymbol(node, ctx)
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
        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Unit): PlanNode {

            // Assert root type
            val type = node.root.type
            if (type !is ListType || type.asNonNullable() !is ListType) {
                error("Trino only supports indexing on `array` type data; found $type")
            }

            // Assert key type
            val op = node.key.op
            if (op !is Rex.Op.Lit) {
                error("Trino array indexing only supports integer literals, e.g. x[1].")
                return super.visitRexOpPathIndex(node, ctx)
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
                return super.visitRexOpPathIndex(node, ctx)
            }
            // rewrite to be 1-indexed
            val index = i + 1
            val key = rex(StaticType.INT, rexOpLit(int32Value(index)))
            return rexOpPathIndex(node.root, key)
        }

        private fun error(message: String) {
            onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
        }
    }
}
