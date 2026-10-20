package org.partiql.scribe.frontend.redshift.internal

import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.AstNode
import org.partiql.ast.FromExpr
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.WindowPartition
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener

/**
 * Rewrites of the PartiQL AST that the Scribe PartiQL target makes from a plan, before it is printed. Each rewrite
 * fixes a form that the target prints, but that PartiQL does not read back as the same query; see
 * [PartiQLOutputTarget].
 */
internal object PartiQLOutputRewrites {
    fun apply(
        node: AstNode,
        listener: ScribeProblemListener,
    ): AstNode {
        val names = FreshNames(node)
        var result = GroupKeyReferences(names).visit(node, Unit)
        result = WithHeaders.visit(result, Unit)
        result = LateralRoots.visit(result, false)
        return PrintableIdentifiers(listener).visit(result, Unit)
    }

    /**
     * Names that do not occur as an identifier in the output, for the aliases that the rewrites add.
     */
    private class FreshNames(root: AstNode) {
        private val used = HashSet<String>()
        private var next = 0

        init {
            // The default visit visits the children, then calls defaultReturn.
            val collector =
                object : AstWalker() {
                    override fun visitIdentifierSimple(
                        node: Identifier.Simple,
                        ctx: Unit,
                    ) {
                        used.add(node.text.lowercase())
                    }
                }
            root.accept(collector, Unit)
        }

        fun fresh(prefix: String): Identifier.Simple {
            while (true) {
                val name = "$prefix${next++}"
                if (used.add(name.lowercase())) return Identifier.Simple.delimited(name)
            }
        }
    }

    /**
     * References to GROUP BY keys after the aggregation.
     *
     * The Scribe target prints a reference to a group key in HAVING, ORDER BY, and a window PARTITION BY as the key
     * expression, for example `"t"['c']`. After GROUP BY, PartiQL does not resolve such a path: the variable `t` is not
     * in scope (VAR_REF_NOT_FOUND), or it is a catalog table of the same name, and the path is always MISSING. This
     * rewrite gives each referenced key a fresh alias, `GROUP BY "t"['c'] AS "$k0"`, and prints the references as the
     * alias. The select list keeps the key expressions, which PartiQL matches with the keys.
     *
     * The target makes each reference from the same key node, or a path that extends it, so the rewrite finds the
     * references by identity. The aliases are unique in the statement, so that a reference from a subquery to a key of
     * an outer query is not captured by a key of the subquery.
     */
    private class GroupKeyReferences(private val names: FreshNames) : IdentityRewriter<Unit>() {
        override fun visitExprQuerySet(
            node: ExprQuerySet,
            ctx: Unit,
        ): AstNode {
            val body = node.body
            val groupBy = (body as? QueryBody.SFW)?.groupBy
            val rewritten = if (groupBy != null && groupBy.keys.isNotEmpty()) aliasKeys(node, body, groupBy) else node
            return super.visitExprQuerySet(rewritten, ctx)
        }

        private fun aliasKeys(
            node: ExprQuerySet,
            body: QueryBody.SFW,
            groupBy: GroupBy,
        ): ExprQuerySet {
            val keys = groupBy.keys.map { it.expr }
            // First find the referenced keys, then name them in key order, so that the names do not depend on the
            // order of the clauses.
            val referenced = BooleanArray(keys.size)
            val finder =
                KeyReplacer(keys) { i ->
                    referenced[i] = true
                    null
                }
            body.having?.let { finder.visit(it, Unit) }
            node.orderBy?.let { finder.visit(it, Unit) }
            val partitions =
                PartitionReplacer(keys) { i ->
                    referenced[i] = true
                    null
                }
            partitions.visit(body.select, Unit)
            if (referenced.none { it }) return node

            val aliases = keys.indices.map { i -> if (referenced[i]) names.fresh("\$k") else null }
            val ref = { i: Int -> aliases[i]?.let { exprVarRef(Identifier.of(it), false) } }
            val replacer = KeyReplacer(keys, ref)
            val having = body.having?.let { replacer.visit(it, Unit) as Expr }
            val orderBy = node.orderBy?.let { replacer.visit(it, Unit) as OrderBy }
            val select = PartitionReplacer(keys) { i -> aliases[i]?.let { Identifier.of(it) } }.visit(body.select, Unit) as Select
            val newKeys = groupBy.keys.mapIndexed { i, key -> aliases[i]?.let { groupByKey(key.expr, it) } ?: key }
            val newGroupBy = groupBy(groupBy.strategy, newKeys, groupBy.asAlias)
            val newBody = QueryBody.SFW(select, body.exclude, body.from, body.let, body.where, newGroupBy, having, body.window)
            return exprQuerySet(newBody, orderBy, node.limit, node.offset, node.with)
        }
    }

    /**
     * Replaces each reference to the key `keys[i]` (the key node itself, or a path that extends it) with `ref(i)`. A
     * null `ref(i)` keeps the reference.
     */
    private class KeyReplacer(
        private val keys: List<Expr>,
        private val ref: (Int) -> Expr?,
    ) : IdentityRewriter<Unit>() {
        override fun visitExpr(
            node: Expr,
            ctx: Unit,
        ): AstNode = replace(node) ?: super.visitExpr(node, ctx)

        private fun replace(node: Expr): Expr? {
            keys.forEachIndexed { i, key ->
                if (node === key) return ref(i)
                if (node is ExprPath) {
                    if (node.root === key) return ref(i)?.let { exprPath(it, node.steps) }
                    if (key is ExprPath && node.root === key.root && node.steps.size > key.steps.size &&
                        key.steps.indices.all { node.steps[it] === key.steps[it] }
                    ) {
                        return ref(i)?.let { exprPath(it, node.steps.drop(key.steps.size)) }
                    }
                }
            }
            return null
        }
    }

    /**
     * Replaces each window PARTITION BY column that is the key `keys[i]` with `ref(i)`. The target prints the column as
     * a qualified name, `"t"."c"`, which PartiQL resolves to an outer variable `t` when one is in scope.
     */
    @Suppress("DEPRECATION")
    private class PartitionReplacer(
        keys: List<Expr>,
        private val ref: (Int) -> Identifier?,
    ) : IdentityRewriter<Unit>() {
        private val identifiers = keys.map { qualifiedName(it) }

        override fun visitExprQuerySet(
            node: ExprQuerySet,
            ctx: Unit,
        ): AstNode = node

        override fun visitWindowPartition(
            node: WindowPartition,
            ctx: Unit,
        ): AstNode {
            val i = identifiers.indexOf(node.columnReference)
            if (i < 0) return node
            return ref(i)?.let { WindowPartition(it) } ?: node
        }

        /** The qualified name that the Scribe target prints for a partition column, or null. */
        private fun qualifiedName(expr: Expr): Identifier? {
            val parts = mutableListOf<String>()
            val root = (if (expr is ExprPath) expr.root else expr) as? ExprVarRef ?: return null
            if (root.identifier.hasQualifier()) return null
            parts.add(root.identifier.identifier.text)
            if (expr is ExprPath) {
                for (step in expr.steps) {
                    when (step) {
                        is PathStep.Field -> parts.add(step.field.text)
                        is PathStep.Element -> {
                            val element = step.element
                            if (element is ExprLit && element.lit.code() == Literal.STRING) {
                                parts.add(
                                    element.lit.stringValue(),
                                )
                            } else {
                                return null
                            }
                        }
                        else -> return null
                    }
                }
            }
            return Identifier.delimited(parts)
        }
    }

    /**
     * Removes the WITH clause. The planner replaces every reference to a common table expression with the planned
     * query, so the printed query does not refer to the WITH clause, which would only repeat the queries.
     */
    private object WithHeaders : IdentityRewriter<Unit>() {
        override fun visitExprQuerySet(
            node: ExprQuerySet,
            ctx: Unit,
        ): AstNode {
            val visited = super.visitExprQuerySet(node, ctx) as ExprQuerySet
            if (visited.with == null) return visited
            return exprQuerySet(visited.body, visited.orderBy, visited.limit, visited.offset, null)
        }
    }

    /**
     * Lateral references in FROM sources: `FROM t, t.x AS e`. In a FROM source, PartiQL resolves a name as a catalog
     * table before it resolves it as a variable, so a table with the name of the alias `t` would capture `"t"['x']`.
     * This rewrite prints each variable root of a FROM source expression with `@`, which PartiQL resolves as a variable.
     * The context is true inside a FROM source.
     */
    private object LateralRoots : IdentityRewriter<Boolean>() {
        override fun visitFromExpr(
            node: FromExpr,
            ctx: Boolean,
        ): AstNode {
            if (node.expr is ExprQuerySet) return super.visitFromExpr(node, false)
            val expr = visitExpr(node.expr, true) as Expr
            return if (expr === node.expr) node else FromExpr(expr, node.fromType, node.asAlias, node.atAlias)
        }

        override fun visitExprQuerySet(
            node: ExprQuerySet,
            ctx: Boolean,
        ): AstNode = super.visitExprQuerySet(node, false)

        override fun visitExprVarRef(
            node: ExprVarRef,
            ctx: Boolean,
        ): AstNode {
            // Catalog tables are printed with the catalog name as the qualifier.
            if (!ctx || node.isQualified || node.identifier.hasQualifier()) return node
            return exprVarRef(node.identifier, isQualified = true)
        }
    }

    /**
     * The PLK SQL printer writes a delimited identifier between double quotes without doubling a double quote in the
     * name, and a regular identifier verbatim. This rewrite doubles the double quotes, and rejects a regular identifier
     * that PartiQL does not read as one identifier (for example a name with a blank), because a case-insensitive name
     * cannot be printed otherwise.
     */
    private class PrintableIdentifiers(private val listener: ScribeProblemListener) : IdentityRewriter<Unit>() {
        override fun visitIdentifierSimple(
            node: Identifier.Simple,
            ctx: Unit,
        ): AstNode {
            if (node.isRegular) {
                if (!REGULAR.matches(node.text)) {
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            ScribeProblem.UNSUPPORTED_OPERATION,
                            "case-insensitive name `${node.text}`, which is not a PartiQL identifier",
                        ),
                    )
                }
                return node
            }
            if (!node.text.contains('"')) return node
            return Identifier.Simple.delimited(node.text.replace("\"", "\"\""))
        }

        private companion object {
            /** A PartiQL regular identifier: `IDENTIFIER: [A-Z$_][A-Z0-9$_]*`, case-insensitive. */
            val REGULAR = Regex("[A-Za-z\$_][A-Za-z0-9\$_]*")
        }
    }
}
