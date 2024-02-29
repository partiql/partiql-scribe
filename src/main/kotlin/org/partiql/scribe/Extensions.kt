package org.partiql.scribe

import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.NULL
import org.partiql.types.StaticType.Companion.unionOf

/**
 *  Returns a nullable version of the current [StaticType].
 *
 *  If it already nullable, returns the original type.
 */
public fun StaticType.asNullable(): StaticType =
    when {
        this.isNullable() -> this
        else -> unionOf(this, NULL).flatten()
    }

public fun StaticType.asMissable(): StaticType = unionOf(this, MissingType).flatten()

public fun StaticType.asAbsent(): StaticType = unionOf(this, NULL, MissingType).flatten()

/**
 *  Returns a non-nullable version of the current [StaticType].
 *
 *  If it already non-nullable, returns the original type.
 */
public fun StaticType.asNonNullable(): StaticType = when (this.isNullable()) {
    false -> this
    true -> unionOf(this.allTypes.filter { it !is NullType }.toSet()).flatten()
}
