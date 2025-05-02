package org.partiql.scribe.targets.redshift

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.PlanToAst

public class RedshiftRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext
) : RexConverter(transform, locals, context) {
}
