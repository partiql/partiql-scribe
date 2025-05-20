package org.partiql.scribe.sql

import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem

public abstract class SqlFeatures : OperatorVisitor<Unit, ScribeContext> {
    public fun validate(
        plan: Plan,
        context: ScribeContext,
    ) {
        when (val action = plan.action) {
            is Action.Query -> visit(action.rex, context)
            else ->
                context.getProblemListener().report(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "Can only transform a query statement",
                    ),
                )
        }
    }

    /**
     * An [SqlFeatures] which denies all features (rel.op and rex.op) by default; thereby requiring explicit opt-in.
     */
    public open class Defensive : SqlFeatures() {
        public open val allow: Set<Class<*>> = emptySet()

        override fun defaultReturn(
            node: Operator,
            ctx: ScribeContext,
        ) {
            if (!allow.contains(node::class.java)) {
                ctx.getProblemListener().report(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "PartiQL feature (${feature(node)}) is not supported.",
                    ),
                )
            }
        }
    }

    public open class Permissive : SqlFeatures() {
        override fun defaultReturn(
            node: Operator,
            ctx: ScribeContext,
        ): Unit = Unit
    }

    internal fun feature(node: Operator): String {
        return node.javaClass.typeName.let {
            it.substring(it.lastIndexOf(".") + 1)
        }.replace('$', '.')
    }
}
