package org.partiql.scribe.targets.redshift

import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RelConverter
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.PlanToAst
import org.partiql.spi.catalog.Session

class RedshiftPlanToAst(
    val session: Session,
    val calls: SqlCalls,
    val context: ScribeContext
): PlanToAst(session, calls, context) {
    override fun getRexConverter(locals: Locals): RexConverter {
        return RedshiftRexConverter(this, locals, context)
    }

    override fun getRelConverter(): RelConverter {
        return RedshiftRelConverter(this, context)
    }
}
