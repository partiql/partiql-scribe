package org.partiql.scribe.targets.redshift

import org.partiql.ast.Expr
import org.partiql.plan.Catalog
import org.partiql.plan.Rex
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.sql.Locals
import org.partiql.scribe.sql.RexConverter
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.SqlTransform
import org.partiql.types.StaticType

class RedshiftTransform(
    catalogs: List<Catalog>,
    calls: SqlCalls,
    onProblem: ProblemCallback,
) : SqlTransform(catalogs, calls, onProblem) {

    override fun getRexConverter(locals: Locals): RexConverter = Visitor(this, locals)

    private class Visitor(transform: SqlTransform, locals: Locals) : RexConverter(transform, locals) {

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType): Expr {
            // TODO, parse the view and return its org.partiql.ast.Expr node
            // Invoke default logic
            return super.visitRexOpGlobal(node, ctx)
        }
    }
}
