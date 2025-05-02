package org.partiql.scribe

import org.partiql.plan.Plan
import org.partiql.spi.catalog.Session

public interface ScribeTarget<T> {
    public val target: String

    public val version: String

    public fun compile(plan: Plan, session: Session, context: ScribeContext): ScribeOutput<T>
}
