package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.catalog.Identifier as SpiIdentifier

/**
 * A FROM item of a query block, as the name resolution of the AST builder sees it.
 *
 * Redshift resolves an unqualified column name to the FROM item of the innermost query block that has the column, and
 * an outer block only when no FROM item of the inner block has it. PartiQL resolves a name as a FROM alias first, then
 * as a catalog table, then as a field of a FROM item. The AST builder therefore qualifies every column reference with
 * the alias of its FROM item, so that PartiQL resolves it as Redshift does; see [Scope.resolveColumn].
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_correlated_subqueries.html
 *
 * @property name The name that Redshift references use: the alias, or the table name without an alias.
 * @property alias The alias in the PartiQL AST. It is the name, unless the translation renames the alias.
 * @property columns The column names, or null if the catalog does not know them (an open or dynamic schema).
 * @property origin The FROM item in the parse tree, which identifies the binding in every pass of the AST builder.
 */
internal class Binding(
    val name: Identifier.Simple,
    val alias: Identifier.Simple,
    val kind: Kind,
    val columns: List<String>?,
    val scope: Scope,
    val origin: ParserRuleContext,
    /** The name of the table as the FROM clause writes it, if the FROM item is a table without an alias. */
    val table: List<Identifier.Simple>? = null,
) {
    enum class Kind {
        /** A table, view, common table expression, or subquery: a name is a column. */
        RELATION,

        /** An element of an unnested SUPER array (`FROM t, t.items AS i`): the name is the element value. */
        ELEMENT,
    }

    /** True if [column] is a column of this binding, with the PartiQL matching rules of [ref]. */
    fun hasColumn(ref: Identifier.Simple): Boolean = columns?.any { matches(ref, it) } == true

    override fun toString(): String = "Binding(${name.text} AS ${alias.text}, $kind, $columns)"
}

/**
 * A column of a `JOIN ... USING (c)`: an unqualified reference to `c` is the column of the left (INNER, LEFT) or right
 * (RIGHT) FROM item, or `COALESCE(left.c, right.c)` (FULL).
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause30.html
 */
internal class UsingColumn(
    val name: Identifier.Simple,
    val expr: () -> Expr,
    val bindings: List<Binding>,
)

/**
 * A common table expression: the Redshift name, the name of the PartiQL WITH element, and the output columns.
 */
internal class CommonTable(
    val name: Identifier.Simple,
    val partiqlName: Identifier.Simple,
    val columns: List<String>?,
)

/**
 * The names that a query block can see. [parent] is the enclosing query block: a subquery in an expression sees the
 * FROM items of the query that contains it, and a subquery in FROM sees the FROM items of the enclosing query blocks,
 * but not the other FROM items of its own FROM clause.
 */
internal class Scope(
    val parent: Scope?,
    val commonTables: List<CommonTable>,
) {
    /** The FROM items, in FROM order. */
    val bindings = mutableListOf<Binding>()

    /** When not null, only these FROM items are visible: the items of a join for its ON condition. */
    var visible: List<Binding>? = null

    /** The USING columns of the joins of the FROM clause. */
    val usingColumns = mutableListOf<UsingColumn>()

    fun visibleBindings(): List<Binding> = visible ?: bindings

    /** The common table expression named [name] that is in scope, the innermost first. */
    fun commonTable(name: Identifier.Simple): CommonTable? = commonTables.lastOrNull { matches(name, it.name.text) }

    /**
     * Returns the FROM item named [name] (an alias or an unaliased table name), searching this block, then the
     * enclosing blocks.
     */
    fun resolveBinding(name: Identifier.Simple): Binding? {
        var scope: Scope? = this
        while (scope != null) {
            scope.visibleBindings().lastOrNull { matches(name, it.name.text) }?.let { return it }
            scope = scope.parent
        }
        return null
    }

    /**
     * Resolves the unqualified column name [name] as Redshift does: the innermost block whose FROM items have the
     * column. Within a block, exactly one FROM item must have it; a FROM item with unknown columns may have it.
     */
    fun resolveColumn(name: Identifier.Simple): ColumnResolution {
        var scope: Scope? = this
        while (scope != null) {
            val visible = scope.visibleBindings()
            scope.usingColumns.lastOrNull { matches(name, it.name.text) && visible.containsAll(it.bindings) }?.let {
                return ColumnResolution.Using(it)
            }
            val known = visible.filter { it.kind == Binding.Kind.RELATION && it.hasColumn(name) }
            val elements = visible.filter { it.kind == Binding.Kind.ELEMENT && matches(name, it.name.text) }
            val unknown = visible.filter { it.kind == Binding.Kind.RELATION && it.columns == null }
            when {
                known.size == 1 && elements.isEmpty() -> return ColumnResolution.Column(known.single())
                known.isEmpty() && elements.size == 1 -> {
                    return if (unknown.isEmpty()) ColumnResolution.Element(elements.single()) else ColumnResolution.Unknown
                }
                known.size > 1 && elements.isEmpty() -> return ColumnResolution.Ambiguous
                known.isNotEmpty() || elements.isNotEmpty() -> return ColumnResolution.Unknown
                unknown.size > 1 -> return ColumnResolution.Unknown
                unknown.size == 1 -> {
                    // The column is in this FROM item, unless it is a column of an enclosing block.
                    return if (scope.parent?.mayHaveColumn(name) == true) {
                        ColumnResolution.Unknown
                    } else {
                        ColumnResolution.Column(
                            unknown.single(),
                        )
                    }
                }
            }
            scope = scope.parent
        }
        return ColumnResolution.NotFound
    }

    private fun mayHaveColumn(name: Identifier.Simple): Boolean {
        var scope: Scope? = this
        while (scope != null) {
            if (scope.visibleBindings().any { it.columns == null || it.hasColumn(name) || matches(name, it.name.text) }) return true
            scope = scope.parent
        }
        return false
    }

    /** The FROM items of this block and of the enclosing blocks. */
    fun allVisibleBindings(): List<Binding> = visibleBindings() + (parent?.allVisibleBindings() ?: emptyList())
}

internal sealed class ColumnResolution {
    class Column(val binding: Binding) : ColumnResolution()

    class Element(val binding: Binding) : ColumnResolution()

    class Using(val column: UsingColumn) : ColumnResolution()

    /** More than one FROM item of the innermost block that has the column has it: a Redshift error. */
    object Ambiguous : ColumnResolution()

    /** A FROM item with unknown columns may have the column, so the translator cannot decide. */
    object Unknown : ColumnResolution()

    object NotFound : ColumnResolution()
}

/**
 * Matches the name [text] with the reference [ref] as PartiQL does: a regular identifier is case-insensitive, a
 * delimited identifier is exact.
 */
internal fun matches(
    ref: Identifier.Simple,
    text: String,
): Boolean = if (ref.isRegular) ref.text.equals(text, ignoreCase = true) else ref.text == text

/**
 * The decisions about names that hold for every pass of the AST builder over the same parse tree. The first pass finds
 * the FROM aliases that must be renamed, because PartiQL would resolve a reference to them to another FROM item; the
 * later passes rename them.
 */
internal class NameAnalysis(
    /** The texts of all tokens of the input, lower case, so that generated names do not collide with a name. */
    private val usedNames: MutableSet<String>,
) {
    private val renames = HashMap<ParserRuleContext, Identifier.Simple>()
    private val referencedAcrossBlocks = LinkedHashSet<ParserRuleContext>()
    private val passBindings = mutableListOf<Binding>()
    private val fresh = HashMap<String, Int>()

    /** The alias of the FROM item [origin] in this pass. */
    fun alias(
        origin: ParserRuleContext,
        alias: Identifier.Simple,
    ): Identifier.Simple = renames[origin] ?: alias

    fun register(binding: Binding) {
        passBindings.add(binding)
    }

    /** Records that a column reference of an inner query block resolves to [binding]. */
    fun referencedFromInnerBlock(binding: Binding) {
        referencedAcrossBlocks.add(binding.origin)
    }

    /**
     * Ends a pass. A FROM item that a column reference of an inner block resolves to gets a new alias if another FROM
     * item of the statement has the same alias, because PartiQL resolves the alias of a qualified reference to the
     * innermost FROM item with that alias. Returns true if this pass found an alias to rename.
     */
    fun finishPass(): Boolean {
        var renamed = false
        for (binding in passBindings) {
            if (binding.origin !in referencedAcrossBlocks || binding.origin in renames) continue
            val collides =
                passBindings.any {
                    it !== binding && it.origin !== binding.origin && it.alias.text.equals(binding.alias.text, ignoreCase = true)
                }
            if (collides) {
                renames[binding.origin] = freshName(binding.alias.text)
                renamed = true
            }
        }
        passBindings.clear()
        referencedAcrossBlocks.clear()
        return renamed
    }

    /** True if [origin] is a renamed FROM item. */
    fun isRenamed(origin: ParserRuleContext): Boolean = origin in renames

    /**
     * Returns a delimited identifier `<prefix><n>` that is not a name of the input and was not returned before. The
     * same sequence of calls returns the same names in every pass.
     */
    fun freshName(prefix: String): Identifier.Simple {
        var n = fresh[prefix] ?: 1
        while (true) {
            val candidate = "$prefix\$$n"
            n++
            if (usedNames.add(candidate.lowercase())) {
                fresh[prefix] = n
                return Identifier.Simple.delimited(candidate)
            }
        }
    }

    /** True if [name] is a name of the input. */
    fun isUsed(name: String): Boolean = name.lowercase() in usedNames
}

/**
 * Generated names of one pass of the AST builder: the hidden columns and aliases of rewrites. Each pass starts from
 * the same state, so that all passes generate the same names.
 */
internal class PassNames(private val analysis: NameAnalysis) {
    private val issued = HashSet<String>()

    /** Returns a delimited identifier with [name], or `name<n>` if a name of the input or an issued name is the same. */
    fun fresh(name: String): Identifier.Simple {
        var candidate = name
        var n = 1
        while (analysis.isUsed(candidate) || !issued.add(candidate.lowercase())) {
            candidate = "$name${n++}"
        }
        return Identifier.Simple.delimited(candidate)
    }
}

/**
 * Looks up tables in the session catalog the way the PartiQL planner resolves a FROM source, for the column names of
 * a table.
 */
internal class RedshiftCatalog(private val session: Session) {
    class Table(val columns: List<String>?)

    /**
     * Returns the table named by [parts], or null if no table has exactly that name. A name with more parts than a
     * table name (a path into a table) is not a table.
     */
    fun table(parts: List<Identifier.Simple>): Table? {
        val identifier =
            SpiIdentifier.of(
                parts.map {
                    if (it.isRegular) SpiIdentifier.Simple.regular(it.text) else SpiIdentifier.Simple.delimited(it.text)
                },
            )
        val catalogs = session.getCatalogs()
        val default = catalogs.getCatalog(session.getCatalog()) ?: return null
        // 1. In the current namespace.
        val namespace = session.getNamespace()
        val inNamespace =
            if (namespace.isEmpty()) {
                identifier
            } else {
                SpiIdentifier.of(namespace.getLevels().map { SpiIdentifier.Simple.delimited(it) } + identifier.getParts())
            }
        lookup(default, inNamespace)?.let { return it }
        if (!identifier.hasQualifier()) return null
        // 2. Schema-qualified.
        lookup(default, identifier)?.let { return it }
        // 3. Catalog-qualified.
        val head = identifier.getParts().first()
        val catalog = catalogs.getCatalog(head.getText(), ignoreCase = head.isRegular()) ?: return null
        return lookup(catalog, SpiIdentifier.of(identifier.getParts().drop(1)))
    }

    private fun lookup(
        catalog: org.partiql.spi.catalog.Catalog,
        identifier: SpiIdentifier,
    ): Table? {
        val name = catalog.resolveTable(session, identifier) ?: return null
        if (name.count() != identifier.getParts().size) return null
        val table = catalog.getTable(session, name) ?: return null
        return Table(columns(table.getSchema()))
    }

    private fun columns(schema: PType): List<String>? {
        val element =
            when (schema.code()) {
                PType.BAG, PType.ARRAY -> schema.typeParameter
                else -> return null
            }
        if (element.code() != PType.ROW) return null
        return element.fields.map { it.name }
    }
}

/**
 * Stops the translation at a query that Redshift rejects: an ambiguous or unknown column, a wrong position. The
 * transpiler reports it as INVALID_INPUT.
 */
internal class InvalidQueryException(
    override val message: String,
    val line: Int,
    val column: Int,
) : RuntimeException(message) {
    companion object {
        fun at(
            ctx: ParserRuleContext,
            message: String,
        ): InvalidQueryException = at(ctx.start, message)

        fun at(
            token: Token,
            message: String,
        ): InvalidQueryException = InvalidQueryException(message, token.line, token.charPositionInLine + 1)
    }
}
