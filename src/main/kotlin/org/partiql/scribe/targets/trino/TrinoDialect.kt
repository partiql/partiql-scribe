package org.partiql.scribe.targets.trino

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Type
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

public object TrinoDialect : SqlDialect() {

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, head: SqlBlock): SqlBlock {
        return SqlBlock.Link(head, SqlBlock.Text(node.attribute.name.lowercase()))
    }

    /**
     * Trino does not support x['y'] syntax; replace with x.y.
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

    /**
     * Trino's equivalent for struct is the ROW type. It can be created either by
     * 1. a SELECT projection
     *  e.g. Trino: (SELECT 1 AS x, 2 AS y) == PartiQL: { 'x': 1, 'y': 2}
     * 2. creating a ROW and casting the ROW with the column names
     *  e.g. Trino: CAST(ROW(1, 2) AS ROW(x INTEGER, y INTEGER)) == PartiQL: { 'x': 1, 'y': 2 }
     *
     * Option 1 is often the easiest way to create a ROW but due to subquery scalar coercion, if only a single ROW is
     * projected, the output value will be the single value not in a ROW.
     *  e.g. Trino: (SELECT 1 AS x) => 1
     * [TrinoTarget] should have caught any instance of option 1 before reaching this stage.
     *
     * Option 2 requires additional type information for the ROW CAST. In [TrinoTarget], the
     * `CAST(ROW(...) AS ROW(...))` is encoded as a scalar fn call, `cast_row` with the type information
     * (i.e. everything following the `AS` in the `CAST`) encoded as a string literal.
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprStruct(node: Expr.Struct, head: SqlBlock): SqlBlock {
        // TODO figure out how to output empty Trino ROW
        assert(node.fields.size != 1)
        val fieldsAsItems = node.fields.map { field ->
            selectProjectItemExpression(
                expr = field.value,
                asAlias = identifierSymbol((((field.name as Expr.Lit).value) as StringValue).string!!, caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE)
            )
        }
        return head concat list("(SELECT ", ")") { fieldsAsItems }
    }

    override fun visitTypeCustom(node: Type.Custom, head: SqlBlock): SqlBlock = head concat r(node.name)

    override fun visitExprCall(node: Expr.Call, head: SqlBlock): SqlBlock {
        return when {
            node.function is Identifier.Symbol && (node.function as Identifier.Symbol).symbol == "transform"-> {
                val array = visitExpr(node.args[0], SqlBlock.Nil)
                val varName = visitExpr(node.args[1], SqlBlock.Nil)
                val lambda = visitExpr(node.args[2], SqlBlock.Nil)
                var h = head
                h = visitIdentifier(node.function, h)
                h = h concat "($array, $varName -> $lambda)"
                h
            }
            else -> super.visitExprCall(node, head)
        }
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
