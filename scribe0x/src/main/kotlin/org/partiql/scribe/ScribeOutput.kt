package org.partiql.scribe

import org.partiql.types.StaticType

/**
 * Result of retargeting.
 *
 * @param T
 * @property tag        An information tag for this transpilation unit.
 * @property value      The transpilation result.
 * @property schema     The output schema as determined by the PartiQL typer.
 */
public abstract class ScribeOutput<T>(
    public val tag: ScribeTag,
    public val value: T,
    public val schema: StaticType,
) {

    abstract override fun toString(): String

    abstract fun toDebugString(): String

}
