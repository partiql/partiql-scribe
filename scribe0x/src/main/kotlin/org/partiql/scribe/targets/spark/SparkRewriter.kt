package org.partiql.scribe.targets.spark

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.relOpProject
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpTupleUnion
import org.partiql.plan.util.PlanRewriter
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.RexOpVarTypeRewriter
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.asNonAbsent
import org.partiql.scribe.excludeBindings
import org.partiql.types.SingleType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

/**
 * The [SparkRewriter] holds the base logic for a PartiQL plan for translation to Spark SQL.
 */
public open class SparkRewriter(val onProblem: ProblemCallback) : PlanRewriter<Rel.Type?>() {
    /**
     * For Spark, expand every wildcard, including relation wildcards (e.g. `SELECT tbl.*`) and
     * struct wildcards (e.g. `SELECT tbl.foo.*`).
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
                newArgs.addAll(expandStructSpark(op, type))
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
                type.toRexSpark(
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

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Rel.Type?): PlanNode {
        if ((node.statement !is Statement.Query) || (node.statement as Statement.Query).root.op !is Rex.Op.Select) {
            error("Spark does not support top level expression")
        }
        return super.visitPartiQLPlan(node, ctx)
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

    private fun error(message: String) {
        onProblem(ScribeProblem(ScribeProblem.Level.ERROR, message))
    }
}
