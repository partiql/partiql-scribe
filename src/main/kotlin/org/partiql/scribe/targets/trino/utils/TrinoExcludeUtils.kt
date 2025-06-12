package org.partiql.scribe.targets.trino.utils

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.scribe.sql.utils.containsExcludedFieldMeta
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal const val TRANSFORM_VAR = "___coll_wildcard___"

internal fun PType.toRexTrino(prefixPath: Rex): Rex {
    val type = this
    if (!type.containsExcludedFieldMeta()) {
        return prefixPath
    }
    return when (type.code()) {
        PType.ROW -> {
            when (type.fields.size) {
                0 -> error("Currently Trino does not allow empty ROWs. Consider `EXCLUDE` on the outer struct")
                else -> type.toRexCastRow(prefixPath)
            }
        }
        PType.ARRAY, PType.BAG -> type.toRexCallTransform(prefixPath)
        else -> prefixPath
    }
}

private val cast_row_fn_sig =
    Fn.Builder("cast_row")
        .addParameters(
            Parameter("cast_value", PType.dynamic()),
            Parameter("as_type", PType.dynamic()),
        )
        .returns(PType.dynamic())
        .build()

// Use for reconstructing Trino `ROW`s. Simplified `ROW` construction syntax to not specify types using `SELECT`
// (https://github.com/trinodb/trino/discussions/7758) does not work for certain nested cases (e.g. within a
// lambda).
private fun PType.toRexCastRow(prefixPath: Rex): RexCall {
    val rowValues =
        this.fields.map { field ->
            val newPath =
                RexPathKey.create(
                    prefixPath,
                    RexLit.create(Datum.string(field.name)),
                )
            val newV =
                field.type.toRexTrino(
                    prefixPath = newPath,
                )
            newV
        }
    val castType = RexLit.create(Datum.string(this.toTrinoString()))
    return RexCall.create(
        cast_row_fn_sig,
        listOf(RexArray.create(rowValues), castType),
    )
}

// https://trino.io/docs/current/functions/array.html#transform
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
            elementType.toRexTrino(
                elementVar,
            ),
        ),
    )
}

private fun PType.toTrinoString(): String {
    val type = this
    return when (type.code()) {
        PType.TINYINT -> "TINYINT"
        PType.SMALLINT -> "SMALLINT"
        PType.INTEGER -> "INTEGER"
        PType.BIGINT -> "BIGINT"
        PType.STRING -> "VARCHAR"
        PType.VARCHAR -> {
            val length = type.length
            if (length < 0) {
                error("VARCHAR length must be non-negative $length")
            } else {
                "VARCHAR($length)"
            }
        }
        PType.CHAR -> {
            val length = type.length
            if (length < 0) {
                error("CHAR length must be non-negative $length")
            } else {
                "CHAR($length)"
            }
        }
        PType.ROW -> {
            val head = "ROW("
            val fieldsAsString =
                this.fields.foldIndexed("") { index, acc, field ->
                    // wrap `field.key` in double-quotes since the field name could be a reserved keyword
                    val fieldStr = acc + "\"${field.name}\"" + " " + field.type.toTrinoString()
                    if (index < fields.size - 1) {
                        "$fieldStr, "
                    } else {
                        fieldStr
                    }
                }
            "$head$fieldsAsString)"
        }
        PType.BOOL -> "BOOLEAN"
        PType.DECIMAL -> {
            val precision = type.precision
            val scale = type.scale
            if (precision !in 1..38) {
                error("DECIMAL precision not in range [1, 38]: $precision")
            } else if (scale !in 0..precision) {
                error("DECIMAL scale not in range [0, $precision]: $scale")
            } else {
                "DECIMAL($precision, $scale)"
            }
        }
        PType.TIMESTAMP -> {
            when (type.metas["UNSPECIFIED_PRECISION"] == true) {
                true -> "TIMESTAMP"
                false -> {
                    if (precision !in 0..12) {
                        error("TIMESTAMP precision not in range [0, 12]: $precision")
                    } else {
                        "TIMESTAMP($precision)"
                    }
                }
            }
        }
        PType.DATE -> "DATE"
        PType.TIME -> {
            when (type.metas["UNSPECIFIED_PRECISION"] == true) {
                true -> "TIME"
                false -> {
                    if (precision !in 0..12) {
                        error("TIME precision not in range [0, 12]: $precision")
                    } else {
                        "TIME($precision)"
                    }
                }
            }
        }
        PType.TIMESTAMPZ, PType.TIMEZ -> TODO()
        PType.REAL -> "REAL"
        PType.DOUBLE -> "DOUBLE"
        PType.UNKNOWN -> "NULL"
        PType.ARRAY, PType.BAG -> {
            val head = "ARRAY<"
            val elementType = type.typeParameter.toTrinoString()
            "$head$elementType>"
        }
        else -> TODO("Not able to convert StaticType $this to Trino")
    }
}
