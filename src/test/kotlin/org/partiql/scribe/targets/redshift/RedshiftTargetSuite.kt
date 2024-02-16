package org.partiql.scribe.targets.redshift

import org.partiql.plugins.local.LocalConnector
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.test.SessionProvider
import kotlin.io.path.toPath

class RedshiftTargetSuite : SqlTargetSuite() {

    override val target = RedshiftTarget.DEFAULT

    override val root = this::class.java.getResource("/outputs/redshift")!!.toURI().toPath()

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
