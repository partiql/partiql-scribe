package org.partiql.scribe.targets.trino

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.isPathRex
import org.partiql.scribe.targets.trino.utils.toRexTrino
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

public open class TrinoRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    private val listener = context.getProblemListener()

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
                    if (fieldValue is RexVar || fieldValue.isPathRex()) {
                        type.pType.toRexTrino(
                            prefixPath = fieldValue,
                        )
                    } else {
                        fieldValue
                    }
                RexStruct.field(
                    field.key,
                    newOp,
                )
            }
        val newStruct = RexStruct.create(newStructFields)
        newStruct.type = struct.type
        return newStruct
    }

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
        val pathIndex = RexPathIndex.create(node.operand, RexLit.create(Datum.bigint(rexIndex)))
        pathIndex.type = node.type
        return pathIndex
    }
}
