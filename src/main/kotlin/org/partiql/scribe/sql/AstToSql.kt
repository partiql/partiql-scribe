package org.partiql.scribe.sql

import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.MARKER_EXISTS
import org.partiql.scribe.SCRIBE_MARKER_FN_PREFIX
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.quoteIdentifier

/**
 * Base class for AST to SQL translators.
 *
 * Every identifier is rendered through [identifierSql] so that delimited identifiers have their embedded quote
 * character escaped. The visitors below are overridden only because [SqlDialect] renders identifiers with a
 * private helper that does not escape the quote character; they otherwise match [SqlDialect].
 */
public abstract class AstToSql(public val context: ScribeContext) : SqlDialect() {
    private val markerPrefix = SCRIBE_MARKER_FN_PREFIX

    /**
     * The character that wraps a delimited identifier in the target dialect.
     */
    protected open val identifierQuote: Char = '"'

    /**
     * Returns [text] as a delimited identifier, wrapped in [identifierQuote] with embedded quotes doubled.
     */
    protected fun quoted(text: String): String = quoteIdentifier(text, identifierQuote)

    /**
     * Returns the SQL text for [node]. Regular identifiers are emitted as-is; delimited identifiers are [quoted].
     */
    protected open fun identifierSql(node: Identifier.Simple): String =
        when (node.isRegular) {
            true -> node.text
            false -> quoted(node.text)
        }

    override fun visitIdentifierSimple(
        node: Identifier.Simple,
        tail: SqlBlock,
    ): SqlBlock = tail concat identifierSql(node)

    override fun visitIdentifier(
        node: Identifier,
        tail: SqlBlock,
    ): SqlBlock {
        val path = (node.qualifier + node.identifier).joinToString(".") { identifierSql(it) }
        return tail concat path
    }

    override fun visitPathStepField(
        node: PathStep.Field,
        tail: SqlBlock,
    ): SqlBlock = tail concat ".${identifierSql(node.field)}"

    override fun visitSelectItemExpr(
        node: SelectItem.Expr,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${identifierSql(asAlias)}" else t
        return t
    }

    override fun visitFromExpr(
        node: FromExpr,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t =
            when (node.fromType.code()) {
                FromType.SCAN -> t
                FromType.UNPIVOT -> t concat "UNPIVOT "
                else -> defaultReturn(node, tail)
            }
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${identifierSql(asAlias)}" else t
        val atAlias = node.atAlias
        t = if (atAlias != null) t concat " AT ${identifierSql(atAlias)}" else t
        return t
    }

    override fun visitLetBinding(
        node: Let.Binding,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat " AS ${identifierSql(node.asAlias)}"
        return t
    }

    override fun visitGroupBy(
        node: GroupBy,
        tail: SqlBlock,
    ): SqlBlock {
        // Render the GROUP BY without its alias, then append the alias using the escaping renderer.
        val asAlias = node.asAlias
        var t = super.visitGroupBy(GroupBy(node.strategy, node.keys, null), tail)
        t = if (asAlias != null) t concat " GROUP AS ${identifierSql(asAlias)}" else t
        return t
    }

    override fun visitGroupByKey(
        node: GroupBy.Key,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${identifierSql(asAlias)}" else t
        return t
    }

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
            return super.visitExprCall(rewritten, tail)
        }
        return super.visitExprCall(node, tail)
    }
}
