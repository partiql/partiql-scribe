package org.partiql.scribe.targets.spark

import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.Fn
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpCollection
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathSymbol
import org.partiql.plan.rexOpSelect
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpVar
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

object SparkTarget : SqlTarget() {
    override val target: String = "Spark"

    override val version: String = "3.1"

    override val dialect: SqlDialect = SparkDialect

    override val features: SqlFeatures = SparkFeatures

    override fun getCalls(onProblem: ProblemCallback): SqlCalls = SparkCalls(onProblem)

    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan =
        SparkRewriter(onProblem).visitPartiQLPlan(plan, emptyList()) as PartiQLPlan

    private class SparkRewriter(val onProblem: ProblemCallback) : PlanRewriter<List<Rel.Binding>>() {

        override fun visitPartiQLPlan(node: PartiQLPlan, ctx: List<Rel.Binding>): PlanNode {
            if ((node.statement !is Statement.Query) || (node.statement as Statement.Query).root.op !is Rex.Op.Select) {
                error("Spark does not support top level expression")
            }
            return super.visitPartiQLPlan(node, ctx)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: List<Rel.Binding>): PlanNode {
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

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: List<Rel.Binding>): PlanNode {
            if (node.key.op !is Rex.Op.Lit) {
                error("Spark does not support path non-literal path expressions, found ${node.key.op}")
            }
            if (node.key.type !is StringType) {
                error("Spark path expression must be a string literal.")
            }
            return super.visitRexOpPathKey(node, ctx)
        }

        override fun visitRel(node: Rel, ctx: List<Rel.Binding>): PlanNode {
            return super.visitRel(node, node.type.schema)
        }

        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: List<Rel.Binding>): PlanNode {
            // Replace `EXCLUDE` with a subquery
            val origInput = node.input
            val bindings = ctx

            val mappedBindings = bindings.map { binding ->
                val type = binding.type

                val constructor = type.toRex(
                    Rex(
                        type = type,
                        op = rexOpVar(
                            0
                        )
                    )
                )
                relOpScan(
                    rex = Rex(
                        type = type,
                        op = rexOpSelect(
                            constructor = Rex(
                                type = type,
                                op = rexOpVar(
                                    ref = 0
                                )
                            ),
                            rel = Rel(
                                type = Rel.Type(
                                    schema = listOf(binding),
                                    props = emptySet(),
                                ),
                                op = relOpProject(
                                    projections = listOf(
                                        constructor
                                    ),
                                    input = origInput
                                )
                            )
                        )
                    )
                )
            }
            return mappedBindings.first()   // TODO ALAN currently just return the first binding; need to support multiple bindings
        }

        private fun StaticType.toRex(prefixPath: Rex): Rex {
            return when (this) {
                is StructType -> Rex(
                    type = this,
                    op = this.toRexStruct(prefixPath),
                )
                is CollectionType -> Rex(
                    type = this,
                    op = this.toRexCallTransform(prefixPath),
                )
                else -> prefixPath
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private val transform_fn_sig = FunctionSignature.Scalar(
            name = "transform",
            returns = PartiQLValueType.ANY,
            parameters = listOf(
                FunctionParameter("prefix_path", PartiQLValueType.ANY),
                FunctionParameter("value", PartiQLValueType.ANY)
            ),
            isNullable = false,
            isNullCall = true
        )

        @OptIn(PartiQLValueExperimental::class)
        private fun CollectionType.toRexCallTransform(prefixPath: Rex): Rex.Op.Call.Static {
            val elementType = this.elementType
            val newPath = Rex(
                type = StaticType.ANY,
                op = rexOpLit(
                    value = symbolValue("coll_wildcard")    // TODO ALAN make unique?
                )
            )
            return rexOpCallStatic(
                fn = Fn(transform_fn_sig),
                args = listOf(
                    prefixPath,
                    elementType.toRex(
                        newPath
                    )
                )
            )
        }

        // Converts the Struct type to a Rex.Op.Struct
        @OptIn(PartiQLValueExperimental::class)
        private fun StructType.toRexStruct(prefixPath: Rex): Rex.Op.Struct {
            val fieldsAsRexOpStructField: List<Rex.Op.Struct.Field> = this.fields.map { field ->
                val newPath = rexOpPathSymbol(
                    prefixPath,
                    field.key
                )
                val newV = field.value.toRex(
                    prefixPath = Rex(
                        type = field.value,
                        op = newPath
                    )
                )
                rexOpStructField(
                    k = Rex(
                        type = StaticType.STRING,
                        op = rexOpLit(stringValue(field.key))
                    ),
                    v = newV
                )
            }
            return rexOpStruct(
                fields = fieldsAsRexOpStructField
            )
        }

        private fun error(message: String) {
            onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
        }
    }
}
