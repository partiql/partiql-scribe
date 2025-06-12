package org.partiql.scribe.targets.spark.utils

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexStruct
import org.partiql.scribe.sql.utils.containsExcludedFieldMeta
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal const val TRANSFORM_VAR = "___coll_wildcard___"

internal fun PType.toRexSpark(prefixPath: Rex): Rex {
    val type = this
    if (!type.containsExcludedFieldMeta()) {
        return prefixPath
    }
    return when (type.code()) {
        PType.ROW ->
            when (type.fields.size) {
                0 -> error("Currently do not support outputting empty structs in Spark. Consider `EXCLUDE` on the outer struct.")
                else -> type.toRexStruct(prefixPath)
            }
        PType.ARRAY, PType.BAG -> type.toRexCallTransform(prefixPath)
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
 *
 * Requires [this] [PType] to be a [PType.ARRAY] or [PType.BAG].
 */
private fun PType.toRexCallTransform(prefixPath: Rex): RexCall {
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
            ),
        ),
    )
}

private fun PType.toRexStruct(prefixPath: Rex): RexStruct {
    val fieldsAsRexOpStructField: List<RexStruct.Field> =
        this.fields.map { field ->
            val newPath =
                RexPathKey.create(
                    prefixPath,
                    RexLit.create(Datum.string(field.name)),
                )
            val newV = field.type.toRexSpark(prefixPath = newPath)
            RexStruct.field(
                RexLit.create(Datum.string(field.name)),
                newV,
            )
        }
    return RexStruct.create(
        fieldsAsRexOpStructField,
    )
}
