package org.partiql.scribe.targets.duckdb.utils

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

private fun ScribeContext.logError(msg: String): Nothing =
    this.getProblemListener().reportAndThrow(
        ScribeProblem.simpleError(
            ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
            msg,
        ),
    )

/**
 * Converts this [PType] to a [Rex]. If this [PType] contains the meta "CONTAINS_EXCLUDED_FIELD" meta, additional logic
 * is applied to reconstruct the [PType.ROW] or collection (([PType.ARRAY] or [PType.BAG]) to properly exclude
 * any ROW fields.
 *
 * ROW types are reconstructed as a [RexStruct], which [org.partiql.scribe.targets.duckdb.DuckDBAstToSql] renders as a
 * DuckDB struct literal `{'field': value, ...}` — DuckDB's native struct constructor, which also works inside a
 * `list_transform` lambda (used for collection wildcards).
 */
internal fun PType.toRexDuckDB(
    prefixPath: Rex,
    context: ScribeContext,
): Rex {
    val type = this
    // A RexStruct already renders as a DuckDB struct literal; nothing to reconstruct.
    if (prefixPath is RexStruct) {
        return prefixPath
    }
    if (!this.containsExcludedFieldMeta()) {
        return prefixPath
    }
    return when (type.code()) {
        PType.ROW -> {
            when (type.fields.size) {
                0 -> context.logError("Currently DuckDB does not allow empty ROW/struct values.")
                else -> type.toRexStruct(prefixPath, context)
            }
        }
        PType.ARRAY, PType.BAG -> type.toRexCallTransform(prefixPath, context)
        else -> prefixPath
    }
}

/**
 * Reconstructs the [PType.ROW] as a [RexStruct] whose field keys are the ROW field names and whose values are the
 * (recursively reconstructed) field paths off [prefixPath]. Rendered as a DuckDB struct literal `{'f': v, ...}`.
 *
 * Requires [this] [PType] to be a [PType.ROW].
 */
private fun PType.toRexStruct(
    prefixPath: Rex,
    context: ScribeContext,
): RexStruct {
    val fields =
        this.fields.map { field ->
            val newPath =
                RexPathKey.create(
                    prefixPath,
                    RexLit.create(Datum.string(field.name)),
                )
            val newV =
                field.type.toRexDuckDB(
                    prefixPath = newPath,
                    context = context,
                )
            RexStruct.field(
                RexLit.create(Datum.string(field.name)),
                newV,
            )
        }
    return RexStruct.create(fields)
}

// https://duckdb.org/docs/current/functions/array.html#transform
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
 * Converts the collection [PType] to a [RexCall] representing the DuckDB `transform` function (rendered as
 * `list_transform`).
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
            elementType.toRexDuckDB(
                elementVar,
                context,
            ),
        ),
    )
}
