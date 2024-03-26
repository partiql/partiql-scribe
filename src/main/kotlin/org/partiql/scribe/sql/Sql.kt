package org.partiql.scribe.sql

import org.partiql.ast.sql.SqlBlock

internal infix fun SqlBlock.concat(rhs: String): SqlBlock {
    next = SqlBlock.Text(rhs)
    return next!!
}

internal infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
    next = rhs
    return next!!
}
