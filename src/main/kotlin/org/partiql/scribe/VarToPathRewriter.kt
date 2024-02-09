package org.partiql.scribe

import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpPathSymbol
import org.partiql.plan.rexOpVar
import org.partiql.plan.util.PlanRewriter
import org.partiql.types.StaticType

/**
 * Rewrite every [Rex.Op.Var] to a [Rex.Op.Path.Symbol] with a root of [Rex.Op.Var] specified from [newVar].
 */
internal class VarToPathRewriter(
    val newVar: Int,
    val oldVarToName: Map<Int, String>
): PlanRewriter<StaticType>() {
    fun apply(rex: Rex): PlanNode = rex.accept(this, StaticType.ANY)

    override fun defaultReturn(node: PlanNode, ctx: StaticType): PlanNode = node

    override fun visitRex(node: Rex, ctx: StaticType) = super.visitRex(node, node.type)

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: StaticType): PlanNode {
        return rexOpPathSymbol(
            root = rex(
                op = rexOpVar(
                    newVar
                ),
                type = ctx
            ),
            key = oldVarToName[node.ref]!!
        )
    }

}