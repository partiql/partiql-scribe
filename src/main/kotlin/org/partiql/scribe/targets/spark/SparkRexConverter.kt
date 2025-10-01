package org.partiql.scribe.targets.spark

import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprVarRef
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.utils.unquotedStringExpr
import org.partiql.scribe.targets.spark.utils.TRANSFORM_VAR
import org.partiql.spi.types.PType

public open class SparkRexConverter(
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
     * Throw an error on any function call argument that is of type TIME/TIMEZ.
     */
    override fun visitCall(
        rex: RexCall,
        ctx: Unit,
    ): Expr {
        if (rex.args.any { arg -> arg.type.pType.code() == PType.TIME || arg.type.pType.code() == PType.TIMEZ }) {
            context.getProblemListener().report(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Spark does not support Time types.",
                ),
            )
        }
        return super.visitCall(rex, ctx)
    }
}
