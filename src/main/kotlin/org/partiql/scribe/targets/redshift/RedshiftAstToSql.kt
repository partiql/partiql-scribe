package org.partiql.scribe.targets.redshift

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql

public open class RedshiftAstToSql(context: ScribeContext) : AstToSql(context)
