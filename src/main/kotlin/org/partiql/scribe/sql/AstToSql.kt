package org.partiql.scribe.sql

import org.partiql.ast.Identifier
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX
import org.partiql.scribe.ScribeContext

/**
 * Base class for AST to SQL translators.
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
        if (fnName.startsWith(markerPrefix)) {
            val realName = fnName.removePrefix(markerPrefix)
            val newFn = Identifier.regular(realName)
            val rewritten = ExprCall(newFn, node.args, node.setq)
            return super.visitExprCall(rewritten, tail)
        }
        return super.visitExprCall(node, tail)
    }
}
