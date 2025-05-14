package org.partiql.scribe.targets.sandbox

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.targets.trino.TrinoTarget
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

/**
 * Basic test suite to allow us to transpile a single query with the specified [target]. We may opt to delete this once
 * all the previous tests are re-enabled.
 *
 * See resources/inputs/sandbox/sandbox.sql for the location of the input PartiQL query.
 * See resources/outputs/sandbox/sandbox/sandbox.sql for the location of the output query.
 */
class SandboxTargetSuite : SqlTargetSuite() {
    override val target = TrinoTarget() // Currently uses the PartiQLTarget

    override val root = this::class.java.getResource("/outputs/sandbox")!!.toURI().toPath()

    override val sessions = SessionProvider()
}
