package org.partiql.scribe.targets.sandbox

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class SandboxTargetSuite : SqlTargetSuite() {

    override val target = PartiQLTarget // Currently uses the PartiQLTarget

    override val root = this::class.java.getResource("/outputs/sandbox")!!.toURI().toPath()

    override val sessions = SessionProvider()
}
