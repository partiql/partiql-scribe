package org.partiql.scribe

import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
import org.partiql.plan.rexOpVar
import org.partiql.plan.util.PlanRewriter
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * Rewrite every [Rex.Op.Var] to a [Rex.Op.Path.Symbol] with a root of [Rex.Op.Var] specified from [newVar]. Uses the
 * original [Rex.Op.Var] to get the [Rel.Binding.name] which will be the path's key.
 *
 * E.g. given input type of [<bindingName0: type0>, <bindingName1: type1>] and newVar = 0, rewrite varRef(0) and
 * varRef(1) to paths varRef(0).bindingName0 and varRef(0).bindingName1 respectively.
 */
internal class VarToPathRewriter(
    val newVar: Int,
    val origType: Rel.Type
): PlanRewriter<StaticType>() {
    override fun defaultReturn(node: PlanNode, ctx: StaticType): PlanNode = node

    /** Pass along the `Rex`'s [StaticType]. */
    override fun visitRex(node: Rex, ctx: StaticType) = super.visitRex(node, node.type)

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpVar(node: Rex.Op.Var, ctx: StaticType): PlanNode {
        val newName = origType.schema[node.ref].name
        return rexOpPathKey(
            root = rex(
                op = rexOpVar(newVar),
                type = ctx
            ),
            key = rex(StaticType.STRING, rexOpLit(stringValue(newName)))
        )
    }
}
