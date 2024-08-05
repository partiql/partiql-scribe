package org.partiql.scribe.targets.redshift

import org.partiql.plan.Fn
import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpCallStatic
import org.partiql.plan.rexOpCollection
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue

private fun StaticType.shouldExpand() = this.metas["EXPAND"] == true

// Output will be a pair of
// 1. the output paths to KEEP
// 2. the output paths to SET to empty structs
private fun fieldToRedshiftPaths(field: StructType.Field, prefix: String?): Pair<List<String>, List<String>> {
    val fieldKey = field.key
    val fieldValue = field.value
    return if (fieldValue is StructType && fieldValue.shouldExpand()) {
        if (fieldValue.fields.isEmpty()) {
            // Empty struct (all fields excluded). Add the path to both the keepList and setList.
            val setList = if (prefix == null) {
                listOf("\"${fieldKey}\"")
            } else {
                listOf("${prefix}.\"${fieldKey}\"")
            }
            setList to setList
        } else {
            // Non-empty struct. Generate the keep and set path(s) for each field and flatten two one pair of
            // keep list and set list.
            fieldValue.fields.fold(emptyList<String>() to emptyList()) { (keyAcc, setAcc), subField ->
                val newPrefix = if (prefix == null) {
                    "\"${field.key}\""
                } else {
                    "${prefix}.\"${field.key}\""
                }
                val (keepNew, setNew) = fieldToRedshiftPaths(subField, prefix = newPrefix)
                keyAcc + keepNew to setAcc + setNew
            }
        }
    } else {
        // Not a struct OR is a struct with no excluded fields or subfields. Return back the field
        if (prefix == null) {
            listOf("\"${fieldKey}\"") to emptyList()
        } else {
            listOf("${prefix}.\"${fieldKey}\"") to emptyList()
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
private val object_transform_fn_sig = FunctionSignature.Scalar(
    name = "OBJECT_TRANSFORM",
    returns = PartiQLValueType.ANY,
    parameters = listOf(
        FunctionParameter("input", PartiQLValueType.ANY),
        FunctionParameter("keep_list", PartiQLValueType.ANY),
        FunctionParameter("set_list", PartiQLValueType.ANY)
    ),
    isNullable = false,
    isNullCall = true
)

/**
 * Rewrites the [structType] to an `OBJECT_TRANSFORM` function call if the "EXPAND" meta is set to true. Otherwise,
 * returns back [op].
 */
@OptIn(PartiQLValueExperimental::class)
internal fun rewriteToObjectTransform(op: Rex.Op, structType: StructType): Rex.Op {
    return if (structType.shouldExpand()) {
        // Edge case when top-level struct has all fields omitted
        if (structType.fields.isEmpty()) {
            return rexOpStruct(emptyList())
        }
        // https://docs.aws.amazon.com/redshift/latest/dg/r_object_transform_function.html
        // First argument to OBJECT_TRANSFORM is expression that resolves to a SUPER type object (i.e. PartiQL's STRUCT)
        val input = Rex(
            type = structType,
            op = op
        )
        val (keepPaths, setPaths) = structType.fields.fold(emptyList<String>() to emptyList<String>()) { (keepAcc, setAcc), field ->
            val (keepNew, setNew) = fieldToRedshiftPaths(field, prefix = null)
            keepAcc + keepNew to setAcc + setNew
        }
        // Second argument is a Rex.Op.Collection of the paths to keep
        val rexKeepPaths = keepPaths.map { keepPath ->
            rex(
                type = StaticType.STRING,
                op = rexOpLit(stringValue(keepPath))
            )
        }
        // Third argument is a Rex.Op.Collection of the paths we will alter the value.
        // This is of the form `<set path string>, <set path value>`. For now, we only use the `SET` argument to
        // recreate any empty structs in the output set.
        val rexSetPaths = setPaths.flatMap { setPath ->
            listOf(
                // <set path string>
                rex(
                    type = StaticType.STRING,
                    op = rexOpLit(stringValue(setPath))
                ),
                // Interweave empty structs as the <set path value>
                rex(
                    type = StaticType.STRUCT,
                    op = rexOpStruct(emptyList())
                )
            )
        }
        rexOpCallStatic(
            fn = Fn(object_transform_fn_sig),
            args = listOf(
                input,
                rex(
                    type = StaticType.LIST,
                    op = rexOpCollection(rexKeepPaths)
                ),
                rex(
                    type = StaticType.LIST,
                    op = rexOpCollection(rexSetPaths)
                )
            )
        )
    } else {
        op
    }
}

/**
 * Expand path wildcards as described in [RedshiftRewriter].
 *
 * This struct expansion differs from the general [org.partiql.scribe.expandStruct] function to account for nested
 * `EXCLUDE` paths that may omit certain nested fields from structs.
 *
 * Any [StaticType.STRUCT] with the "EXPAND" meta set to true will recreate the struct using the `OBJECT_TRANSFORM`
 * in Redshift with the explicit struct fields to keep.
 *
 * Any other type or [StaticType.STRUCT]s without the "EXPAND" meta set to true will recreate the struct using the same
 * struct expansion used in [org.partiql.scribe.expandStruct].
 */
@OptIn(PartiQLValueExperimental::class)
internal fun expandStruct(op: Rex.Op, structType: StructType): List<Rex> {
    return structType.fields.map { topLevelField ->
        val pathOp = rexOpPathKey(
            root = Rex(
                type = topLevelField.value,
                op = op
            ),
            key = rex(StaticType.STRING, rexOpLit(stringValue(topLevelField.key)))
        )
        val fieldValue = topLevelField.value
        // Create using OBJECT_TRANSFORM since the struct has excluded fields and/or nested excluded fields
        if (fieldValue is StructType && fieldValue.shouldExpand()) {
            val newOp = rewriteToObjectTransform(pathOp, fieldValue)
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
                                op = newOp
                            )
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
