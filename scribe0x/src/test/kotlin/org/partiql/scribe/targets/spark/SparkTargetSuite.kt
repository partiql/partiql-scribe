package org.partiql.scribe.targets.spark

import org.partiql.plugins.local.LocalConnector
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.test.SessionProvider
import kotlin.io.path.toPath

class SparkTargetSuite : SqlTargetSuite() {

    override val target = SparkTarget.DEFAULT

    override val root = this::class.java.getResource("/outputs/spark")!!.toURI().toPath()

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
