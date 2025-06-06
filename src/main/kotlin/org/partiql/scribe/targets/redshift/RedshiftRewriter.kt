package org.partiql.scribe.targets.redshift

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.redshift.utils.rewriteToObjectTransform
import org.partiql.spi.types.PType

public open class RedshiftRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    override fun visitProject(
        rel: RelProject,
        ctx: ScribeContext?,
    ): Operator {
        val input = rel.input
        // Substitute any `RelExclude` with the `RelExclude`'s input
        val newNode =
            if (input is RelExclude) {
                val inputToExclude = input.input
                val newProject =
                    RelProject.create(
                        inputToExclude,
                        rel.projections,
                    )
                newProject.type = rel.type
                newProject
            } else {
                rel
            }
        return super.visitProject(newNode, ctx)
    }

    private fun Rex.isPathRex() = this is RexPathIndex || this is RexPathKey || this is RexPathSymbol

    override fun visitStruct(
        rex: RexStruct,
        ctx: ScribeContext,
    ): Operator {
        val struct = super.visitStruct(rex, ctx) as RexStruct
        val newStructFields =
            struct.fields.map { field ->
                val fieldValue = field.value
                val type = field.value.type
                // Rewrite any structs that have a field that's a ROW type and is a var reference or path.
                val newOp =
                    if (type.pType.code() == PType.ROW && (fieldValue is RexVar || fieldValue.isPathRex())) {
                        rewriteToObjectTransform(fieldValue)
                    } else {
                        fieldValue
                    }
                RexStruct.field(
                    field.key,
                    newOp,
                )
            }
        return RexStruct.create(newStructFields)
    }
}
