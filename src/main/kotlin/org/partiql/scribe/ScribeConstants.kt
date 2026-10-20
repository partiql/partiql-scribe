package org.partiql.scribe

/**
 * Scribe uses a marker function pattern to communicate between the plan-to-AST layer (RelConverter)
 * and the AST-to-SQL text layer (AstToSql).
 *
 * Problem: Some SQL constructs (e.g., Spark's LATERAL VIEW EXPLODE, Trino's CROSS JOIN UNNEST)
 * have no direct representation in the PartiQL AST. The AST only supports standard FROM expressions
 * and JOINs.
 *
 * Solution: RelConverter produces a standard ExprCall AST node with a prefixed function name
 * (e.g., "SCRIBE_MARKER_FN_EXPLODE"). The target's AstToSql detects this prefix and renders
 * the appropriate dialect-specific syntax instead of a normal function call. The base AstToSql
 * strips the prefix before rendering, so if a marker reaches the default rendering path, it
 * produces the real function name (e.g., "EXPLODE").
 */
internal const val SCRIBE_MARKER_FN_PREFIX: String = "SCRIBE_MARKER_FN_"

/**
 * Marks an `EXISTS (<subquery>)` predicate. There is no EXISTS AST node, and rendering it as a plain call would
 * emit `exists(SELECT ...)` or a quoted function name, so [org.partiql.scribe.sql.AstToSql] renders it directly.
 */
internal const val MARKER_EXISTS: String = "${SCRIBE_MARKER_FN_PREFIX}EXISTS"
