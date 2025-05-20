package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.sql.utils.inferredAlias

public open class RedshiftDialect : SqlDialect() {
    /**
     * Redshift does not support x['y'] syntax; replace with x.y
     */
    override fun visitPathStepElement(
        node: PathStep.Element,
        tail: SqlBlock,
    ): SqlBlock {
        val key = node.element
        return if (key is ExprLit && key.lit.code() == Literal.STRING) {
            val elemString = key.lit.stringValue()
            val stepField = exprPathStepField(Identifier.Simple.delimited(elemString))
            visitPathStepField(stepField, tail)
        } else {
            super.visitPathStepElement(node, tail)
        }
    }

    /**
     *
     */
    override fun visitSelectItemExpr(
        node: SelectItem.Expr,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        // Check if we can omit the `AS` alias
        val expr = node.expr
        val asAlias = node.asAlias
        if (asAlias != null) {
            // only add the alias if the inferred alias is not equal to the `asName`
            if (expr.inferredAlias() != asAlias.text) {
                t = t concat " AS \"${asAlias.text}\""
            }
        }
        return t
    }

    /**
     * Redshift TRIM without specified chars does not need FROM.
     */
    override fun visitExprTrim(
        node: ExprTrim,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [LEADING|TRAILING|BOTH] [chars FROM]
        val trimSpec = node.trimSpec
        val chars = node.chars
        when {
            trimSpec != null && chars != null -> {
                t = t concat trimSpec.name()
                t = t concat " "
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
            trimSpec != null -> {
                t = t concat trimSpec.name()
                t = t concat " " // omit the FROM
            }
            chars != null -> {
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprBag(
        node: ExprBag,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list("(", ")") { node.values }
    }

    /**
     * Type mappings for Redshift
     * Shorten the naming
     * - TIME WITH TIME ZONE -> TIMETZ
     * - TIMESTAMP WITH TIME ZONE -> TIMESTAMPTZ
     */
    override fun visitDataType(
        node: DataType,
        tail: SqlBlock,
    ): SqlBlock {
        return when (node.code()) {
            DataType.TIME_WITH_TIME_ZONE -> tail concat type("TIMETZ", node.precision, gap = true)
            DataType.TIMESTAMP_WITH_TIME_ZONE -> tail concat type("TIMESTAMPTZ", node.precision, gap = true)
            else -> super.visitDataType(node, tail)
        }
    }

    // Private utils (copied from SqlDialect)
    private infix fun SqlBlock.concat(rhs: String): SqlBlock {
        next = SqlBlock.Text(rhs)
        return next!!
    }

    private infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
        next = rhs
        return next!!
    }

    private fun type(
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

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): SqlBlock {
        val kids = children()
        val h = SqlBlock.none()
        var t = h
        kids.forEachIndexed { i, child ->
            t = child.accept(this, t)
            t = if (delimiter != null && (i + 1) < kids.size) t concat delimiter else t
        }
        return SqlBlock.Nest(
            prefix = start,
            postfix = end,
            child = h,
        )
    }
}
