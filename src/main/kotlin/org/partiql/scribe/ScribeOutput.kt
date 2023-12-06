package org.partiql.scribe

import org.partiql.types.StaticType

/**
 * Result of retargeting.
 *
 * @param T
 * @property schema
 * @property value
 */
public abstract class ScribeOutput<T>(
    public val schema: StaticType,
    public val value: T,
) {

    abstract override fun toString(): String

    abstract fun toDebugString(): String
}
