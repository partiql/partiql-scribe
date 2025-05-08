package org.partiql.scribe.targets.trino

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RexConverter

public class TrinoRexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : RexConverter(transform, locals, context)
