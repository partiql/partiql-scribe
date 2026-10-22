package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Lists the tables that a query reads, for a caller that builds the session before the translation: every `FROM` item
 * that names a table, as the qualified name the query writes (`andes.provider.table` gives three parts), without
 * duplicates, in order of first appearance.
 *
 * A name that a common table expression of an enclosing query defines is not a table, and neither is a one-part name
 * that is the alias of another `FROM` item of the same query block (a lateral reference, `FROM t, t.arr`); such names
 * are left out. Identifiers fold as [RedshiftAstBuilder.simple] folds them, so a caller can define the tables with the
 * folded names.
 */
internal class TableReferences(private val caseSensitiveIdentifiers: Boolean) {
    private val tables = LinkedHashSet<List<String>>()

    fun collect(root: P.RootContext): List<List<String>> {
        visit(root, emptyList())
        return tables.toList()
    }

    private fun visit(
        node: ParseTree,
        commonTables: List<String>,
    ) {
        when (node) {
            is P.QueryContext -> {
                val defined = node.withClause()?.commonTableExpression().orEmpty().map { fold(it.name) }
                val scope = commonTables + defined
                node.withClause()?.commonTableExpression().orEmpty().forEach { visit(it.query(), scope) }
                node.children.orEmpty().filter { it !is P.WithClauseContext }.forEach { visit(it, scope) }
            }
            is P.TableNameContext -> {
                val parts = nameParts(node.primaryExpr())
                val commonTable = parts?.size == 1 && parts[0] in commonTables
                if (parts != null && !commonTable && !isLateral(node, parts[0])) tables.add(parts)
                node.children.orEmpty().forEach { visit(it, commonTables) }
            }
            is ParserRuleContext -> node.children.orEmpty().forEach { visit(it, commonTables) }
        }
    }

    /**
     * True if [root], the first part of the name of [item], is the alias, or the unqualified name, of another FROM item
     * of the query block of [item] or of an enclosing block: then [item] navigates into that item (`FROM t, t.arr`).
     */
    private fun isLateral(
        item: P.TableNameContext,
        root: String,
    ): Boolean {
        var ctx: ParserRuleContext? = item.parent as? ParserRuleContext
        while (ctx != null) {
            if (ctx is P.QuerySpecificationContext) {
                val items = ctx.fromClause()?.let(::fromItems).orEmpty()
                if (items.any { other -> other !== item && bindingName(other) == root }) return true
            }
            ctx = ctx.parent as? ParserRuleContext
        }
        return false
    }

    private fun bindingName(item: P.TableNameContext): String? =
        item.tableAlias()?.alias?.let(::fold) ?: nameParts(item.primaryExpr())?.lastOrNull()

    private fun fromItems(ctx: ParseTree): List<P.TableNameContext> =
        when (ctx) {
            is P.TableNameContext -> listOf(ctx) + ctx.children.orEmpty().filter { it !is P.PrimaryExprContext }.flatMap(::fromItems)
            is P.QueryContext, is P.ExprContext -> emptyList()
            is ParserRuleContext -> ctx.children.orEmpty().flatMap(::fromItems)
            else -> emptyList()
        }

    private fun nameParts(ctx: P.PrimaryExprContext): List<String>? =
        when (ctx) {
            is P.PathFieldContext -> nameParts(ctx.primaryExpr())?.plus(fold(ctx.identifier()))
            is P.PrimaryAtomContext -> (ctx.atom() as? P.AtomVariableContext)?.let { listOf(fold(it.identifier())) }
            else -> null
        }

    private fun fold(ctx: P.IdentifierContext): String =
        when (ctx) {
            is P.IdentifierQuotedContext -> {
                val text = ctx.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
                if (caseSensitiveIdentifiers) text else RedshiftAstBuilder.foldAscii(text)
            }
            else -> RedshiftAstBuilder.foldAscii(ctx.text)
        }
}
