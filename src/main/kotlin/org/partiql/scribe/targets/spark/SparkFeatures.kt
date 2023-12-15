package org.partiql.scribe.targets.spark

import org.partiql.plan.Fn
import org.partiql.plan.Global
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

object SparkFeatures : SqlFeatures.Defensive() {

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRel(node: Rel, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: ProblemCallback) = visitChildren(node, ctx)

    // Skip projection, as the condition has been validated in visitRexOpSelect
    override fun visitRelOpProject(node: Rel.Op.Project, ctx: ProblemCallback) = visitChildren(node.input, ctx)

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: ProblemCallback) {
        when (val type = node.constructor.type) {
            is StructType -> {
                // We don't need the assertion on that the struct key is a string literal here
                // because, when the key is not a string literal, we can not type the struct field.
                // the return the struct will be an open struct with no field.
                if (type.contentClosed && type.constraints.contains(TupleConstraint.Ordered)) {
                    visitChildren(node, ctx)
                } else {
                    ctx.error("SELECT VALUE of open, unordered structs is NOT supported.")
                }
            }

            else -> ctx.error("SELECT VALUE is NOT supported.")
        }


//        val relProject = input.op as? Rel.Op.Project ?: return null
//        val structOp = getConstructorFromProjection(constructor, relProject)?.op as? Rex.Op.Struct ?: return null
//        val newRexToSql = RexToSql(transform, Locals(relProject.input.type.schema))
//        val type = constructor.type as? StructType ?: return null
//        if (type.constraints.contains(TupleConstraint.Open(false))
//                .not() || type.constraints.contains(TupleConstraint.Ordered).not()
//        ) {
//            return null
//        }
//
//        // AGG HACK; this is terrible!
//        val projections = if (curr != null && curr is Select.Project) {
//            val first = curr.items.first() as Select.Project.Item.Expression
//            val struct = first.expr as Expr.Struct
//            struct.fields.map {
//                val expr = it.value
//                val asAlias = binder(((it.name as Expr.Lit).value as StringValue).value ?: "")
//                selectProjectItemExpression(expr, asAlias)
//            }
//        } else {
//            structOp.fields.map { field ->
//                val key = field.k.op
//                if (key !is Rex.Op.Lit || key.value !is StringValue) {
//                    return null
//                }
//                val fieldName = (key.value as StringValue).value ?: return null
//                //
//                val expr = newRexToSql.apply(field.v)
//                val asAlias = binder(fieldName)
//                selectProjectItemExpression(expr, asAlias)
//            }
//        }
//
//        return selectProject(
//            items = projections,
//            setq = setq
//        )
    }

    override fun visitRex(node: Rex, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitStatementQuery(node: Statement.Query, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitGlobal(node: Global, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpPathStep(node: Rex.Op.Path.Step, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelBinding(node: Rel.Binding, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelType(node: Rel.Type, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpPathStepSymbol(node: Rex.Op.Path.Step.Symbol, ctx: ProblemCallback) =
        visitChildren(node, ctx)

    override fun visitFn(node: Fn, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: ProblemCallback) = visitChildren(node, ctx)

    override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: ProblemCallback) = visitChildren(node, ctx)
}