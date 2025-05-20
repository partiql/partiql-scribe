package org.partiql.scribe.sql

import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.ScribeContext

/**
 * Base class for AST to SQL translators.
 */
public abstract class AstToSql(public val context: ScribeContext) : SqlDialect()
