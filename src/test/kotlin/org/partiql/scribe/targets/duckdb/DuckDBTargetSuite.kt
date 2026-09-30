package org.partiql.scribe.targets.duckdb

import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.SessionProvider
import kotlin.io.path.toPath

class DuckDBTargetSuite : SqlTargetSuite() {
    override val target = DuckDBTarget()

    override val root = this::class.java.getResource("/outputs/duckdb")!!.toURI().toPath()

    override val sessions = SessionProvider()
}
