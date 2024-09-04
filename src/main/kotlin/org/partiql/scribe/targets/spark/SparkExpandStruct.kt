package org.partiql.scribe.targets.spark

import org.partiql.plan.Fn
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.scribe.asNonNullable
import org.partiql.types.CollectionType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

private fun StaticType.expand() = this.metas["EXPAND"] == true

internal fun StaticType.toRexSpark(prefixPath: Rex): Rex {
    if (!this.expand()) {
        return prefixPath
    }
    return when (val nonNullType = this.asNonNullable()) {
        is StructType -> Rex(
            type = nonNullType,
            op = when (nonNullType.fields.size) {
                0 -> error("Currently do not support outputting empty structs in Spark. Consider `EXCLUDE` on the outer struct.")
                else -> nonNullType.toRexStruct(prefixPath)
            }
        )
        is CollectionType -> Rex(
            type = nonNullType,
            op = nonNullType.toRexCallTransform(prefixPath),
        )
        else -> prefixPath
    }
}

// https://spark.apache.org/docs/latest/api/sql/index.html#transform
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
            elementType.toRexSpark(
                elementVar
            )
        )
    )
}

@OptIn(PartiQLValueExperimental::class)
private fun StructType.toRexStruct(prefixPath: Rex): Rex.Op.Struct {
    val fieldsAsRexOpStructField: List<Rex.Op.Struct.Field> = this.fields.map { field ->
        val newPath = rexOpPathKey(
            prefixPath,
            rex(StaticType.STRING, rexOpLit(stringValue(field.key)))
        )
        val newV = field.value.toRexSpark(
            prefixPath = Rex(
                type = field.value,
                op = newPath
            )
        )
        rexOpStructField(
            k = Rex(
                type = StaticType.STRING,
                op = rexOpLit(stringValue(field.key))
            ),
            v = newV
        )
    }
    return rexOpStruct(
        fields = fieldsAsRexOpStructField
    )
}


/**
 * Expand path wildcards as described in [SparkRewriter].
 *
 * Any [StaticType] with the "EXPAND" meta set to true will recreate any
 * - structs using the `STRUCT` function
 * - arrays using the `transform` function
 * in Spark by recursing into the structs/arrays, explicitly specifying the values to keep.
 *
 * Any types without the "EXPAND" meta set to true will recreate the struct without recursing into any nested values.
 */
@OptIn(PartiQLValueExperimental::class)
internal fun expandStructSpark(op: Rex.Op, structType: StructType): List<Rex> {
    return structType.fields.map { topLevelField ->
        val pathOp = rexOpPathKey(
            root = Rex(
                type = topLevelField.value,
                op = op
            ),
            key = rex(StaticType.STRING, rexOpLit(stringValue(topLevelField.key)))
        )
        val fieldValue = topLevelField.value.asNonNullable()
        if (fieldValue.expand()) {
            val newOp = fieldValue.toRexSpark(
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
