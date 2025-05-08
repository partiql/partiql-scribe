package org.partiql.scribe.targets.spark

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.RelConverter

public class SparkRelConverter(transform: SparkPlanToAst, context: ScribeContext) : RelConverter(transform, context)
