package org.partiql.scribe.targets.spark

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.PlanToAst
import org.partiql.scribe.sql.RelConverter
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlCalls
import org.partiql.spi.catalog.Session

public class SparkPlanToAst(
    public val session: Session,
    public val calls: SqlCalls,
    public val context: ScribeContext,
) : PlanToAst(session, calls, context) {
    override fun getRexConverter(locals: Locals): RexConverter {
        return SparkRexConverter(this, locals, context)
    }

    override fun getRelConverter(): RelConverter {
        return SparkRelConverter(this, context)
    }
}
