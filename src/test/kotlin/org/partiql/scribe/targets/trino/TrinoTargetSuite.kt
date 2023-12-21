package org.partiql.scribe.targets.trino

import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.plugins.local.LocalConnector
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.test.SessionProvider
import kotlin.io.path.toPath

class TrinoTargetSuite : SqlTargetSuite() {

    override val target = TrinoTarget

    override val root = this::class.java.getResource("/outputs/trino")!!.toURI().toPath()

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
