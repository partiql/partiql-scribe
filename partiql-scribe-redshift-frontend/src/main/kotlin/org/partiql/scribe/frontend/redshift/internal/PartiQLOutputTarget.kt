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
import org.partiql.plan.RoutineRef
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexLit
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.frontend.redshift.RedshiftProblem
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType

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
 *
 * The [rules] print calls and casts whose Redshift semantics depend on the static types of the arguments; see
 * [OutputRules].
 *
 * @param notes The translation notes; a rule adds a note when it changes the form of the query.
 */
internal class PartiQLOutputTarget(
    private val notes: MutableList<RedshiftProblem>,
    private val rules: List<OutputRules> = OutputRules.ALL,
) : SqlTarget() {
    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override val features: SqlFeatures = SqlFeatures.Permissive()

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan = plan

    override fun getCalls(context: ScribeContext): SqlCalls = Calls(context, notes, rules)

    override fun planToAst(
        newPlan: Plan,
        session: Session,
        context: ScribeContext,
    ): AstNode {
        val calls = Calls(context, notes, rules)
        val transform =
            object : PlanToAst(session, calls, context) {
                override fun getRexConverter(locals: Locals): RexConverter = Rex(this, locals, context, calls.output, rules)
            }
        return transform.apply(newPlan)
    }

    private class Calls(
        context: ScribeContext,
        notes: MutableList<RedshiftProblem>,
        rules: List<OutputRules>,
    ) : SqlCalls(context) {
        val output = OutputContext(this, context, notes)

        override val rules: Map<String, SqlCallFn> =
            super.rules +
                mapOf(
                    "is_true" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.TRUE()) },
                    "is_false" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.FALSE()) },
                    "is_unknown" to { args -> exprBoolTest(args[0].expr, not = false, TruthValue.UNKNOWN()) },
                    "exists" to ::exists,
                ) +
                rules.flatMap { it.scalar(output).entries }.associate { it.key to it.value }

        override val aggregateRules: Map<RoutineRef, (SqlArgs, Boolean) -> Expr> =
            rules.flatMap { it.aggregate(output).entries }.associate { RoutineRef(SYSTEM_CATALOG, Name.of(it.key)) to it.value }

        private fun exists(args: SqlArgs): Expr = exprCall(Identifier.regular("EXISTS"), args.map { it.expr })
    }

    private class Rex(
        transform: PlanToAst,
        locals: Locals,
        context: ScribeContext,
        private val output: OutputContext,
        private val rules: List<OutputRules>,
    ) : RexConverter(transform, locals, context) {
        override fun visitCast(
            rex: RexCast,
            ctx: Unit,
        ): Expr {
            if (rules.isNotEmpty()) {
                val operand = SqlArg(visitRex(rex.operand, ctx), rex.operand.type.pType)
                for (rule in rules) {
                    rule.cast(output, operand, rex.target)?.let { return it }
                }
            }
            return super.visitCast(rex, ctx)
        }

        override fun visitLit(
            rex: RexLit,
            ctx: Unit,
        ): Expr {
            val datum = rex.datum
            if (datum.isNull || datum.isMissing) return super.visitLit(rex, ctx)
            return when (datum.type.code()) {
                PType.DOUBLE -> exprLit(Literal.approxNum(approximate(datum.double.toString())))
                // PartiQL has no REAL literal.
                PType.REAL -> exprCast(exprLit(Literal.approxNum(approximate(datum.float.toString()))), DataType.REAL())
                else -> super.visitLit(rex, ctx)
            }
        }

        private fun approximate(text: String): String = if (text.contains('E')) text else "${text}E0"
    }

    private companion object {
        /** The name of the PartiQL system catalog, which holds the built-in functions. */
        const val SYSTEM_CATALOG = "\$system"
    }
}

/**
 * Print rules for one area of the translation.
 *
 * The AST builder translates a Redshift construct to the PartiQL construct with the same meaning when it can do so
 * without types. When the Redshift semantics depend on the static types of the arguments (for example, Redshift `AVG`
 * of an integer column truncates the result), the AST builder emits the PartiQL construct, the planner resolves the
 * argument types, and a rule here prints the construct so that it has the Redshift semantics for those types.
 *
 * The frontend prints only plans that it built from Redshift input, so a rule can read a PartiQL call or cast in the
 * plan as the Redshift construct that the AST builder translated it from.
 */
internal interface OutputRules {
    /** Rules for scalar calls, by function name without the hidden-function prefix. */
    fun scalar(output: OutputContext): Map<String, SqlCallFn> = emptyMap()

    /** Rules for calls of built-in aggregations, by function name. The second argument is true for DISTINCT. */
    fun aggregate(output: OutputContext): Map<String, (SqlArgs, Boolean) -> Expr> = emptyMap()

    /** Returns the printed cast of [operand] to [target], or null to print the PartiQL cast. */
    fun cast(
        output: OutputContext,
        operand: SqlArg,
        target: PType,
    ): Expr? = null

    companion object {
        /** The print rules of the frontend. */
        val ALL: List<OutputRules> = emptyList()
    }
}

/**
 * What an [OutputRules] rule can use: the calls of the output target, translation notes, and errors.
 */
internal class OutputContext(
    val calls: SqlCalls,
    context: ScribeContext,
    private val notes: MutableList<RedshiftProblem>,
) {
    private val listener = context.getProblemListener()

    /**
     * Adds a translation note. A note from a print rule has no location, because the plan does not keep the locations
     * of the input; the same note is added once.
     */
    fun note(message: String) {
        if (notes.none { it.code == RedshiftProblem.Code.TRANSLATION_INFO && it.line == null && it.message == message }) {
            notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message))
        }
    }

    /** Stops the translation: the construct has no translation for the argument types. */
    fun unsupported(message: String): Nothing =
        listener.reportAndThrow(ScribeProblem.simpleError(ScribeProblem.UNSUPPORTED_OPERATION, message))
}
