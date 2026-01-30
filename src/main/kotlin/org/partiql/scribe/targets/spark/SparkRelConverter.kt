package org.partiql.scribe.targets.spark

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter

public open class SparkRelConverter(transform: SparkPlanToAst, context: ScribeContext, outer: Locals? = null) : RelConverter(
    transform,
    context,
    outer,
)
