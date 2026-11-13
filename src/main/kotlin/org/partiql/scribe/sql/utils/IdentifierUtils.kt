package org.partiql.scribe.sql.utils

/**
 * Returns [text] as a delimited identifier wrapped in [quote], with each embedded [quote] doubled, e.g.
 * `a"b` -> `"a""b"` (or, for Spark, ``a`b`` -> ```a``b```). Every target SQL dialect uses doubling to escape the
 * delimiter inside a delimited identifier, so this is the only escaping needed to keep the identifier text from
 * ending the identifier early.
 *
 * All delimited identifiers emitted by Scribe must go through this function (see [org.partiql.scribe.sql.AstToSql]).
 */
internal fun quoteIdentifier(
    text: String,
    quote: Char = '"',
): String {
    val q = quote.toString()
    return q + text.replace(q, q + q) + q
}
