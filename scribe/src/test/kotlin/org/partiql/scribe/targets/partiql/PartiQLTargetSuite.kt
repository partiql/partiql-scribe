package org.partiql.scribe.targets.partiql

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.Functions
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class PartiQLTargetSuite : SqlTargetSuite() {
    override val target = PartiQLTarget

    override val root = this::class.java.getResource("/outputs/partiql")!!.toURI().toPath()

    override val sessions =
        SessionProvider(
            // some additional function overloads to test transpilation of UDFs
            scalarOverloads =
                mapOf(
                    "split" to Functions.scalarSplit,
                ),
        )
}
