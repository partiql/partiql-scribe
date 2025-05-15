package org.partiql.scribe.targets.partiql

import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.scribe.sql.SqlTarget

/**
 * Default PartiQL Target does nothing as there is no need to rewrite the plan.
 */
public object PartiQLTarget : SqlTarget() {
    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override val features: SqlFeatures = SqlFeatures.Permissive()

    override fun rewrite(
        plan: Plan,
        context: ScribeContext,
    ): Plan = plan
}
