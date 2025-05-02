package org.partiql.scribe

import org.partiql.spi.types.PType

public abstract class ScribeOutput<T> (
    public val tag: ScribeTag,
    public val value: T,
    public val schema: PType
) {
    abstract override fun toString(): String

    abstract fun toDebugString(): String
}
