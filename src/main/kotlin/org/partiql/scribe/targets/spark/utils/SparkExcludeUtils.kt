package org.partiql.scribe.targets.spark.utils

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexStruct
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.containsExcludedFieldMeta
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal const val TRANSFORM_VAR = "___coll_wildcard___"

/**
 * Converts this [PType] to a [Rex]. If the [PType] contains the meta "CONTAINS_EXCLUDED_FIELD" meta, additional
 * logic is applied to reconstruct the [PType.ROW] or collection ([PType.ARRAY] or [PType.BAG]) to properly exclude
 * any ROW fields.
 */
internal fun PType.toRexSpark(
    prefixPath: Rex,
    context: ScribeContext,
): Rex {
    val type = this
    if (!type.containsExcludedFieldMeta()) {
        return prefixPath
    }
    return when (type.code()) {
        PType.ROW ->
            when (type.fields.size) {
                0 -> {
                    context.getProblemListener().reportAndThrow(
                        ScribeProblem.simpleError(
                            ScribeProblem.INTERNAL_ERROR,
                            "Currently do not support outputting empty structs in Spark. Consider `EXCLUDE` on the outer struct.",
                        ),
                    )
                }
                else -> type.toRexStruct(prefixPath, context)
            }
        PType.ARRAY, PType.BAG -> type.toRexCallTransform(prefixPath, context)
        else -> prefixPath
    }
}

// https://spark.apache.org/docs/latest/api/sql/index.html#transform
private val transform_fn_sig =
    Fn.Builder("transform")
        .addParameters(
            Parameter("array_expr", PType.dynamic()),
            Parameter("element_var", PType.dynamic()),
            Parameter("element_expr", PType.dynamic()),
        )
        .returns(PType.dynamic())
        .build()

/**
 * Converts the collection [PType] to a [RexCall] representing the Spark `transform` function.
 *
 * Requires [this] [PType] to be a [PType.ARRAY] or [PType.BAG].
 */
private fun PType.toRexCallTransform(
    prefixPath: Rex,
    context: ScribeContext,
): RexCall {
    val elementType = this.typeParameter
    val elementVar =
        RexLit.create(
            Datum.string(TRANSFORM_VAR),
        )
    return RexCall.create(
        transform_fn_sig,
        listOf(
            prefixPath,
            elementVar,
            elementType.toRexSpark(
                elementVar,
                context,
            ),
        ),
    )
}

/**
 * Converts the [PType.ROW] to a [RexStruct] representing the Spark `struct` function.
 *
 * Requires [this] [PType] to be a [PType.ROW].
 */
private fun PType.toRexStruct(
    prefixPath: Rex,
    context: ScribeContext,
): RexStruct {
    val fieldsAsRexOpStructField: List<RexStruct.Field> =
        this.fields.map { field ->
            val newPath =
                RexPathKey.create(
                    prefixPath,
                    RexLit.create(Datum.string(field.name)),
                )
            val newV = field.type.toRexSpark(prefixPath = newPath, context)
            RexStruct.field(
                RexLit.create(Datum.string(field.name)),
                newV,
            )
        }
    return RexStruct.create(
        fieldsAsRexOpStructField,
    )
}
