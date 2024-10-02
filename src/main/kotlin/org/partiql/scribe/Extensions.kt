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
 *  Returns a non-null, non-missing version of the current [StaticType].
 *
 *  Note: this extension function will not be needed post PLK 0.15+ with the deprecation of null and missing types.
 */
public fun StaticType.asNonAbsent(): StaticType =
    unionOf(this.allTypes.filter { it !is NullType && it !is MissingType }.toSet()).flatten()
