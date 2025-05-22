@file:Suppress("ktlint:standard:no-empty-file")

package org.partiql.scribe.targets.trino

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class TrinoTargetSuite : SqlTargetSuite() {
    override val target = TrinoTarget()

    override val root = this::class.java.getResource("/outputs/trino")!!.toURI().toPath()

    override val sessions =
        SessionProvider(
            // some additional function overloads to test transpilation of UDFs
            scalarOverloads =
                mapOf(
                    "split" to Functions.scalarSplit,
                ),
        )
}
