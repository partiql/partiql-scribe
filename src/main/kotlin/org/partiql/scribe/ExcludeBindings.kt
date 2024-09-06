package org.partiql.scribe

import org.partiql.plan.Identifier
import org.partiql.plan.Rel
import org.partiql.types.AnyOfType
import org.partiql.types.BagType
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StructType

/**
 * Apply the given [excludePath] to the [bindings].
 *
 * For any of the [StaticType] that have an excluded attribute, include "EXPAND" in the metas.
 */
internal fun excludeBindings(bindings: List<Rel.Binding>, excludePath: Rel.Op.Exclude.Item): List<Rel.Binding> {
    val newBindings = bindings.toMutableList()
    val varRef = excludePath.root.ref
    val binding = bindings[varRef]
    val newType = binding.type.exclude(excludePath.steps)
    newBindings[varRef] = binding.copy(
        type = newType
    )
    return newBindings
}

private fun StaticType.exclude(steps: List<Rel.Op.Exclude.Step>): StaticType =
    when (val nonNullType = this.asNonNullable()) {
        is StructType -> nonNullType.exclude(steps)
        is CollectionType -> nonNullType.exclude(steps)
        is AnyOfType -> StaticType.unionOf(
            nonNullType.types.map { it.exclude(steps) }.toSet()
        )
        else -> this
    }.flatten()

/**
 * Applies exclusions to struct fields and annotates structs that have an excluded field (or nested field) with
 * a meta "EXPAND" set to true.
 *
 * @param steps
 * @return
 */
private fun StructType.exclude(steps: List<Rel.Op.Exclude.Step>): StaticType {
    val step = steps.first()
    val output = fields.mapNotNull { field ->
        val newField = if (steps.size == 1) {
            // excluding at current level
            null
        } else {
            // excluding at a deeper level
            val k = field.key
            val v = field.value.exclude(steps.drop(1))
            StructType.Field(k, v)
        }
        when (step) {
            is Rel.Op.Exclude.Step.StructField -> {
                if (step.symbol.isEquivalentTo(field.key)) {
                    newField
                } else {
                    field
                }
            }
            is Rel.Op.Exclude.Step.StructWildcard -> newField
            else -> field
        }
    }
    val newMetas = this.metas.toMutableMap()
    newMetas["EXPAND"] = true
    return this.copy(fields = output, metas = newMetas)
}

/**
 * Applies exclusions to collection element type.
 *
 * Note: this function should not be called for Redshift since collection index and wildcard exclusion is not currently
 * supported for Redshift transpilation.
 *
 * @param steps
 * @return
 */
private fun CollectionType.exclude(steps: List<Rel.Op.Exclude.Step>): StaticType {
    var e = this.elementType
    val newMetas = this.metas.toMutableMap()
    newMetas["EXPAND"] = true
    when (steps.first()) {
        is Rel.Op.Exclude.Step.CollIndex -> {
            if (steps.size > 1) {
                e = e.exclude(steps.drop(1))
            }
        }
        is Rel.Op.Exclude.Step.CollWildcard -> {
            if (steps.size > 1) {
                e = e.exclude(steps.drop(1))
                newMetas["EXPAND"] = true
            }
            // currently no change to elementType if collection wildcard is last element; this behavior could
            // change based on RFC definition
        }
        else -> {
            // currently no change to elementType and no error thrown; could consider an error/warning in
            // the future
        }
    }
    return when (this) {
        is BagType -> this.copy(
            elementType = e,
            metas = newMetas
        )
        is ListType -> this.copy(
            elementType = e,
            metas = newMetas
        )
        is SexpType -> this.copy(
            elementType = e,
            metas = newMetas
        )
    }
}

/**
 * Compare an identifier to a struct field; handling case-insensitive comparisons.
 *
 * @param other
 * @return
 */
private fun Identifier.Symbol.isEquivalentTo(other: String): Boolean = when (caseSensitivity) {
    Identifier.CaseSensitivity.SENSITIVE -> symbol == other
    Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other, ignoreCase = true)
}
