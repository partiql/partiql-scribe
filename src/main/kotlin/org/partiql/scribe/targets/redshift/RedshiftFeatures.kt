package org.partiql.scribe.targets.redshift

import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.sql.SqlFeatures
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

public object RedshiftFeatures : SqlFeatures.Defensive() {
    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRel(node: Rel, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: ProblemCallback) = visitChildren(node, ctx)

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

    override fun visitRex(node: Rex, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitStatementQuery(node: Statement.Query, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: ProblemCallback) = visitChildren(node, ctx)
    override fun visitRelBinding(node: Rel.Binding, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelType(node: Rel.Type, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: ProblemCallback) = visitChildren(node, ctx)
}
