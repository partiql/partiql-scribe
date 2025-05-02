package org.partiql.scribe

import org.partiql.plan.PlanNode
import org.partiql.plan.Rel.Binding
import org.partiql.plan.Rex
import org.partiql.plan.util.PlanRewriter
import org.partiql.types.StructType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StringValue

/**
 * Rewrite [Rex]s with [Rex.Op] of [Rex.Op.Var]s to use the type specified from [bindings]. Any re-written
 * [Rex.Op.Var]s will also be propagated to other parent nodes (e.g. paths).
 *
 * This [PlanRewriter] is used as part of the nested `EXCLUDE` rewrite used by Redshift. Other targets may also use this
 * rewriter in the future https://github.com/partiql/partiql-scribe/issues/60. As part of that rewrite, certain
 * [bindings] can be marked with metas (e.g. "EXPAND" to indicate we should expand a projected struct) that we need to
 * propagate to the corresponding [Rex.Op.Var]s in the projection list.
 */
internal class RexOpVarTypeRewriter(
    private val bindings: List<Binding>,
): PlanRewriter<Unit>() {
    override fun defaultReturn(node: PlanNode, ctx: Unit): PlanNode = node

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRex(node: Rex, ctx: Unit): PlanNode {
        val newNode = when (val op = node.op) {
            is Rex.Op.Var -> {
                val newType = bindings[op.ref].type
                node.copy(
                    type = newType
                )
            }
            is Rex.Op.Path.Key -> {
                val newPathKey = super.visitRexOp(node.op, ctx) as Rex.Op.Path.Key
                val newRoot = newPathKey.root
                val newKeyOp = newPathKey.key.op
                val t = newRoot.type.asNonAbsent()
                if (t is StructType && newKeyOp is Rex.Op.Lit) {
                    if (newKeyOp.value.type != PartiQLValueType.STRING) {
                        error("Invalid literal along path: $newKeyOp")
                    }
                    val fieldName = (newKeyOp.value as StringValue).value
                    val newType = t.fields.first { it.key == fieldName }.value
                    node.copy(
                        type = newType
                    )
                } else {
                    node
                }
            }
            is Rex.Op.Path.Symbol -> {
                val newPathSymbol = super.visitRexOp(node.op, ctx) as Rex.Op.Path.Symbol
                val newRoot = newPathSymbol.root
                val fieldName = newPathSymbol.key
                val t = newRoot.type.asNonAbsent()
                if (t is StructType) {
                    val newType = t.fields.first { it.key.equals(fieldName, ignoreCase = true) }.value
                    node.copy(
                        type = newType
                    )
                } else {
                    node
                }
            }
            else -> node
        }
        return super.visitRex(newNode, ctx)
    }
}
