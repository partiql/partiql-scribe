package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Lists the tables that a query reads, for a caller that builds the session before the translation: every `FROM` item
 * that names a table, as the qualified name the query writes (`db.schema.table` gives three parts), without duplicates,
 * in order of first appearance. A name that a common table expression of an enclosing query defines is not a table.
 * Identifiers fold as [RedshiftAstBuilder.simple] folds them, so a caller can define the tables with the folded names.
 */
internal class TableReferences(caseSensitiveIdentifiers: Boolean) {
    private val names = RedshiftAstBuilder(caseSensitiveIdentifiers, ScribeProblemListener.abortOnError())
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
                val parts = node.qualifiedName().identifier().map(::fold)
                if (!(parts.size == 1 && parts[0] in commonTables)) tables.add(parts)
            }
            is ParserRuleContext -> node.children.orEmpty().forEach { visit(it, commonTables) }
        }
    }

    private fun fold(ctx: P.IdentifierContext): String = names.simple(ctx).text
}
