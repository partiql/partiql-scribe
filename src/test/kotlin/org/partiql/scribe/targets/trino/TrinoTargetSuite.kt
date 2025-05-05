package org.partiql.scribe.targets.trino

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class TrinoTargetSuite : SqlTargetSuite() {
    override val target = TrinoTarget()

    override val root = this::class.java.getResource("/outputs/trinio")!!.toURI().toPath()

    override val sessions =
        SessionProvider(
            scalarOverloads =
                mapOf(
                    "split" to Functions.scalarSplit,
                ),
        )
}
