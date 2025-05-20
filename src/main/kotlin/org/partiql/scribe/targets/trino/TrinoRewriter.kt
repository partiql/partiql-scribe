package org.partiql.scribe.targets.trino

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathIndex
import org.partiql.scribe.ScribeContext
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

public open class TrinoRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    /**
     * From Trino docs,
     *
     * "The [] operator is used to access an element of an array and is indexed starting from one".
     *
     * @param node
     * @param ctx
     * @return
     */
    override fun visitPathIndex(
        node: RexPathIndex,
        ctx: ScribeContext,
    ): Operator {
        // Assert root type
        val type = node.operand.type
        if (type.pType.code() != PType.ARRAY) {
            error("Trino only supports indexing on `array` type data; found $type") // TODO ALAN convert to error message
        }

        // Assert key type
        val op = node.index
        if (op !is RexLit) {
            error("Trino array indexing only supports integer literals, e.g. x[1].") // TODO ALAN convert to error message
            return super.visitPathIndex(node, ctx)
        }

        val rexIndex =
            when (op.datum.type.code()) {
                PType.TINYINT -> {
                    op.datum.byte.toLong() + 1
                }
                PType.SMALLINT -> {
                    op.datum.short.toLong() + 1
                }
                PType.INTEGER -> {
                    op.datum.int.toLong() + 1
                }
                PType.BIGINT -> {
                    op.datum.long + 1
                }
                else -> error("Trino array index must be a non-null integer, e.g. x[1].") // TODO ALAN convert to error message
            }
        // rewrite to be 1-indexed
        return RexPathIndex.create(node.operand, RexLit.create(Datum.bigint(rexIndex)))
    }
}
