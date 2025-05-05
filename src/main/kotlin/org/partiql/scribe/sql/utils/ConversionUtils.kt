package org.partiql.scribe.sql.utils

import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.QueryBody
import org.partiql.scribe.sql.RelConverter

// TODO move to utils
public fun RelConverter.RelContext.toQueryBodySFW(): QueryBody.SFW {
    assert(this.select != null)
    return queryBodySFW(
        select = this.select!!,
        exclude = this.exclude,
        from = this.from!!,
        let = this.let,
        where = this.where,
        groupBy = this.groupBy,
        having = this.having,
    )
}
