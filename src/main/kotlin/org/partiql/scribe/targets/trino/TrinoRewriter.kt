package org.partiql.scribe.targets.trino

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

public open class TrinoRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    private val listener = context.getProblemListener()

    override fun visitPathKey(
        rex: RexPathKey,
        ctx: ScribeContext,
    ): Operator {
        if (rex.operand.type.pType.code() != PType.ROW) {
            error("Trino path expression must be on a ROW type (PartiQL STRUCT), found ${rex.operand.type}")
        }
        if (rex.key !is RexLit) {
            error("Trino does not support path non-literal path expressions, found ${rex.key}")
        }
        if (rex.key.type.pType.code() != PType.STRING) {
            error("Trino path expression must be a string literal.")
        }
        return super.visitPathKey(rex, ctx)
    }

    override fun visitPathSymbol(
        rex: RexPathSymbol,
        ctx: ScribeContext,
    ): Operator {
        if (rex.operand.type.pType.code() != PType.ROW) {
            error("Trino path expression must be on a ROW type (PartiQL STRUCT), found ${rex.operand.type}")
        }
        return super.visitPathSymbol(rex, ctx)
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
    override fun visitPathIndex(
        node: RexPathIndex,
        ctx: ScribeContext,
    ): Operator {
        // Assert root type
        val type = node.operand.type
        if (type.pType.code() != PType.ARRAY) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "Trino only supports indexing on `array` type data; found $type",
                ),
            )
        }

        // Assert key type
        val op = node.index
        if (op !is RexLit) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "Trino array indexing only supports integer literals, e.g. x[1].",
                ),
            )
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
                else ->
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            ScribeProblem.INVALID_PLAN,
                            "Trino array index must be a non-null integer, e.g. x[1].",
                        ),
                    )
            }
        // rewrite to be 1-indexed
        return RexPathIndex.create(node.operand, RexLit.create(Datum.bigint(rexIndex)))
    }
}
