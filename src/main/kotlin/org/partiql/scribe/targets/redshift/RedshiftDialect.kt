package org.partiql.scribe.targets.redshift

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

/**
 * Redshift SQL dialect for PartiQL Scribe.
 */
public open class RedshiftDialect : SqlDialect() {

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, head: SqlBlock): SqlBlock {
        var h = head
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS \"${node.asAlias!!.symbol}\"") else h
        return h
    }

    /**
     * Redshift does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param head
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, head: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(identifierSymbol(
                symbol = (key.value as StringValue).value!!,
                caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
            ))
            super.visitExprPathStepSymbol(symbol, head)
        } else {
            super.visitExprPathStepIndex(node, head)
        }
    }

    // Redshift's equivalent for PartiQL's STRUCT type is SUPER OBJECT. Can use the `OBJECT` function to create SUPER
    // OBJECTs: https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html
    override fun visitExprStruct(node: Expr.Struct, head: SqlBlock): SqlBlock {
        return head concat list("OBJECT(", ")") { node.fields }
    }

    override fun visitExprStructField(node: Expr.Struct.Field, head: SqlBlock): SqlBlock {
        var h = head
        h = visitExprWrapped(node.name, h)
        h = h concat r(", ")
        h = visitExprWrapped(node.value, h)
        return h
    }

    private fun r(text: String): SqlBlock = SqlBlock.Text(text)

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): SqlBlock {
        val kids = children()
        var h = start?.let { r(it) } ?: SqlBlock.Nil
        kids.forEachIndexed { i, child ->
            h = child.accept(this, h)
            h = if (delimiter != null && (i + 1) < kids.size) h concat r(delimiter) else h
        }
        h = if (end != null) h concat r(end) else h
        return h
    }
}
