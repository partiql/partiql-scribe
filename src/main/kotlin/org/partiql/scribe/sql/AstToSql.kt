package org.partiql.scribe.sql

import org.partiql.ast.FromExpr
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
 * Every identifier is printed through [formatIdentifier], which doubles the [identifierDelimiter] inside a delimited
 * identifier. [SqlDialect] (partiql-lang-kotlin 1.6.1) prints identifiers with a private helper that does not escape
 * it, so the visitors below route each identifier position through [formatIdentifier]. Where the node allows it, they
 * call `super` with the alias removed and append the alias, so other upstream printing changes still apply.
 *
 * TODO: drop these overrides once partiql-lang-kotlin escapes delimited identifiers and exposes an identifier hook
 *  (https://github.com/partiql/partiql-lang-kotlin/issues/2005, https://github.com/partiql/partiql-lang-kotlin/pull/2003,
 *  https://github.com/partiql/partiql-lang-kotlin/pull/2011); [identifierDelimiter] and [formatIdentifier] then
 *  become overrides of the upstream hook.
 */
public abstract class AstToSql(public val context: ScribeContext) : SqlDialect() {
    private companion object {
        /**
         * Text that every target accepts as an unquoted identifier: a letter or underscore, then letters, digits,
         * underscores, or dollar signs.
         */
        val REGULAR_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_$]*")
    }

    private val markerPrefix = SCRIBE_MARKER_FN_PREFIX

    /**
     * The character that wraps a delimited identifier in the target dialect.
     */
    protected open val identifierDelimiter: Char = '"'

    /**
     * Returns [text] as a delimited identifier, wrapped in [identifierDelimiter] with each embedded
     * [identifierDelimiter] doubled.
     */
    protected fun quoted(text: String): String = quoteIdentifier(text, identifierDelimiter)

    /**
     * Returns the SQL text for [node]. A regular identifier whose text is a valid unquoted identifier
     * ([REGULAR_IDENTIFIER]) is printed as-is. Any other identifier is [quoted], including a regular identifier whose
     * text could not be printed unquoted, e.g. a name with a space, quote, or parenthesis built from plan or catalog
     * text.
     */
    protected open fun formatIdentifier(node: Identifier.Simple): String =
        when (node.isRegular && REGULAR_IDENTIFIER.matches(node.text)) {
            true -> node.text
            false -> quoted(node.text)
        }

    override fun visitIdentifierSimple(
        node: Identifier.Simple,
        tail: SqlBlock,
    ): SqlBlock = tail concat formatIdentifier(node)

    override fun visitIdentifier(
        node: Identifier,
        tail: SqlBlock,
    ): SqlBlock {
        val path = (node.qualifier + node.identifier).joinToString(".") { formatIdentifier(it) }
        return tail concat path
    }

    override fun visitPathStepField(
        node: PathStep.Field,
        tail: SqlBlock,
    ): SqlBlock = tail concat ".${formatIdentifier(node.field)}"

    override fun visitSelectItemExpr(
        node: SelectItem.Expr,
        tail: SqlBlock,
    ): SqlBlock {
        val asAlias = node.asAlias ?: return super.visitSelectItemExpr(node, tail)
        val t = super.visitSelectItemExpr(SelectItem.Expr(node.expr, null), tail)
        return t concat " AS ${formatIdentifier(asAlias)}"
    }

    override fun visitFromExpr(
        node: FromExpr,
        tail: SqlBlock,
    ): SqlBlock {
        val asAlias = node.asAlias
        val atAlias = node.atAlias
        if (asAlias == null && atAlias == null) return super.visitFromExpr(node, tail)
        var t = super.visitFromExpr(FromExpr(node.expr, node.fromType, null, null), tail)
        t = if (asAlias != null) t concat " AS ${formatIdentifier(asAlias)}" else t
        t = if (atAlias != null) t concat " AT ${formatIdentifier(atAlias)}" else t
        return t
    }

    // The LET alias is required, so this cannot delegate to `super` with the alias removed.
    override fun visitLetBinding(
        node: Let.Binding,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat " AS ${formatIdentifier(node.asAlias)}"
        return t
    }

    override fun visitGroupBy(
        node: GroupBy,
        tail: SqlBlock,
    ): SqlBlock {
        val asAlias = node.asAlias ?: return super.visitGroupBy(node, tail)
        val t = super.visitGroupBy(GroupBy(node.strategy, node.keys, null), tail)
        return t concat " GROUP AS ${formatIdentifier(asAlias)}"
    }

    override fun visitGroupByKey(
        node: GroupBy.Key,
        tail: SqlBlock,
    ): SqlBlock {
        val asAlias = node.asAlias ?: return super.visitGroupByKey(node, tail)
        val t = super.visitGroupByKey(GroupBy.Key(node.expr, null), tail)
        return t concat " AS ${formatIdentifier(asAlias)}"
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
