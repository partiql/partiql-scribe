package org.partiql.scribe

import org.partiql.plan.Plan
import org.partiql.spi.catalog.Session

public class Scribe(
    public val scribeContext: ScribeContext,
) {
    public fun <T> compile(
        plan: Plan,
        session: Session,
        target: ScribeTarget<T>,
    ): Result<T> {
        // TODO run plan validation pass to ensure no error nodes
        val output = target.compile(plan, session, context = scribeContext)
        return Result(plan, output)
    }

    public class Result<T>(
        public val input: Plan,
        public val output: ScribeOutput<T>,
    )
}
