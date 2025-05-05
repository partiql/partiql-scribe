package org.partiql.scribe.targets.spark

import org.partiql.plan.OperatorRewriter
import org.partiql.scribe.ScribeContext

public class SparkRewriter(context: ScribeContext) : OperatorRewriter<ScribeContext>()
