package org.partiql.scribe.sql.utils

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
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

// ////////////////////////////// AST -> Text utils

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

internal infix fun SqlBlock.concat(rhs: String): SqlBlock {
    next = SqlBlock.Text(rhs)
    return next!!
}

internal infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
    next = rhs
    return next!!
}

internal fun type(
    symbol: String,
    vararg args: Int?,
    gap: Boolean = false,
): SqlBlock {
    val p = args.filterNotNull()
    val t =
        when {
            p.isEmpty() -> symbol
            else -> {
                val a = p.joinToString(",")
                when (gap) {
                    true -> "$symbol ($a)"
                    else -> "$symbol($a)"
                }
            }
        }
    // types are modeled as text; as we don't way to reflow
    return SqlBlock.Text(t)
}

internal fun list(
    visitor: AstVisitor<SqlBlock, SqlBlock>,
    start: String? = "(",
    end: String? = ")",
    delimiter: String? = ", ",
    children: () -> List<AstNode>,
): SqlBlock {
    val kids = children()
    val h = SqlBlock.none()
    var t = h
    kids.forEachIndexed { i, child ->
        t = child.accept(visitor, t)
        t = if (delimiter != null && (i + 1) < kids.size) t concat delimiter else t
    }
    return SqlBlock.Nest(
        prefix = start,
        postfix = end,
        child = h,
    )
}
