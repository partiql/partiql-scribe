package org.partiql.scribe.sql

import org.partiql.ast.Identifier
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprRowValue
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.MARKER_EXISTS
import org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list

/**
 * Base class for AST to SQL translators.
 *
 * A subquery in an argument list (a function call, `COALESCE`, `NULLIF`) or a row value (an `IN` list) is parenthesized,
 * `COALESCE((SELECT ...), 0)`; the dialect's own lists wrap a subquery only when it is an operand.
 */
public abstract class AstToSql(public val context: ScribeContext) : SqlDialect() {
    private val markerPrefix = SCRIBE_MARKER_FN_PREFIX

    /**
     * Strip the marker prefix from function calls so the rendered SQL uses the real function name.
     */
    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
        val fnName = node.function.identifier.getText()
        if (fnName == MARKER_EXISTS) {
            return visitExpr(node.args[0], tail concat "EXISTS (") concat ")"
        }
        if (fnName.startsWith(markerPrefix)) {
            val realName = fnName.removePrefix(markerPrefix)
            val newFn = Identifier.regular(realName)
            val rewritten = ExprCall(newFn, node.args, node.setq)
            return visitExprCall(rewritten, tail)
        }
        if (node.args.any { it is ExprQuerySet }) {
            val setq = node.setq
            var t = visitIdentifier(node.function, tail)
            t = t concat list(this, if (setq != null) "(${setq.name()} " else "(") { node.args }
            return t
        }
        return super.visitExprCall(node, tail)
    }

    override fun visitExprCoalesce(
        node: ExprCoalesce,
        tail: SqlBlock,
    ): SqlBlock = tail concat "COALESCE" concat list(this) { node.args }

    override fun visitExprNullIf(
        node: ExprNullIf,
        tail: SqlBlock,
    ): SqlBlock = tail concat "NULLIF" concat list(this) { listOf(node.v1, node.v2) }

    override fun visitExprRowValue(
        node: ExprRowValue,
        tail: SqlBlock,
    ): SqlBlock = tail concat list(this) { node.values }
}
