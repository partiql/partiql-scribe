package org.partiql.scribe.targets.spark

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql

public open class SparkAstToSql(context: ScribeContext) : AstToSql(context)
