package org.partiql.scribe.targets.trino

import org.partiql.plan.Fn
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.rex
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathIndex
import org.partiql.plan.rexOpPathSymbol
import org.partiql.plan.rexOpSelect
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpVar
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.asNonNullable
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.CollectionType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

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
        TrinoRewriter(onProblem).visitPartiQLPlan(plan, emptyList()) as PartiQLPlan

    private class TrinoRewriter(val onProblem: ProblemCallback) : PlanRewriter<List<Rel.Binding>>() {

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: List<Rel.Binding>): PlanNode {
            // Make sure that the output type is homogeneous
            node.projections.forEachIndexed { index, projection ->
                val type = projection.type.asNonNullable().flatten()
                if (type !is SingleType) {
                    error("Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type.")
                }
            }
            return super.visitRelOpProject(node, ctx)
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

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: List<Rel.Binding>): PlanNode {
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
        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: List<Rel.Binding>): PlanNode {

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

        // TODO ALAN figure out better way to pass along schema w/ the excluded attributes
        override fun visitRel(node: Rel, ctx: List<Rel.Binding>): PlanNode {
            return super.visitRel(node, node.type.schema)   // Pass along parent's node's schema
        }

        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: List<Rel.Binding>): PlanNode {
            // Replace `EXCLUDE` with a subquery
            val origInput = node.input
            val bindings = ctx

            val mappedBindings = bindings.map { binding ->
                val type = binding.type

                val constructor = when (type) {
                    is StructType -> { // binding's type should be a StructType
                        type.toRexStruct(
                            Rex(
                                type = type,
                                op = rexOpVar(
                                    0
                                )
                            )
                        )
                    }
                    else -> TODO()
                }
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
                                        Rex(
                                            type = type,
                                            op = constructor
                                        )
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

        @OptIn(PartiQLValueExperimental::class)
        private val cast_row_fn_sig = FunctionSignature.Scalar(
            name = "cast_row",
            returns = PartiQLValueType.ANY,
            parameters = listOf(
                FunctionParameter("value", PartiQLValueType.ANY)
            ),
            isNullable = false,
            isNullCall = true
        )



        // Due to subquery coercion we need to make the `ROW` construction explicit and not rely on subquery coercion
        @OptIn(PartiQLValueExperimental::class)
        private fun StructType.toRexCastRow(prefixPath: Rex): Rex.Op.Call.Static {
            assert(this.fields.size  == 1)
            val field = this.fields.first()
            val newPath = rexOpPathSymbol(
                prefixPath,
                field.key
            )
            // TODO ALAN convert to be Trino type
            val newK = Rex(
                type = StaticType.STRING,
                op = rexOpLit(stringValue(this.toTrinoString()))
            )
            val newV = field.value.toRex(
                prefixPath = Rex(
                    type = field.value,
                    op = newPath
                )
            )
            return rexOpCallStatic(
                fn = Fn(cast_row_fn_sig),
                args = listOf(
                    newV,
                    newK
                )
            )
        }

        private fun StaticType.toRex(prefixPath: Rex): Rex {
            return when (this) {
                is StructType -> {
                    Rex(
                        type = this,
                        op = when (this.fields.size) {
                            1 -> this.toRexCastRow(prefixPath)
                            else -> this.toRexStruct(prefixPath)
                        },
                    )
                }
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

        private fun StaticType.toTrinoString(): String {
            return when (this) {
                is IntType -> "INTEGER"
                is StringType -> "VARCHAR"
                is StructType -> {
                    val head = "ROW("
                    val fieldsAsString = this.fields.foldIndexed("") { index, acc, field ->
                        val fieldStr = acc + field.key + " " + field.value.toTrinoString()
                        if (index < fields.size - 1) {
                            "$fieldStr, "
                        } else {
                            fieldStr
                        }
                    }
                    head + fieldsAsString + ")"
                }
                else -> TODO("other trino string conversions")
            }
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
    }
}
