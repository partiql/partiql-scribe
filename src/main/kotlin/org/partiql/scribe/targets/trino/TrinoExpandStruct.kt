package org.partiql.scribe.targets.trino

import org.partiql.plan.Fn
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpCollection
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.scribe.asNonAbsent
import org.partiql.types.AnyOfType
import org.partiql.types.BoolType
import org.partiql.types.CollectionType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLTimestampExperimental
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

private fun StaticType.expand() = this.metas["EXPAND"] == true

internal fun StaticType.toRexTrino(prefixPath: Rex): Rex {
    if (!this.expand()) {
        return prefixPath
    }
    return when (val nonNullType = this.asNonAbsent()) {
        is StructType -> {
            Rex(
                type = nonNullType,
                op = when (nonNullType.fields.size) {
                    0 -> error("Currently Trino does not allow empty ROWs. Consider `EXCLUDE` on the outer struct")
                    else -> nonNullType.toRexCastRow(prefixPath)
                },
            )
        }
        is CollectionType -> Rex(
            type = nonNullType,
            op = nonNullType.toRexCallTransform(prefixPath),
        )
        else -> prefixPath
    }
}

@OptIn(PartiQLValueExperimental::class)
private val cast_row_fn_sig = FunctionSignature.Scalar(
    name = "cast_row",
    returns = PartiQLValueType.ANY,
    parameters = listOf(
        FunctionParameter("cast_value", PartiQLValueType.ANY),
        FunctionParameter("as_type", PartiQLValueType.ANY)
    ),
    isNullable = false,
    isNullCall = true
)

// Use for reconstructing Trino `ROW`s. Simplified `ROW` construction syntax to not specify types using `SELECT`
// (https://github.com/trinodb/trino/discussions/7758) does not work for certain nested cases (e.g. within a
// lambda).
@OptIn(PartiQLValueExperimental::class)
private fun StructType.toRexCastRow(prefixPath: Rex): Rex.Op.Call.Static {
    val rowValues = this.fields.map { field ->
        val newPath = rexOpPathKey(
            prefixPath,
            rex(StaticType.STRING, rexOpLit(stringValue(field.key)))
        )
        val newV = field.value.toRexTrino(
            prefixPath = Rex(
                type = field.value,
                op = newPath
            )
        )
        newV
    }
    val castType = Rex(
        type = StaticType.STRING,
        op = rexOpLit(stringValue(this.toTrinoString()))
    )
    return rexOpCallStatic(
        fn = Fn(cast_row_fn_sig),
        args = listOf(
            Rex(
                type = StaticType.LIST,
                op = rexOpCollection(rowValues)
            ),
            castType
        )
    )
}

// https://trino.io/docs/current/functions/array.html#transform
@OptIn(PartiQLValueExperimental::class)
private val transform_fn_sig = FunctionSignature.Scalar(
    name = "transform",
    returns = PartiQLValueType.ANY,
    parameters = listOf(
        FunctionParameter("array_expr", PartiQLValueType.ANY),
        FunctionParameter("element_var", PartiQLValueType.ANY),
        FunctionParameter("element_expr", PartiQLValueType.ANY)
    ),
    isNullable = false,
    isNullCall = true
)

@OptIn(PartiQLValueExperimental::class)
private fun CollectionType.toRexCallTransform(prefixPath: Rex): Rex.Op.Call.Static {
    val elementType = this.elementType
    val elementVar = Rex(
        type = StaticType.ANY,
        op = rexOpLit(
            value = symbolValue("___coll_wildcard___")
        )
    )
    return rexOpCallStatic(
        fn = Fn(transform_fn_sig),
        args = listOf(
            prefixPath,
            elementVar,
            elementType.toRexTrino(
                elementVar
            )
        )
    )
}

@OptIn(PartiQLTimestampExperimental::class)
private fun StaticType.toTrinoString(): String {
    return when (this) {
        is IntType -> {
            when (rangeConstraint) {
                // PartiQL IntType does not support 8-bit INT (Trino's TINYINT)
                IntType.IntRangeConstraint.SHORT -> "SMALLINT"
                IntType.IntRangeConstraint.INT4 -> "INTEGER"
                IntType.IntRangeConstraint.LONG -> "BIGINT"
                IntType.IntRangeConstraint.UNCONSTRAINED -> kotlin.error("Unconstrained int not supported in Trino")
            }
        }
        is StringType -> {
            when (val constraint = lengthConstraint) {
                StringType.StringLengthConstraint.Unconstrained -> "VARCHAR"
                is StringType.StringLengthConstraint.Constrained -> {
                    when (val numConstraint = constraint.length) {
                        is NumberConstraint.Equals -> {
                            if (numConstraint.value < 0) {
                                kotlin.error("CHAR length must be non-negative ${numConstraint.value}")
                            } else {
                                "CHAR(${numConstraint.value})"
                            }
                        }
                        is NumberConstraint.UpTo -> {
                            if (numConstraint.value < 0) {
                                kotlin.error("VARCHAR length must be non-negative ${numConstraint.value}")
                            } else {
                                "VARCHAR(${numConstraint.value})"
                            }
                        }
                    }
                }
            }
        }
        is StructType -> {
            val head = "ROW("
            val fieldsAsString = this.fields.foldIndexed("") { index, acc, field ->
                val fieldStr = acc + field.key + " " + field.value.toTrinoString()
                if (index < fields.size - 1) {
                    "$fieldStr, "
                } else {
                    fieldStr
                }
            }
            head + fieldsAsString + ")"
        }
        is BoolType -> "BOOLEAN"
        is DecimalType -> {
            when (val constraint = precisionScaleConstraint) {
                DecimalType.PrecisionScaleConstraint.Unconstrained -> kotlin.error("Unconstrained decimal not supported in Trino")
                is DecimalType.PrecisionScaleConstraint.Constrained -> {
                    if (constraint.precision !in 1..38) {
                        kotlin.error("DECIMAL precision not in range [1, 38]: ${constraint.precision}")
                    } else if (constraint.scale !in 0..constraint.precision) {
                        kotlin.error("DECIMAL scale not in range [0, ${constraint.precision}]: ${constraint.scale}")
                    } else {
                        "DECIMAL(${constraint.precision}, ${constraint.scale})"
                    }
                }
            }
        }
        is TimestampType -> {
            when (precision) {
                null -> "TIMESTAMP"
                else -> {
                    if (precision!! !in 0..12) {
                        kotlin.error("TIMESTAMP precision not in range [0, 12]: $precision")
                    } else {
                        "TIMESTAMP($precision)"
                    }
                }
            }
        }
        is DateType -> "DATE"
        is TimeType -> {
            when (precision) {
                null -> "TIME"
                else -> {
                    if (precision!! !in 0..12) {
                        kotlin.error("TIME precision not in range [0, 12]: $precision")
                    } else {
                        "TIME($precision)"
                    }
                }
            }
        }
        is FloatType -> "DOUBLE"    // PartiQL FloatType does not have a constraint to differentiate between 32 and 64-bit floats. For now, mapping to DOUBLE.
        is NullType -> "NULL"
        is ListType -> {
            val head = "ARRAY<"
            val elementType = elementType.toTrinoString()
            "$head$elementType>"
        }
        is AnyOfType -> {
            // Filter out unknown types. Trino types are nullable by default. Once Scribe uses PLK 0.15+,
            // `StaticType`s will be nullable + missable by default, so this branch will not be needed.
            val typesWithoutUnknown = this.flatten().allTypes.filterNot { it is NullType || it is MissingType }
            if (typesWithoutUnknown.size != 1) {
                error("Not able to convert StaticType $this to Trino")
            }
            val singleType = typesWithoutUnknown.first()
            singleType.toTrinoString()
        }
        else -> TODO("Not able to convert StaticType $this to Trino")
    }
}


/**
 * Expand path wildcards as described in [TrinoRewriter].
 *
 * Any [StaticType] with the "EXPAND" meta set to true will recreate any
 * - structs using the `CAST(ROW(...) AS ROW(...))` function
 * - arrays using the `transform` function
 * in Trino by recursing into the structs/arrays, explicitly specifying the values to keep.
 *
 * Any types without the "EXPAND" meta set to true will recreate the struct without recursing into any nested values.
 */
@OptIn(PartiQLValueExperimental::class)
internal fun expandStructTrino(op: Rex.Op, structType: StructType): List<Rex> {
    return structType.fields.map { topLevelField ->
        val pathOp = rexOpPathKey(
            root = Rex(
                type = topLevelField.value,
                op = op
            ),
            key = rex(StaticType.STRING, rexOpLit(stringValue(topLevelField.key)))
        )
        val fieldValue = topLevelField.value.asNonAbsent()
        if (fieldValue.expand()) {
            val newOp = fieldValue.toRexTrino(
                prefixPath = Rex(
                    type = fieldValue,
                    op = pathOp
                )
            )
            Rex(
                type = StructType(
                    fields = listOf(
                        StructType.Field(
                            key = topLevelField.key,
                            value = topLevelField.value
                        )
                    )
                ),
                op = rexOpStruct(
                    fields = listOf(
                        rexOpStructField(
                            k = Rex(
                                type = StaticType.STRING,
                                op = rexOpLit(
                                    stringValue(
                                        topLevelField.key
                                    )
                                )
                            ),
                            v = newOp
                        )
                    )
                )
            )
        } else {
            // Otherwise, create using default struct expansion
            Rex(
                type = StructType(
                    fields = listOf(
                        StructType.Field(
                            key = topLevelField.key,
                            value = topLevelField.value
                        )
                    )
                ),
                op = rexOpStruct(
                    fields = listOf(
                        rexOpStructField(
                            k = Rex(
                                type = StaticType.STRING,
                                op = rexOpLit(
                                    stringValue(
                                        topLevelField.key
                                    )
                                )
                            ),
                            v = Rex(
                                type = topLevelField.value,
                                op = pathOp
                            )
                        )
                    )
                )
            )
        }
    }
}
