package org.partiql.scribe.sql.rewriters

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Type
import org.partiql.ast.exprCase
import org.partiql.ast.exprCoalesce
import org.partiql.ast.exprLit
import org.partiql.ast.exprNullIf
import org.partiql.ast.util.AstRewriter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue

/**
 * The RewriteCaseWhen is able to reduce certain CASE-WHEN identities to a simpler COALESCE or NULLIF when applicable.
 */
internal object RewriteCaseWhen : Rewriter {

    override fun apply(ast: AstNode): AstNode = ast.accept(Visitor(), Unit)

    private class Visitor : AstRewriter<Unit>() {

        @OptIn(PartiQLValueExperimental::class)
        private fun isNullIf(caseWhen: Expr.Case): Boolean {
            val branches = caseWhen.branches
            val default = caseWhen.default

            if (default == null) {
                return false
            }
            if (branches.size != 1) {
                return false
            }
            val v1 = branches.first()
            if (v1.expr != exprLit(nullValue())) {
                if (v1.expr is Expr.Cast && (v1.expr as Expr.Cast).value == exprLit(nullValue())) {
                    // continue TODO figure out if this cast addition is necessary
                } else {
                    return false
                }
            }
            val v1When = v1.condition
            if (v1When is Expr.Binary && v1When.op == Expr.Binary.Op.EQ) {
                if (v1When.lhs == default) {
                    return true
                }
            }
            return false
        }

        private fun isCoalesce(caseWhen: Expr.Case): Boolean {
            return caseWhen.branches.all { branch ->
                val whenExpr = branch.condition
                val thenExpr = branch.expr
                if (whenExpr is Expr.Unary && whenExpr.op == Expr.Unary.Op.NOT) {
                    val innerExpr = whenExpr.expr
                    if (innerExpr is Expr.IsType && innerExpr.type == Type.NullType()) {
                        return@all innerExpr.value == thenExpr || innerExpr.value == (thenExpr as Expr.Cast).value
                    }
                } else if (whenExpr is Expr.IsType && whenExpr.type == Type.NullType() && whenExpr.not == true) {
                    // Redshift constant folds NOT IS TYPE to IS NOT TYPE
                    val innerExpr = whenExpr.value
                    return@all whenExpr.value == thenExpr || innerExpr == (thenExpr as Expr.Cast).value
                }
                false
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitExprCase(node: Expr.Case, ctx: Unit): AstNode {
            val case = super.visitExprCase(node, ctx) as Expr.Case
            val branches = case.branches
            val default = case.default ?: exprLit(nullValue())
            val caseWhen = when (branches.isEmpty()) {
                true -> default
                false -> {
                    if (isCoalesce(case)) { // rewrite back into coalesce
                        val exprs = branches.map {
                            it.expr
                        }
                        exprCoalesce(exprs)
                    } else if (isNullIf(case)) {  // rewrite back into nullif
                        val equality = (branches.first().condition) as Expr.Binary
                        exprNullIf(equality.lhs, equality.rhs)
                    } else {
                        exprCase(expr = null, branches = branches, default = default)
                    }
                }
            }
            return caseWhen
        }
    }
}
