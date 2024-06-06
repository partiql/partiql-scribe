package org.partiql.scribe

import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathKey
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
internal fun expandStruct(op: Rex.Op, structType: StructType): List<Rex> {
    return structType.fields.map { field ->
        // Create a new struct for each field
        Rex(
            type = StructType(
                fields = listOf(
                    StructType.Field(
                        key = field.key,
                        value = field.value
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
                                    field.key
                                )
                            )
                        ),
                        v = Rex(
                            type = field.value,
                            op = rexOpPathKey(
                                root = Rex(
                                    type = field.value,
                                    op = op
                                ),
                                key = rex(StaticType.STRING, rexOpLit(stringValue(field.key)))
                            )
                        )
                    )
                )
            )
        )
    }
}
