package org.partiql.scribe.sql

/**
 * A name is *assigned* to (lvalue), while an identifier *references* something (rvalue).
 *
 * ```sql
 * SELECT x AS <name> FROM <identifier>;
 * CREATE TABLE <name> (...);
 * CREATE VIEW <name> (...);
 * ```
 *
 * Note that names come with different case-normalization rules than identifiers.
 */
public class SqlName(name: String) : SqlNode() {

    /**
     * Name string value.
     */
    private val name: String = name

    /**
     * @return the name of this identifier
     */
    public fun getName(): String = name

    override fun getChildren(): List<SqlNode> = emptyList()

    override fun <R, C> accept(visitor: SqlVisitor<R, C>, ctx: C): R = visitor.visitName(this, ctx)
}
