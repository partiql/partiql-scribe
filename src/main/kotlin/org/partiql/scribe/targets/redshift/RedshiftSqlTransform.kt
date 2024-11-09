package org.partiql.scribe.targets.redshift

import org.partiql.plan.Catalog
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlTransform

public open class RedshiftSqlTransform(
    catalogs: List<Catalog>,
    calls: SqlCalls,
    onProblem: ProblemCallback
): SqlTransform(catalogs, calls, onProblem) {
    override fun getRexConverter(locals: Locals): RexConverter = RedshiftRexConverter(this, locals)
}
