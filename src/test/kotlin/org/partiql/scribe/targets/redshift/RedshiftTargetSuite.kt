package org.partiql.scribe.targets.redshift

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class RedshiftTargetSuite : SqlTargetSuite() {
    override val target = RedshiftTarget.STANDARD

    override val root = this::class.java.getResource("/outputs/redshift")!!.toURI().toPath()

    override val sessions =
        SessionProvider(
            scalarOverloads =
                mapOf(
                    "split" to Functions.scalarSplit,
                ),
        )
}
