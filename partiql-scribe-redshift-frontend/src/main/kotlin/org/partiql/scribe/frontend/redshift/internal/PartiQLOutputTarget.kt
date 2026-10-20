package org.partiql.scribe.frontend.redshift.internal

import org.partiql.ast.Ast.exprBoolTest
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.TruthValue
import org.partiql.plan.Plan
import org.partiql.plan.rex.RexLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import java.math.BigDecimal

/**
 * The Scribe target that prints a translated plan as PartiQL.
 *
 * This is the Scribe PartiQL target with the rules that it lacks for constructs that the frontend emits. Without them,
 * the printed text does not parse and plan to the same query:
 *  - `x IS [NOT] TRUE | FALSE | UNKNOWN`: the planner lowers these to hidden functions, which the PartiQL target prints
 *    as calls of functions that do not exist.
 *  - `EXISTS (subquery)`: the PartiQL target quotes the function name, and PartiQL then reads the subquery as a scalar.
 *  - Approximate numeric literals: the PartiQL target prints `1000.0` for the DOUBLE PRECISION value `1e3`, which PartiQL
 *    reads as a DECIMAL. This target keeps an exponent.
 *  - Exact numeric literals: the PartiQL target prints the DECIMAL value `5.` as `5`, which PartiQL reads as an INTEGER,
 *    and `0.0000001` as `1E-7`, which PartiQL reads as a DOUBLE PRECISION. This target prints the plain digits, with a
 *    trailing point for a scale of 0 in the BIGINT range.
 */
internal class PartiQLOutputTarget : SqlTarget() {
    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override val features: SqlFeatures = SqlFeatures.Permissive()

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan = plan

    override fun getCalls(context: ScribeContext): SqlCalls = Calls(context)

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val transform =
            object : PlanToAst(session, getCalls(context), context) {
                override fun getRexConverter(locals: Locals): RexConverter = Rex(this, locals, context)
            }
        return transform.apply(newPlan)
    }

    private class Calls(context: ScribeContext) : SqlCalls(context) {
        override val rules: Map<String, SqlCallFn> =
            super.rules +
                mapOf(
                    "is_true" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.TRUE()) },
                    "is_false" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.FALSE()) },
                    "is_unknown" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.UNKNOWN()) },
                    "exists" to ::exists,
                )

        private fun exists(args: SqlArgs): Expr = exprCall(Identifier.regular("EXISTS"), args.map { it.expr })
    }

    private class Rex(
        transform: PlanToAst,
        locals: Locals,
        context: ScribeContext,
    ) : RexConverter(transform, locals, context) {
        override fun visitLit(
            rex: RexLit,
            ctx: Unit,
        ): Expr {
            val datum = rex.datum
            if (datum.isNull || datum.isMissing) return super.visitLit(rex, ctx)
            return when (datum.type.code()) {
                PType.DECIMAL, PType.NUMERIC -> exprLit(Literal.exactNum(exact(datum.bigDecimal)))
                PType.DOUBLE -> exprLit(Literal.approxNum(approximate(datum.double.toString())))
                // PartiQL has no REAL literal.
                PType.REAL -> exprCast(exprLit(Literal.approxNum(approximate(datum.float.toString()))), DataType.REAL())
                else -> super.visitLit(rex, ctx)
            }
        }

        private fun approximate(text: String): String = if (text.contains('E')) text else "${text}E0"

        /**
         * PartiQL reads an integer literal in the BIGINT range as an integer, and one outside it as a DECIMAL. A DECIMAL
         * with scale 0 in the BIGINT range gets a trailing point to stay a DECIMAL.
         */
        private fun exact(value: BigDecimal): String {
            val text = value.toPlainString()
            val bigint = value.scale() <= 0 && value >= MIN_BIGINT && value <= MAX_BIGINT
            return if (bigint) "$text." else text
        }

        private companion object {
            val MIN_BIGINT: BigDecimal = BigDecimal.valueOf(Long.MIN_VALUE)
            val MAX_BIGINT: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)
        }
    }
}
