package org.partiql.scribe.targets.duckdb

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.isPathRex
import org.partiql.scribe.sql.utils.isUnknown
import org.partiql.scribe.targets.duckdb.utils.toRexDuckDB
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Plan -> plan rewriter for the DuckDB target.
 *
 * It drops standalone [RelExclude] nodes feeding a [RelProject] (matching the other SQL targets) and
 * reconstructs struct fields that contain excluded fields (see DuckDBExcludeTranspilation.md). Other
 * DuckDB-specific rewrites (array index shifting, BAG -> ARRAY, IS MISSING -> IS NULL, etc.) will be added here.
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

        // Non-literal index (e.g. `x[i]`, `x[i + 1]`): rewrite to `<index> + 1`. Its value isn't known at transpile
        // time, so the bounds below can't be checked; an out-of-range index yields NULL in DuckDB.
        val op = node.index
        if (op !is RexLit) {
            val oneBased = RexCall.create(plusFnSig, listOf(op, RexLit.create(Datum.bigint(1))))
            oneBased.type = op.type
            val pathIndex = RexPathIndex.create(node.operand, oneBased)
            pathIndex.type = node.type
            return pathIndex
        }

        if (op.datum.isUnknown()) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB array index must be a non-null integer, e.g. x[1].",
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
                            "DuckDB array index must be a non-null integer, e.g. x[1].",
                        ),
                    )
            }
        // PartiQL indexes are 0-based, so after shifting a valid index is >= 1. A negative index has no DuckDB
        // equivalent (DuckDB counts negative indexes from the end), and Long.MAX_VALUE overflows when shifted.
        if (rexIndex < 1) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN,
                    "DuckDB array index must be a non-negative integer less than ${Long.MAX_VALUE}, e.g. x[1].",
                ),
            )
        }
        // rewrite to be 1-indexed
        val pathIndex = RexPathIndex.create(node.operand, RexLit.create(Datum.bigint(rexIndex)))
        pathIndex.type = node.type
        return pathIndex
    }

    private companion object {
        // `plus` is rendered as the `+` operator by `SqlCalls.plusFn`.
        private val plusFnSig =
            Fn.Builder("plus")
                .addParameters(
                    Parameter("lhs", PType.dynamic()),
                    Parameter("rhs", PType.bigint()),
                )
                .returns(PType.dynamic())
                .build()
    }
}
