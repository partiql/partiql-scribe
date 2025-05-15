package org.partiql.scribe.targets.spark

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter

public class SparkRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context)
