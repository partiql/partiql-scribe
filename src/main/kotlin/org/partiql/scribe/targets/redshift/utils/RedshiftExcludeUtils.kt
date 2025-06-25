package org.partiql.scribe.targets.redshift.utils

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexType
import org.partiql.scribe.sql.utils.containsExcludedFieldMeta
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum

/**
 * Second and third arguments of Redshift's OBJECT_TRANSFORM are the "paths" to keep and set in the output SUPER object.
 * These "paths" must are string literals with double-quoted path componenets delimited by periods.
 *
 * E.g. '"a"."b"."c"'
 *
 * Output will be a pair of
 * 1. the output paths to KEEP
 * 2. the output paths to SET to empty rows
 */
private fun fieldToRedshiftPaths(
    field: PTypeField,
    prefix: String?,
): Pair<List<String>, List<String>> {
    val fieldKey = field.name
    val fieldValue = field.type
    return if (fieldValue.code() == PType.ROW && fieldValue.containsExcludedFieldMeta()) {
        if (fieldValue.fields.isEmpty()) {
            // Empty row (all fields excluded). Add the path to both the keepList and setList.
            val setList =
                if (prefix == null) {
                    listOf("\"${fieldKey}\"")
                } else {
                    listOf("$prefix.\"${fieldKey}\"")
                }
            setList to setList
        } else {
            // Non-empty rows. Generate the keep and set path(s) for each field and flatten two one pair of
            // keep list and set list.
            fieldValue.fields.fold(emptyList<String>() to emptyList()) { (keyAcc, setAcc), subField ->
                val newPrefix =
                    if (prefix == null) {
                        "\"${fieldKey}\""
                    } else {
                        "$prefix.\"${fieldKey}\""
                    }
                val (keepNew, setNew) = fieldToRedshiftPaths(subField, prefix = newPrefix)
                keyAcc + keepNew to setAcc + setNew
            }
        }
    } else {
        // Not a row OR is a row with no excluded fields or subfields. Return back the field
        if (prefix == null) {
            listOf("\"${fieldKey}\"") to emptyList()
        } else {
            listOf("$prefix.\"${fieldKey}\"") to emptyList()
        }
    }
}

/**
 * Rewrites the [rex] to an `OBJECT_TRANSFORM` function call if the "CONTAINS_EXCLUDED_FIELD" meta is set to true.
 * Otherwise, returns back [rex].
 *
 * Requires the [PType] of the [Rex] to have code [PType.ROW].
 */
internal fun rewriteToObjectTransform(rex: Rex): Rex {
    val rowType = rex.type.pType
    return if (rowType.containsExcludedFieldMeta()) {
        // Edge case when top-level row has all fields omitted. Return back an empty `RexStruct` since Redshift's
        // OBJECT_TRANSFORM must output at least one field.
        if (rowType.fields.isEmpty()) {
            return RexStruct.create(emptyList())
        }
        // https://docs.aws.amazon.com/redshift/latest/dg/r_object_transform_function.html
        // First argument to OBJECT_TRANSFORM is expression that resolves to a SUPER type object (i.e. PartiQL's ROW)
        val rexInput = rex
        val (keepPaths, setPaths) =
            rowType.fields.fold(emptyList<String>() to emptyList<String>()) { (keepAcc, setAcc), field ->
                val (keepNew, setNew) = fieldToRedshiftPaths(field, prefix = null)
                keepAcc + keepNew to setAcc + setNew
            }
        // Second argument is a RexArray of the paths to keep
        val rexKeepPaths =
            RexArray.create(
                keepPaths.map { keepPath ->
                    val path = RexLit.create(Datum.string(keepPath))
                    path.type = RexType.of(PType.string())
                    path
                },
            )
        rexKeepPaths.type = RexType.of(PType.array())
        // Third argument is a RexArray of the paths we will alter the value.
        // This is of the form `<set path string>, <set path value>`. For now, we only use the `SET` argument to
        // recreate any empty rows in the output set.
        val rexSetPaths =
            RexArray.create(
                setPaths.flatMap { setPath ->
                    val rexSetPath = RexLit.create(Datum.string(setPath))
                    rexSetPath.type = RexType.of(PType.string())

                    val rexEmptyRow = RexStruct.create(emptyList())
                    rexEmptyRow.type = RexType.of(PType.row())
                    listOf(
                        // <set path string>
                        rexSetPath,
                        // Interweave empty rows as the <set path value>
                        rexEmptyRow,
                    )
                },
            )
        rexSetPaths.type = RexType.of(PType.array())
        // Create the `OBJECT_TRANSFORM` call with the arguments
        RexCall.create(
            Fn.Builder("OBJECT_TRANSFORM")
                .addParameters(
                    Parameter("input", rexInput.type.pType),
                    Parameter("keep_list", PType.array()),
                    Parameter("set_list", PType.array()),
                )
                .returns(rexInput.type.pType)
                .build(),
            listOf(
                rexInput,
                rexKeepPaths,
                rexSetPaths,
            ),
        )
    } else {
        rex
    }
}
