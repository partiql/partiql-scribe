package org.partiql.scribe.sql.utils

import org.partiql.plan.Operator
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelUnpivot
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rel.RelWith
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexVar

/**
 * Returns true if the subquery [rel] contains a window function at its own query level and a correlation reference
 * anywhere, i.e. a variable that refers to a scope outside the subquery.
 */
internal fun isCorrelatedSubqueryWithWindow(rel: Rel): Boolean {
    var hasWindow = false
    var isCorrelated = false
    visitWithDepth(rel, 0) { op, depth ->
        if (op is RelWindow && depth == 0) {
            hasWindow = true
        }
        if (op.refersOutside(depth)) {
            isCorrelated = true
        }
    }
    return hasWindow && isCorrelated
}

/**
 * Returns true if a window function at the subquery [rel]'s own query level contains a correlation reference in its
 * arguments, partitions, or ordering.
 */
@Suppress("DEPRECATION")
internal fun hasCorrelatedWindowFunction(rel: Rel): Boolean {
    var isCorrelated = false
    visitWithDepth(rel, 0) { op, depth ->
        if (op is RelWindow && depth == 0) {
            val exprs = op.windowFunctions.flatMap { it.arguments } + op.partitions + op.collations.map { it.column }
            exprs.forEach { expr -> visitWithDepth(expr, depth) { e, d -> if (e.refersOutside(d)) isCorrelated = true } }
        }
    }
    return isCorrelated
}

/**
 * A variable's scope counts the scopes it skips; it refers outside the subquery if it skips more scopes than it is
 * nested inside the subquery.
 */
private fun Operator.refersOutside(depth: Int): Boolean = this is RexVar && this.scope > depth

/**
 * Visits [op] and the operators and expressions below it. [depth] is the number of scopes (nested subqueries, the
 * right side of a lateral join, or the WITH clause's bindings) between [op] and the root.
 */
@Suppress("DEPRECATION")
private fun visitWithDepth(
    op: Operator,
    depth: Int,
    visit: (Operator, Int) -> Unit,
) {
    visit(op, depth)
    val children: List<Pair<Operator, Int>> =
        when (op) {
            is RelFilter -> listOf(op.input to depth, op.predicate to depth)
            is RelProject -> listOf(op.input to depth) + op.projections.map { it to depth }
            is RelAggregate ->
                listOf(op.input to depth) + op.measures.flatMap { it.args }.map { it to depth } + op.groups.map { it to depth }
            is RelWindow ->
                listOf(op.input to depth) +
                    op.windowFunctions.flatMap { it.arguments }.map { it to depth } +
                    op.partitions.map { it to depth } +
                    op.collations.map { it.column to depth }
            is RelSort -> listOf(op.input to depth) + op.collations.map { it.column to depth }
            is RelJoin -> listOf(op.left to depth, op.right to depth, op.condition to depth)
            is RelCorrelate -> listOf(op.left to depth, op.right to depth + 1)
            // The WITH clause's bindings are a scope: a CTE reference is a variable that refers to it.
            is RelWith -> listOf(op.input to depth + 1) + op.elements.map { it.representation to depth + 1 }
            is RelLimit -> listOf(op.input to depth, op.limit to depth)
            is RelOffset -> listOf(op.input to depth, op.offset to depth)
            is RelScan -> listOf(op.rex to depth)
            is RelIterate -> listOf(op.rex to depth)
            is RelUnpivot -> listOf(op.rex to depth)
            is RexSelect -> listOf(op.input to depth + 1, op.constructor to depth + 1)
            is RexPivot -> listOf(op.input to depth + 1, op.key to depth + 1, op.value to depth + 1)
            else ->
                op.operands.flatMap { it.toList() }.map { child ->
                    child to (if (op is Rex && child is Rel) depth + 1 else depth)
                }
        }
    children.forEach { (child, d) -> visitWithDepth(child, d, visit) }
}
