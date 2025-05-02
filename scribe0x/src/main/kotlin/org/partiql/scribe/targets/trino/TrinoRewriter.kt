package org.partiql.scribe.targets.trino

import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.relOpProject
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathIndex
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpTupleUnion
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.RexOpVarTypeRewriter
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.asNonAbsent
import org.partiql.scribe.excludeBindings
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
 * The [TrinoRewriter] holds the base logic for a PartiQL plan for translation to Trino SQL.
 */
public open class TrinoRewriter(val onProblem: ProblemCallback) : PlanRewriter<Rel.Type?>() {
    /**
     * For Trino, expand every wildcard, including relation wildcards (e.g. `SELECT tbl.*`) and
     * struct/`ROW` wildcards (e.g. `SELECT tbl.foo.*`).
     *
     * For example, consider if tbl's schema is the following: tbl = { flds: Struct(a: int, b: boolean, c: string) }
     * and a query with a relation wildcard
     *      SELECT *
     *      FROM tbl
     * This query would be rewritten to something like
     *      SELECT VALUE TUPLEUNION(varRef(0))
     *      FROM tbl -- varRef(0)
     * Rewrite the output query to something like:
     *      SELECT tbl.flds
     *      FROM tbl
     *
     * When there's a struct wildcard, we have
     *      SELECT tbl.flds.*
     *      FROM tbl
     * Which gets rewritten to:
     *      SELECT VALUE TUPLEUNION(varRef(0).flds)
     *      FROM tbl -- varRef(0)
     * We expand the struct wildcard to something like the following:
     *      SELECT VALUE TUPLEUNION({ 'a': varRef(0).flds.a }, { 'b': varRef(0).flds.b }, { 'c': varRef(0).flds.c })
     *      FROM tbl -- varRef(0)
     * With a final output of:
     *      SELECT tbl.flds.a, tbl.flds.b, tbl.flds.c
     *      FROM tbl
     */
    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Rel.Type?): PlanNode {
        val newTupleUnion = super.visitRexOpTupleUnion(node, ctx) as Rex.Op.TupleUnion
        val newArgs = mutableListOf<Rex>()
        newTupleUnion.args.forEach { arg ->
            val op = arg.op
            val type = arg.type.asNonAbsent()
            // For now, just support the expansion of variable references and paths
            if (type is StructType && (op is Rex.Op.Var || op is Rex.Op.Path)) {
                newArgs.addAll(expandStructTrino(op, type))
            } else {
                newArgs.add(arg)
            }
        }
        return rexOpTupleUnion(newArgs)
    }

    // Ensures we rewrite any [VarRef]s and [Path]s when the `SELECT` does not have a TUPLEUNION.
    // E.g. `SELECT t.a.b FROM t`
    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Rel.Type?): PlanNode {
        val struct = super.visitRexOpStruct(node, ctx) as Rex.Op.Struct
        val newStruct = struct.fields.map { field ->
            val op = field.v.op
            val type = field.v.type.asNonAbsent()
            val newRex = if (op is Rex.Op.Var || op is Rex.Op.Path) {
                type.toRexTrino(
                    prefixPath = field.v
                )
            } else {
                field.v
            }
            rexOpStructField(
                k = field.k,
                v = newRex
            )
        }
        return rexOpStruct(newStruct)
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): PlanNode {
        // Make sure that the output type is homogeneous
        node.projections.forEachIndexed { index, projection ->
            val type = projection.type.asNonAbsent().flatten()
            if (type !is SingleType) {
                error("Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type.")
            }
        }
        val inputOp = node.input.op
        val newNode = if (inputOp is Rel.Op.Exclude) {
            val inputToExclude = inputOp.input
            val excludePaths = inputOp.items
            // apply exclusions to the EXCLUDE's input schema
            val initBindings = inputToExclude.type.schema
            val newSchema = excludePaths.fold((initBindings)) { bindings, item -> excludeBindings(bindings, item) }
            val newInput = inputToExclude.copy(
                type = inputToExclude.type.copy(
                    schema = newSchema
                )
            )
            val newProjections = node.projections.map { projection ->
                // Propagate the modified bindings to each of the projection items' [Rex.Op.Var]s
                RexOpVarTypeRewriter(newSchema).visitRex(projection, Unit) as Rex
            }
            relOpProject(
                input = newInput,
                projections = newProjections
            )
        } else {
            node
        }
        return super.visitRelOpProject(newNode, ctx) as Rel.Op.Project
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Rel.Type?): PlanNode {
        when (val type = node.constructor.type.asNonAbsent()) {
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
        if (node.root.type.asNonAbsent() !is StructType) {
            error("Trino path expression must be on a ROW type (PartiQL STRUCT), found ${node.root.type}")
        }
        if (node.key.op !is Rex.Op.Lit) {
            error("Trino does not support path non-literal path expressions, found ${node.key.op}")
        }
        if (node.key.type !is StringType) {
            error("Trino path expression must be a string literal.")
        }
        return super.visitRexOpPathKey(node, ctx)
    }

    override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: Rel.Type?): PlanNode {
        if (node.root.type.asNonAbsent() !is StructType) {
            error("Trino path expression must be on a ROW type (PartiQL STRUCT), found ${node.root.type}")
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
    override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Rel.Type?): PlanNode {

        // Assert root type
        val type = node.root.type
        if (type !is ListType || type.asNonAbsent() !is ListType) {
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

    override fun visitRel(node: Rel, ctx: Rel.Type?): PlanNode {
        return super.visitRel(node, node.type)
    }
}
