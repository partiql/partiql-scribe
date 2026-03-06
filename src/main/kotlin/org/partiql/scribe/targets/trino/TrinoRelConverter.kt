package org.partiql.scribe.targets.trino

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter

public open class TrinoRelConverter(transform: TrinoPlanToAst, context: ScribeContext, outer: List<Locals> = emptyList()) : RelConverter(
    transform,
    context,
    outer,
)
