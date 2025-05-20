package org.partiql.scribe.targets.trino

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql

public open class TrinoAstToSql(context: ScribeContext) : AstToSql(context)
