package org.partiql.scribe.targets.redshift

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.RelConverter

public open class RedshiftRelConverter(transform: RedshiftPlanToAst, context: ScribeContext) : RelConverter(transform, context)
