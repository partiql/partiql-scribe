package org.partiql.scribe.sql.utils

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.scribe.sql.RelConverter

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

// AST -> Text
internal fun unquotedStringExpr(s: String): Expr {
    return exprVarRef(Identifier.regular(s), isQualified = false)
}

// returns the string alias of an expr if an alias can be inferred. returns null when an alias cannot be inferred
// and must be generated
internal fun Expr.inferredAlias(): String? {
    return when (this) {
        is ExprVarRef -> this.identifier.inferredAlias()
        is ExprPath -> this.inferredAlias()
        is ExprCast -> this.value.inferredAlias()
        // ignore other exprs which are commonly generated; if we need to remove some more aliases, could
        // get rid of `Expr.SessionAttribute`'s generated alias
        else -> null
    }
}

private fun Identifier.Simple.inferredAlias(): String {
    return this.text
}

private fun Identifier.inferredAlias(): String {
    // get the last symbol in qualified path
    return this.identifier.text
}

private fun ExprPath.inferredAlias(): String? {
    if (steps.isEmpty()) return root.inferredAlias()
    // get last step in a path expr
    return when (val last = steps.last()) {
        // get `aBc` from `t.foo.aBc`
        is PathStep.Field -> last.field.inferredAlias()
        // get `aBc` from `t.foo['aBc']`
        is PathStep.Element -> {
            val k = last.element
            if (k is ExprLit && k.lit.code() == Literal.STRING) {
                k.lit.stringValue()
            } else {
                // ignore generated aliases
                null
            }
        }
        // ignore other path exprs
        else -> null
    }
}
