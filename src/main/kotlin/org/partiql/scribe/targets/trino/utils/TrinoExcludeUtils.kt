package org.partiql.scribe.targets.trino.utils

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.utils.containsExcludedFieldMeta
import org.partiql.spi.function.Fn
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

private const val UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH"
private const val UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION"
private const val UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE"

private fun PType.unspecifiedLength() = metas[UNSPECIFIED_LENGTH] == true

private fun PType.unspecifiedPrecision() = metas[UNSPECIFIED_PRECISION] == true

private fun PType.unspecifiedScale() = metas[UNSPECIFIED_SCALE] == true

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
 */
internal fun PType.toRexTrino(
    prefixPath: Rex,
    context: ScribeContext,
): Rex {
    val type = this
    if (!type.containsExcludedFieldMeta()) {
        return prefixPath
    }
    return when (type.code()) {
        PType.ROW -> {
            when (type.fields.size) {
                0 -> context.logError("Currently Trino does not allow empty ROWs. Consider `EXCLUDE` on the outer struct")
                else -> type.toRexCastRow(prefixPath, context)
            }
        }
        PType.ARRAY, PType.BAG -> type.toRexCallTransform(prefixPath, context)
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

/**
 * Use for reconstructing Trino `ROW`s. Simplified `ROW` construction syntax to not specify types using `SELECT`
 * (https://github.com/trinodb/trino/discussions/7758) does not work for certain nested cases (e.g. within a
 * lambda).
 *
 * Requires [this] [PType] to be a [PType.ROW].
 */
private fun PType.toRexCastRow(
    prefixPath: Rex,
    context: ScribeContext,
): RexCall {
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
                    context = context,
                )
            newV
        }
    val castType = RexLit.create(Datum.string(this.toTrinoString(context)))
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
 * Converts the [PType.ROW] to a [RexCall] representing the Trino `transform` function.
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
            elementType.toRexTrino(
                elementVar,
                context,
            ),
        ),
    )
}

/**
 * Returns the Trino string representation of the given [PType].
 *
 * TODO: there is a lot of replicated logic here and in TrinoAstToSql and AstToSql. We should look at a better way to
 *  share this type conversion logic.
 */
private fun PType.toTrinoString(context: ScribeContext): String {
    val type = this
    return when (type.code()) {
        PType.TINYINT -> "TINYINT"
        PType.SMALLINT -> "SMALLINT"
        PType.INTEGER -> "INTEGER"
        PType.BIGINT -> "BIGINT"
        PType.STRING -> "VARCHAR"
        PType.VARCHAR -> {
            when (type.unspecifiedLength()) {
                true -> "VARCHAR"
                false -> {
                    if (length < 0) {
                        context.logError("VARCHAR length must be non-negative $length")
                    } else {
                        "VARCHAR($length)"
                    }
                }
            }
        }
        PType.CHAR -> {
            when (type.unspecifiedLength()) {
                true -> "CHAR"
                false -> {
                    if (length < 0) {
                        context.logError("CHAR length must be non-negative $length")
                    } else {
                        "CHAR($length)"
                    }
                }
            }
        }
        PType.ROW -> {
            val head = "ROW("
            val fieldsAsString =
                this.fields.foldIndexed("") { index, acc, field ->
                    // wrap `field.key` in double-quotes since the field name could be a reserved keyword
                    val fieldStr = acc + "\"${field.name}\"" + " " + field.type.toTrinoString(context)
                    if (index < fields.size - 1) {
                        "$fieldStr, "
                    } else {
                        fieldStr
                    }
                }
            "$head$fieldsAsString)"
        }
        PType.BOOL -> "BOOLEAN"
        PType.DECIMAL, PType.NUMERIC -> {
            val noPrecision = type.unspecifiedPrecision()
            val noScale = type.unspecifiedScale()
            when {
                noPrecision && noScale -> "DECIMAL"
                noScale -> {
                    if (precision !in 1..38) {
                        context.logError("DECIMAL precision not in range [1, 38]: $precision")
                    } else {
                        "DECIMAL($precision)"
                    }
                }
                noPrecision -> context.logError("DECIMAL scale specified without precision")
                else -> {
                    val precision = type.precision
                    val scale = type.scale
                    if (precision !in 1..38) {
                        context.logError("DECIMAL precision not in range [1, 38]: $precision")
                    } else if (scale !in 0..precision) {
                        context.logError("DECIMAL scale not in range [0, $precision]: $scale")
                    } else {
                        "DECIMAL($precision, $scale)"
                    }
                }
            }
        }
        PType.DATE -> "DATE"
        PType.TIME -> {
            when (type.unspecifiedPrecision()) {
                true -> "TIME"
                false -> {
                    if (precision !in 0..12) {
                        context.logError("TIME precision not in range [0, 12]: $precision")
                    } else {
                        "TIME($precision)"
                    }
                }
            }
        }
        PType.TIMEZ -> {
            when (type.unspecifiedPrecision()) {
                true -> "TIME WITH TIME ZONE"
                false -> {
                    if (precision !in 0..12) {
                        context.logError("TIME WITH TIME ZONE precision not in range [0, 12]: $precision")
                    } else {
                        "TIME($precision) WITH TIME ZONE"
                    }
                }
            }
        }
        PType.TIMESTAMP -> {
            when (type.unspecifiedPrecision()) {
                true -> "TIMESTAMP"
                false -> {
                    if (precision !in 0..12) {
                        context.logError("TIMESTAMP precision not in range [0, 12]: $precision")
                    } else {
                        "TIMESTAMP($precision)"
                    }
                }
            }
        }
        PType.TIMESTAMPZ -> {
            when (type.unspecifiedPrecision()) {
                true -> "TIMESTAMP WITH TIME ZONE"
                false -> {
                    if (precision !in 0..12) {
                        context.logError("TIMESTAMP WITH TIME ZONE precision not in range [0, 12]: $precision")
                    } else {
                        "TIMESTAMP($precision) WITH TIME ZONE"
                    }
                }
            }
        }
        PType.REAL -> "REAL"
        PType.DOUBLE -> "DOUBLE"
        PType.UNKNOWN -> "NULL"
        PType.ARRAY, PType.BAG -> {
            val head = "ARRAY<"
            val elementType = type.typeParameter.toTrinoString(context)
            "$head$elementType>"
        }
        else -> context.logError("Not able to convert PType $this to Trino")
    }
}
