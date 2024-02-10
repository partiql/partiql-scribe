package org.partiql.scribe.targets.redshift

import org.partiql.ast.sql.SqlDialect
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.rel
import org.partiql.plan.relBinding
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.relType
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathSymbol
import org.partiql.plan.rexOpSelect
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpTupleUnion
import org.partiql.plan.rexOpVar
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.VarToPathRewriter
import org.partiql.scribe.asNonNullable
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.types.CollectionType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * Experimental Redshift SQL transpilation target.
 */
public object RedshiftTarget : SqlTarget() {

    override val target: String = "Redshift"

    override val version: String = "0"

    override val dialect: SqlDialect = RedshiftDialect

    /**
     * Wire the Redshift call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = RedshiftCalls(onProblem)

    /**
     * Redshift feature set allow list.
     */
    override val features: SqlFeatures = RedshiftFeatures

    /**
     * Rewrite a PartiQLPlan in terms of Redshift features.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) =
        RedshiftRewriter(onProblem).visitPartiQLPlan(plan, null) as PartiQLPlan

    private class RedshiftRewriter(val onProblem: ProblemCallback) : PlanRewriter<Rel.Type?>() {
        private val EXCLUDE_ALIAS = "\$__EXCLUDE_ALIAS__"

        // TODO ALAN more docs + cleanup
        // Redshift doesn't support struct wildcards. Convert any struct wildcards to an equivalent expanded form.
        // Keep any relation references the same as before
        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Rel.Type?): PlanNode {
            val tupleUnion = super.visitRexOpTupleUnion(node, ctx) as Rex.Op.TupleUnion
            val newArgs = tupleUnion.args.fold(emptyList<Rex>()) { agg, rex ->
                when (rex.op) {
                    is Rex.Op.Path -> {
                        val prefixPath = rex.op
                        when (val st = rex.type) {
                            is StructType -> {
                                val expandedFields = st.fields.map { field ->
                                    Rex(
                                        type = StructType(
                                            fields = listOf(
                                                StructType.Field(
                                                    key = field.key,
                                                    value = field.value
                                                )
                                            )
                                        ),
                                        op = rexOpStruct(
                                            fields = listOf(
                                                rexOpStructField(
                                                    k = Rex(
                                                        type = StaticType.STRING,
                                                        op = rexOpLit(
                                                            stringValue(
                                                                field.key
                                                            )
                                                        )
                                                    ),
                                                    v = Rex(
                                                        type = field.value,
                                                        op = rexOpPathSymbol(
                                                            root = Rex(
                                                                type = field.value,
                                                                op = prefixPath
                                                            ),
                                                            key = field.key
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                }
                                agg + expandedFields
                            }
                            else -> agg + rex
                        }
                    }
                    else -> agg + rex
                }
            }
            return rexOpTupleUnion(newArgs)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): PlanNode {
            // Make sure that the output type is homogeneous
            node.projections.forEachIndexed { index, projection ->
                val type = projection.type.asNonNullable().flatten()
                if (type !is SingleType) {
                    error("Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type.")
                }
            }
            val project = super.visitRelOpProject(node, ctx) as Rel.Op.Project
            return if (node.input.op is Rel.Op.Exclude) {
                // When the Rel.Op.Project's input Rel.Op is Exclude
                // 1. Rewrite the Rel's type schema to wrap the existing bindings in a new binding, `$__EXCLUDE_ALIAS__`
                //    For example if schema was originally
                //          [ <binding_name_1: type_1>, <binding_name_2: type_2> ]
                //    Rewrite to:
                //          [ <$__EXCLUDE_ALIAS__: Struct(Field(binding_name_1, type_1), Field(binding_name_2, type_2))> ]
                // 2. Rewrite the projections' Rex.Op.Var to Rex.Op.Path with additional root of `$__EXCLUDE_ALIAS`
                //    For example (for sake of demonstration subbing var refs with their symbol):
                //          binding_name_1 + binding_name2
                //    Rewrite to:
                //          $__EXCLUDE_ALIAS.binding_name_1 + $__EXCLUDE_ALIAS.binding_name_2
                val input = project.input
                val inputOp = input.op as Rel.Op.Scan
                val inputType = input.type
                val newInputType = relType(
                    schema = listOf(
                        relBinding(
                            name = EXCLUDE_ALIAS,
                            type = StructType(
                                fields = inputType.schema.map { binding ->
                                    StructType.Field(binding.name, binding.type)
                                }
                            )
                        )
                    ),
                    props = inputType.props
                )
                val varToNameMap = inputType.schema.mapIndexed { index, binding -> index to binding.name }.toMap()
                val varToPath = VarToPathRewriter(newVar = 0, oldVarToName = varToNameMap)
                val projectWithUpdatedVars = relOpProject(
                    input = rel(
                        type = newInputType,
                        op = inputOp
                    ),
                    projections = project.projections.map { projection ->
                        varToPath.visitRex(projection, StaticType.ANY) as Rex
                    }
                )
                // Visit again to rewrite any struct wildcards
                visitRelOpProject(projectWithUpdatedVars, ctx)
            } else {
                project
            }
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
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: Rel.Type?): PlanNode {
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
        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Rel.Type?): PlanNode {
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
            return super.visitRexOpPathIndex(node, ctx)
        }

        override fun visitRel(node: Rel, ctx: Rel.Type?): PlanNode {
            return super.visitRel(node, node.type)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): PlanNode {
            // Replace `EXCLUDE` with a subquery
            assert(ctx != null)
            val origInput = node.input
            val bindings = ctx!!.schema
            val structType = StructType(
                fields = bindings.map { binding ->
                    StructType.Field(
                        key = binding.name,
                        value = binding.type
                    )
                }
            )
            val rexOpStruct = rexOpStruct(
                bindings.mapIndexed { index, binding ->
                    val type = binding.type
                    val newStructType =
                        rexOpStructField(
                            k = Rex(
                                type = StaticType.STRING,
                                op = rexOpLit(
                                    value = stringValue(binding.name)
                                )
                            ),
                            v = type.toRex(
                                prefixPath = Rex(
                                    type = binding.type,
                                    op = rexOpVar(
                                        index
                                    )
                                )
                            )
                        )
                    newStructType
                }
            )
            val tWrappingStructType = StructType(
                fields = listOf(
                    StructType.Field(
                        EXCLUDE_ALIAS,
                        structType
                    )
                ),
                constraints = setOf(
                    TupleConstraint.Ordered,
                    TupleConstraint.Open(false)
                )
            )
            return relOpScan(
                rex = Rex(
                    type = tWrappingStructType,
                    op = rexOpSelect(
                        constructor = Rex(
                            type = tWrappingStructType,
                            op = rexOpVar(
                                ref = 0
                            )
                        ),
                        rel = Rel(
                            type = Rel.Type(
                                schema = listOf(
                                    relBinding(
                                        name = EXCLUDE_ALIAS,
                                        type = structType
                                    )
                                ),
                                props = emptySet()
                            ),
                            op = relOpProject(
                                projections = listOf(
                                    Rex(
                                        type = structType,
                                        op = rexOpStruct
                                    )
                                ),
                                input = origInput
                            )
                        )
                    )
                )
            )
        }

        private fun StaticType.toRex(prefixPath: Rex): Rex {
            return when (this) {
                is StructType -> Rex(
                    type = this,
                    op = this.toRexStruct(prefixPath),
                )
                is CollectionType -> prefixPath // currently just return back the prefixPath for CollectionType in Redshift
                else -> prefixPath
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

        private fun error(message: String) {
            onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
        }
    }
}
