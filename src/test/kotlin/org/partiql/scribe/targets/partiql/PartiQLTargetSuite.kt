package org.partiql.scribe.targets.partiql

import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.test.SessionProvider
import kotlin.io.path.toPath

class PartiQLTargetSuite : SqlTargetSuite() {

    override val target = PartiQLTarget

    override val root = this::class.java.getResource("/outputs/partiql")!!.toURI().toPath()

    override val sessions = SessionProvider(
        mapOf(
            "default" to ionStructOf(
                "connector_name" to ionString("local"),
                "root" to ionString(this::class.java.getResource("/catalogs/default")!!.path)
            )
        )
    )
}
