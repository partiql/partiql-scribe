package org.partiql.scribe.targets.trino

import org.partiql.plan.*
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

public object TrinoFeatures : SqlFeatures.Permissive() {

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: ProblemCallback) {
        when (val type = node.constructor.type) {
            is StructType -> {
                if (type.contentClosed && type.constraints.contains(TupleConstraint.Ordered)) {
                    visitChildren(node, ctx)
                } else {
                    ctx.error("SELECT VALUE of open, unordered structs is NOT supported.")
                }
            }
            else -> ctx.error("SELECT VALUE is NOT supported.")
        }
    }
}
