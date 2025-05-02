package org.partiql.scribe.sql

import org.partiql.scribe.ScribeOutput
import org.partiql.scribe.ScribeTag
import org.partiql.spi.types.PType

public class SqlOutput(
    tag: ScribeTag,
    value: String,
    schema: PType,

    ) : ScribeOutput<String>(tag, value, schema) {
    override fun toString(): String = value

    override fun toDebugString(): String = buildString {
        appendLine("SQL: ")
        appendLine(value)
        appendLine()
        appendLine("Schema: ")
        appendLine(schema)
    }
}