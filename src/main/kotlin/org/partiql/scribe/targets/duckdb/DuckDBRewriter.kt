package org.partiql.scribe.targets.duckdb

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.isPathRex
import org.partiql.scribe.sql.utils.isUnknown
import org.partiql.scribe.targets.duckdb.utils.toRexDuckDB
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Plan -> plan rewriter for the DuckDB target.
 *
 * It drops standalone [RelExclude] nodes feeding a [RelProject] (matching the other SQL targets), reconstructs
 * struct fields that contain excluded fields (see DuckDBExcludeTranspilation.md), and shifts literal array
 * indexes from PartiQL's 0-based to DuckDB's 1-based indexing. Other DuckDB-specific rewrites (BAG -> ARRAY,
 * IS MISSING -> IS NULL, etc.) will be added here.
 */
public open class DuckDBRewriter(internal val context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    private val listener = context.getProblemListener()

    override fun visitProject(
        rel: RelProject,
        ctx: ScribeContext?,
    ): Operator {
        val input = rel.input
        val newNode =
            if (input is RelExclude) {
                val newProject = RelProject.create(input.input, rel.projections)
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
                    if (fieldValue is RexVar || fieldValue.isPathRex() || fieldValue is RexStruct) {
                        type.pType.toRexDuckDB(
                            prefixPath = fieldValue,
                            context = context,
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
        checkPathOperand(rex.operand)
        if (rex.key !is RexLit) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB path keys must be literals, found ${rex.key}",
                ),
            )
        }
        if (rex.key.type.pType.code() != PType.STRING) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB path key must be a string literal, found ${rex.key.type}",
                ),
            )
        }
        return super.visitPathKey(rex, ctx)
    }

    override fun visitPathSymbol(
        rex: RexPathSymbol,
        ctx: ScribeContext,
    ): Operator {
        checkPathOperand(rex.operand)
        return super.visitPathSymbol(rex, ctx)
    }

    /**
     * A key/symbol path must be on a ROW or MAP. A DYNAMIC (untyped) operand, or one whose type isn't available, is
     * passed through: its shape is only known at runtime (see [DuckDBRexConverter.visitPathKey]).
     */
    private fun checkPathOperand(operand: Rex) {
        val code =
            try {
                operand.type.pType.code()
            } catch (_: UnsupportedOperationException) {
                return
            }
        if (code != PType.ROW && code != PType.MAP && code != PType.DYNAMIC) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB path expression must be on a ROW or MAP type, found ${operand.type}",
                ),
            )
        }
    }

    /**
     * From DuckDB docs,
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
        // Rewrite the operand and index first, so nested indexes (`a[0].b[0]`) are shifted too, not just the outermost.
        val node = super.visitPathIndex(node, ctx) as RexPathIndex
        // MAP subscript access — pass through without rewriting
        val type = node.operand.type
        if (type.pType.code() == PType.MAP) {
            return node
        }

        // Assert root type
        if (type.pType.code() != PType.ARRAY) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB only supports indexing on `array` type data; found $type",
                ),
            )
        }

        // Assert key type. A non-literal index (e.g. `x[i]`) can't be shifted safely: a negative runtime value would
        // index from the end in DuckDB, while PartiQL returns MISSING.
        val op = node.index
        if (op !is RexLit) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB array indexing only supports integer literals, e.g. x[0].",
                ),
            )
        }

        if (op.datum.isUnknown()) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB array index must be a non-null integer " +
                        "(PartiQL indexes are 0-based; Scribe rewrites PartiQL `x[0]` to DuckDB `x[1]`).",
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
                            "DuckDB array index must be a non-null integer " +
                                "(PartiQL indexes are 0-based; Scribe rewrites PartiQL `x[0]` to DuckDB `x[1]`).",
                        ),
                    )
            }
        // PartiQL indexes are 0-based, so after shifting a valid index is >= 1. A negative index has no DuckDB
        // equivalent (DuckDB counts negative indexes from the end), and Long.MAX_VALUE overflows when shifted.
        if (rexIndex < 1) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "PartiQL array index must be a non-negative integer less than ${Long.MAX_VALUE} " +
                        "(PartiQL indexes are 0-based; Scribe rewrites PartiQL `x[0]` to DuckDB `x[1]`).",
                ),
            )
        }
        // rewrite to be 1-indexed
        val pathIndex = RexPathIndex.create(node.operand, RexLit.create(Datum.bigint(rexIndex)))
        pathIndex.type = node.type
        return pathIndex
    }
}
