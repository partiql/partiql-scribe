package org.partiql.scribe.targets.trino

import org.partiql.plan.OperatorRewriter
import org.partiql.scribe.ScribeContext

public class TrinoRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>()
