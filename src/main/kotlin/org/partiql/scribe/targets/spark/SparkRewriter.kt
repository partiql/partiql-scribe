package org.partiql.scribe.targets.spark

import org.partiql.plan.Fn
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.rel
import org.partiql.plan.relBinding
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.relType
import org.partiql.plan.rex
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
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
import org.partiql.scribe.expandStruct
import org.partiql.types.CollectionType
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

/**
 * The [SparkRewriter] holds the base logic for a PartiQL plan for translation to Spark SQL.
 */
public open class SparkRewriter(val onProblem: ProblemCallback) : PlanRewriter<Rel.Type?>() {

    private val EXCLUDE_ALIAS = "\$__EXCLUDE_ALIAS__"

    /**
     * Expand every wildcard including:
     * - relation wildcards (e.g. SELECT tbl.*) and
     * - struct wildcards (e.g. `SELECT tbl.foo.*`).
     */
    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Rel.Type?): PlanNode {
        val newTupleUnion = super.visitRexOpTupleUnion(node, ctx) as Rex.Op.TupleUnion
        val newArgs = mutableListOf<Rex>()
        newTupleUnion.args.forEach { arg ->
            val op = arg.op
            val type = arg.type
            // For now, just support the expansion of variable references and paths
            if (type is StructType && (op is Rex.Op.Var || op is Rex.Op.Path)) {
                newArgs.addAll(expandStruct(op, type))
            } else {
                newArgs.add(arg)
            }
        }
        return rexOpTupleUnion(newArgs)
    }

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Rel.Type?): PlanNode {
        if ((node.statement !is Statement.Query) || (node.statement as Statement.Query).root.op !is Rex.Op.Select) {
            error("Spark does not support top level expression")
        }
        return super.visitPartiQLPlan(node, ctx)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Rel.Type?): PlanNode {
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

    override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Rel.Type?): PlanNode {
        if (node.key.op !is Rex.Op.Lit) {
            error("Spark does not support path non-literal path expressions, found ${node.key.op}")
        }
        if (node.key.type !is StringType) {
            error("Spark path expression must be a string literal.")
        }
        return super.visitRexOpPathKey(node, ctx)
    }

    override fun visitRel(node: Rel, ctx: Rel.Type?): PlanNode {
        return super.visitRel(node, node.type)
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): PlanNode {
        val inputOp = node.input.op
        val newNode = if (inputOp is Rel.Op.Exclude) {
            // Check for special case involving just top-level column exclusions (i.e. # of steps is 1)
            if (inputOp.items.all { it.steps.size == 1 }) {
                node.copy(
                    input = inputOp.input   // get rid of `EXCLUDE` in plan; pass in `EXCLUDE`'s input Rel
                )
            } else {
                // `EXCLUDE` node with non-top-level column exclusions; run existing `EXCLUDE` logic
                node
            }
        } else {
            node
        }
        val project = super.visitRelOpProject(newNode, ctx) as Rel.Op.Project
        return if (newNode.input.op is Rel.Op.Exclude) {
            // When the Rel.Op.Project's input Rel.Op is Exclude and has deeper nested `EXCLUDE` paths
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
            val varToPath = VarToPathRewriter(newVar = 0, inputType)
            relOpProject(
                input = rel(
                    type = newInputType,
                    op = inputOp
                ),
                projections = project.projections.map { projection ->
                    varToPath.visitRex(projection, StaticType.ANY) as Rex
                }
            )
        } else {
            project
        }
    }

    /**
     * TODO consider changing to a simplified rewrite that we currently do for nested `EXCLUDE` queries
     *  https://github.com/partiql/partiql-scribe/issues/60.
     * `EXCLUDE` comes right before `PROJECT`, so here we recreate a [Rel.Op] with all exclude paths applied.
     * We have full schema and since PlanTyper was run before this point, we know the schema after applying all
     * exclude paths. From this schema with the excluded struct and collection fields, we recreate the input
     * environment used by the final [Rel.Op.Project] through a combination of struct and collection constructors.
     *
     * For example, given input schema tbl = { flds: Struct(a: int, b: boolean, c: string) } and query
     * SELECT t.*
     * EXCLUDE t.flds.b
     * FROM tbl AS t
     *
     * After the PlanTyper pass, we know the schema of `t` before the projection will be Struct(a: int, c: string)
     * This rewrite will then output:
     * SELECT $__EXCLUDE_ALIAS__.t.*    -- rewrite occurs in [Rel.Op.Project] after visiting [Rel.Op.Exclude]
     * FROM (
     *     SELECT { 't': { 'a': t.a, 'c': t.c } }
     *     FROM tbl AS t
     * ) AS $__EXCLUDE_ALIAS__
     *
     * The target dialect will then perform its own text rewrite depending on how struct and collections are
     * represented.
     */
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
                rexOpStructField(
                    k = Rex(
                        type = StaticType.STRING,
                        op = rexOpLit(
                            value = stringValue(binding.name)
                        )
                    ),
                    v = type.toRex(
                        prefixPath = Rex(
                            type = type,
                            op = rexOpVar(
                                index
                            )
                        )
                    )
                )
            }
        )
        val structWrappingBindings = StructType(
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
                type = structWrappingBindings,
                op = rexOpSelect(
                    constructor = Rex(
                        type = structWrappingBindings,
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
        return when (val nonNullType = this.asNonNullable()) {
            is StructType -> Rex(
                type = nonNullType,
                op = nonNullType.toRexStruct(prefixPath),
            )
            is CollectionType -> Rex(
                type = nonNullType,
                op = nonNullType.toRexCallTransform(prefixPath),
            )
            else -> prefixPath
        }
    }

    // https://spark.apache.org/docs/latest/api/sql/index.html#transform
    @OptIn(PartiQLValueExperimental::class)
    private val transform_fn_sig = FunctionSignature.Scalar(
        name = "transform",
        returns = PartiQLValueType.ANY,
        parameters = listOf(
            FunctionParameter("array_expr", PartiQLValueType.ANY),
            FunctionParameter("element_var", PartiQLValueType.ANY),
            FunctionParameter("element_expr", PartiQLValueType.ANY)
        ),
        isNullable = false,
        isNullCall = true
    )

    @OptIn(PartiQLValueExperimental::class)
    private fun CollectionType.toRexCallTransform(prefixPath: Rex): Rex.Op.Call.Static {
        val elementType = this.elementType
        val elementVar = Rex(
            type = StaticType.ANY,
            op = rexOpLit(
                value = symbolValue("___coll_wildcard___")
            )
        )
        return rexOpCallStatic(
            fn = Fn(transform_fn_sig),
            args = listOf(
                prefixPath,
                elementVar,
                elementType.toRex(
                    elementVar
                )
            )
        )
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun StructType.toRexStruct(prefixPath: Rex): Rex.Op.Struct {
        val fieldsAsRexOpStructField: List<Rex.Op.Struct.Field> = this.fields.map { field ->
            val newPath = rexOpPathKey(
                prefixPath,
                rex(StaticType.STRING, rexOpLit(stringValue(field.key)))
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
