package org.partiql.scribe.sql

import org.partiql.ast.Expr
import org.partiql.types.StaticType

/**
 * Pair an [Expr] with its resolved type.
 */
public class SqlArg(
    public val expr: Expr,
    public val type: StaticType,
)