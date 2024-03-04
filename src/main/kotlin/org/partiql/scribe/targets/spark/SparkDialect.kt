package org.partiql.scribe.targets.spark

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Type
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

public open class SparkDialect : SqlDialect() {

    override fun visitFromValue(node: From.Value, head: SqlBlock): SqlBlock {
        var h = head
        h = when (node.type) {
            From.Value.Type.SCAN -> h
            else -> error("${node.type} is not unsupported feature by spark")
        }
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS ${node.asAlias!!.sql()}") else h
        // AT and BY should be stopped by feature validation.
        return h
    }

    override fun visitIdentifier(node: Identifier, head: SqlBlock): SqlBlock =
        when (node) {
            is Identifier.Symbol -> head concat node.sql()
            is Identifier.Qualified -> {
                val path = node.steps.fold(node.root.sql()) { p, step -> p + "." + step.sql() }
                head concat r(path)
            }
        }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, head: SqlBlock): SqlBlock {
        var h = head
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS ${node.asAlias!!.sql()}") else h
        return h
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, head: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(
                identifierSymbol(
                    symbol = (key.value as StringValue).value!!,
                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
                )
            )
            visitExprPathStepSymbol(symbol, head)
        } else {
            super.visitExprPathStepIndex(node, head)
        }
    }

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, head: SqlBlock): SqlBlock {
        return head concat r(".${node.symbol.sql()}")
    }

    // Spark's equivalent for PartiQL's STRUCT type is struct type. Can use the `STRUCT` function to create Spark
    // structs: https://spark.apache.org/docs/latest/api/sql/#struct. Names are provided by using an `AS` alias.
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprStruct(node: Expr.Struct, head: SqlBlock): SqlBlock {
        val fieldsAsSparkStructs = node.fields.map { field ->
            selectProjectItemExpression(
                expr = field.value,
                asAlias = identifierSymbol((((field.name as Expr.Lit).value) as StringValue).string!!, caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE)
            )
        }
        return head concat list("STRUCT(", ")") { fieldsAsSparkStructs }
    }

    override fun visitExprCall(node: Expr.Call, head: SqlBlock): SqlBlock {
        return when {
            node.function is Identifier.Symbol && (node.function as Identifier.Symbol).symbol == "transform" -> {
                // Spark's transform function uses `->` to separate between the element variable and the element expr.
                val arrayExpr = visitExpr(node.args[0], SqlBlock.Nil)
                val elementVar = visitExpr(node.args[1], SqlBlock.Nil)
                val elementExpr = visitExpr(node.args[2], SqlBlock.Nil)
                var h = head
                h = visitIdentifier(node.function, h)
                h = h concat "($arrayExpr, $elementVar -> $elementExpr)"
                h
            }
            else -> super.visitExprCall(node, head)
        }
    }

    override fun visitTypeInt2(node: Type.Int2, head: SqlBlock): SqlBlock = head concat r("SMALLINT")

    override fun visitTypeInt4(node: Type.Int4, head: SqlBlock): SqlBlock = head concat r("INT")

    override fun visitTypeInt8(node: Type.Int8, head: SqlBlock): SqlBlock = head concat r("BIGINT")

    override fun visitExprUnary(node: Expr.Unary, head: SqlBlock): SqlBlock {
        val op = when (node.op) {
            Expr.Unary.Op.NOT -> "NOT ("
            Expr.Unary.Op.POS -> "+("
            Expr.Unary.Op.NEG -> "-("
        }
        var h = head
        h = h concat r(op)
        h = visitExprWrapped(node.expr, h)
        h = h concat r(")")
        return h
    }

    private fun r(text: String): SqlBlock = SqlBlock.Text(text)

    // Spark, has no sense of case sensitivity
    // https://spark.apache.org/docs/latest/sql-ref-identifier.html
    private fun Identifier.Symbol.sql() = "`$symbol`"

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
