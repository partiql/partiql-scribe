package org.partiql.scribe.sql

import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem

public abstract class SqlFeatures : OperatorVisitor<Unit, ScribeContext> {
    fun validate(plan: Plan, context: ScribeContext) {
        when (val action = plan.action) {
            is Action.Query -> visit(action.rex, context)
            else -> context.getErrorListener().report(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Can only transform a query statement",
                )
            )
        }
    }

    public open class Defensive : SqlFeatures() {
        open val allow: Set<Class<*>> = emptySet()

        override fun defaultReturn(node: Operator, ctx: ScribeContext) {
            if (!allow.contains(node::class.java)) {
                ctx.getErrorListener().report(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "PartiQL feature (${feature(node)}) is not supported.",
                    )
                )
            }
        }
    }

    public open class Permissive : SqlFeatures() {
        override fun defaultReturn(node: Operator, ctx: ScribeContext) = Unit
    }

    internal fun feature(node: Operator): String {
        return node.javaClass.typeName.let {
            it.substring(it.lastIndexOf(".") + 1)
        }.replace('$', '.')
    }
}
