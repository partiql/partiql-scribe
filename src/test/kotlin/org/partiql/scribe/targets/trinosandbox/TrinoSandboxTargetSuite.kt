package org.partiql.scribe.targets.trinosandbox

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.targets.trino.TrinoTarget
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

/**
 * Trino sandbox test suite for reproducing:
 * UNION ALL with inline struct literal path access does not transpile correctly.
 */
class TrinoSandboxTargetSuite : SqlTargetSuite() {
    override val target = TrinoTarget()

    override val root = this::class.java.getResource("/outputs/trino-sandbox")!!.toURI().toPath()

    override val sessions = SessionProvider()
}
