package org.partiql.scribe.targets.redshift

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Type
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.scribe.sql.SqlBlock
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

/**
 * Redshift SQL dialect for PartiQL Scribe.
 */
public open class RedshiftDialect : SqlDialect() {

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS \"${node.asAlias!!.symbol}\"" else t
        return t
    }

    /**
     * Redshift does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param tail
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(identifierSymbol(
                symbol = (key.value as StringValue).value!!,
                caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
            ))
            visitExprPathStepSymbol(symbol, tail)
        } else {
            super.visitExprPathStepIndex(node, tail)
        }
    }

    // Redshift's equivalent for PartiQL's STRUCT type is SUPER OBJECT. Can use the `OBJECT` function to create SUPER
    // OBJECTs: https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html
    override fun visitExprStruct(node: Expr.Struct, tail: SqlBlock): SqlBlock {
        return tail concat list("OBJECT(", ")") { node.fields }
    }

    override fun visitExprStructField(node: Expr.Struct.Field, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ", "
        t = visitExprWrapped(node.value, t)
        return t
    }

    /**
     * Use (...) syntax for ALL collections.
     */
    override fun visitExprCollection(node: Expr.Collection, tail: SqlBlock): SqlBlock {
        return tail concat list { node.values }
    }

    override fun visitTypeString(node: Type.String, tail: SqlBlock): SqlBlock = tail concat "VARCHAR"

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): SqlBlock {
        val kids = children()
        val h = SqlBlock.root()
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
