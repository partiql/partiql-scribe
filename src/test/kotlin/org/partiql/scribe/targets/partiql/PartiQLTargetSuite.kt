package org.partiql.scribe.targets.partiql

import org.partiql.plugins.local.LocalConnector
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.test.SessionProvider
import kotlin.io.path.toPath

class PartiQLTargetSuite : SqlTargetSuite() {

    override val target = PartiQLTarget

    override val root = this::class.java.getResource("/outputs/partiql")!!.toURI().toPath()

    override val sessions = SessionProvider(
        mapOf(
            "default" to
                    LocalConnector.Metadata(
                        this::class.java.getResource("/catalogs/default")!!.toURI().toPath(),
                        listOf(split)
                    )
        )
    )
}
