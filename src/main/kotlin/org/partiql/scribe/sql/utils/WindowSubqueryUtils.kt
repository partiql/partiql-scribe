package org.partiql.scribe.sql.utils

import org.partiql.plan.Operator
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rel.RelWith
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexMap
import org.partiql.plan.rex.RexMapDynamic
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener

/**
 * Reports and throws a [ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION] with [message].
 */
internal fun ScribeProblemListener.unsupported(message: String): Nothing =
    reportAndThrow(
        ScribeProblem.simpleError(
            code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
            message = message,
        ),
    )

/**
 * Returns true if the subquery [rel] contains a window function at its own query level and a correlation reference
 * anywhere. [isOuterWithScope] tells whether a scope outside the subquery (0 is the scope that contains it) binds common
 * table expressions; a reference to a common table expression is not a correlation reference.
 */
internal fun isCorrelatedSubqueryWithWindow(
    rel: Rel,
    isOuterWithScope: (Int) -> Boolean = { false },
): Boolean {
    var hasWindow = false
    var isCorrelated = false

    // depth: scopes between op and the subquery's root; level: nested queries (a WITH clause adds a scope, not a query).
    fun visit(
        op: Operator,
        depth: Int,
        level: Int,
    ) {
        if (op is RelWindow && level == 0) hasWindow = true
        if (op is RexVar && op.scope > depth && !isOuterWithScope(op.scope - depth - 1)) isCorrelated = true
        if (hasWindow && isCorrelated) return
        val nested = op is RexSelect || op is RexSubquery || op is RexPivot
        for (child in children(op)) {
            when {
                op is RelWith -> visit(child, depth + 1, level)
                nested || (op is Rex && child is Rel) || (op is RelCorrelate && child === op.right) ->
                    visit(child, depth + 1, level + 1)
                else -> visit(child, depth, level)
            }
        }
    }
    visit(rel, 0, 0)
    return hasWindow && isCorrelated
}

/**
 * The children of [op]: its operands, plus the expressions that the plan's operands leave out.
 */
@Suppress("DEPRECATION")
private fun children(op: Operator): List<Operator> =
    op.operands.flatMap { it.toList() } +
        when (op) {
            is RelFilter -> listOf(op.predicate)
            is RelProject -> op.projections
            is RelAggregate -> op.measures.flatMap { it.args } + op.groups
            is RelWindow -> op.windowFunctions.flatMap { it.arguments } + op.partitions + op.collations.map { it.column }
            is RelSort -> op.collations.map { it.column }
            is RelJoin -> listOf(op.condition)
            is RelWith -> op.elements.map { it.representation }
            is RexSelect -> listOf(op.constructor)
            is RexSubquery -> listOf(op.constructor)
            is RexPivot -> listOf(op.key, op.value)
            is RexCase -> listOfNotNull(op.default) + op.branches.flatMap { listOf(it.condition, it.result) }
            is RexStruct -> op.fields.flatMap { listOf(it.key, it.value) }
            is RexMap -> op.entries.flatMap { listOf(it.key, it.value) }
            is RexMapDynamic -> op.entries.flatMap { listOf(it.key, it.value) }
            is RexPathIndex -> listOf(op.index)
            is RexPathKey -> listOf(op.key)
            else -> emptyList()
        }
