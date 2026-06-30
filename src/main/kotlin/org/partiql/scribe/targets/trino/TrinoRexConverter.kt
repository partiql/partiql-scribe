package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprVarRef
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.utils.unquotedStringExpr
import org.partiql.scribe.targets.trino.utils.TRANSFORM_VAR
import org.partiql.spi.types.PType

public open class TrinoRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context) {
    /**
     * Convert any plan string literals (i.e. [RexLit]s) that have a string value of [TRANSFORM_VAR] into an unquoted
     * string AST expr (i.e. [ExprVarRef]).
     */
    override fun visitLit(
        rex: RexLit,
        ctx: Unit,
    ): Expr {
        return when (rex.type.pType.code()) {
            PType.STRING -> {
                val stringValue = rex.datum.string
                if (stringValue == TRANSFORM_VAR) {
                    unquotedStringExpr(stringValue)
                } else {
                    super.visitLit(rex, ctx)
                }
            }
            else -> super.visitLit(rex, ctx)
        }
    }

    /**
     * For MAP types, keep bracket notation (PathStep.Element) since Trino uses subscript for map access.
     * For ROW/struct types, convert to dot notation (PathStep.Field) since Trino uses field dereference.
     */
    override fun visitPathKey(
        rex: RexPathKey,
        ctx: Unit,
    ): Expr {
        val operandType =
            try {
                rex.operand.type.pType.code()
            } catch (_: UnsupportedOperationException) {
                -1
            }
        if (operandType == PType.MAP) {
            return super.visitPathKey(rex, ctx)
        }
        // For non-MAP (ROW/struct/other): convert bracket to dot notation
        val key = rex.key
        if (key is RexLit && key.type.pType.code() == PType.STRING) {
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
