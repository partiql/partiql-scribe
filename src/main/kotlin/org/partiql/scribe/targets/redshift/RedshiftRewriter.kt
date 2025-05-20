package org.partiql.scribe.targets.redshift

import org.partiql.plan.OperatorRewriter
import org.partiql.scribe.ScribeContext

public open class RedshiftRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>()
