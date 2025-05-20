package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.inferredAlias
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.type

public open class RedshiftAstToSql(context: ScribeContext) : AstToSql(context) {
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
     * Remove redundant `AS` aliases for SELECT project items.
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
        return tail concat list(this, "(", ")") { node.values }
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
}
