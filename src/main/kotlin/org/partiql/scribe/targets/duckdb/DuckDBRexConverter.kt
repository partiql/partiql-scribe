package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.utils.isUnknown
import org.partiql.spi.types.PType

public open class DuckDBRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context) {
    /**
     * For MAP types, keep bracket notation (PathStep.Element) since DuckDB uses subscript for map access.
     * For ROW/struct types, convert to dot notation (PathStep.Field) since DuckDB uses field dereference.
     */
    override fun visitPathKey(
        rex: RexPathKey,
        ctx: Unit,
    ): Expr {
        val operandType =
            try {
                rex.operand.type.pType
            } catch (_: UnsupportedOperationException) {
                null
            }
        if (operandType != null && operandType.code() == PType.MAP) {
            return super.visitPathKey(rex, ctx)
        }
        // For non-MAP (ROW/struct/other): convert bracket to dot notation
        val key = rex.key
        if (key is RexLit && key.type.pType.code() == PType.STRING && !key.datum.isUnknown()) {
            val prev = visitRex(rex.operand, ctx)
            val fieldName = key.datum.string
            val step = exprPathStepField(Identifier.Simple.delimited(fieldName))
            return if (prev is ExprPath) {
                exprPath(prev.root, prev.steps + step)
            } else {
                exprPath(prev, listOf(step))
            }
        }
        return super.visitPathKey(rex, ctx)
    }
}
