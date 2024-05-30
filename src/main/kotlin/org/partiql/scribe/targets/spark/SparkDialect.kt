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
import org.partiql.scribe.sql.SqlBlock
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.scribe.sql.sql
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

public open class SparkDialect : SqlDialect() {

    override fun visitFromValue(node: From.Value, tail: SqlBlock): SqlBlock {
        var h = tail
        h = when (node.type) {
            From.Value.Type.SCAN -> h
            else -> error("${node.type} is not unsupported feature by spark")
        }
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        // AT and BY should be stopped by feature validation.
        return h
    }

    override fun visitIdentifier(node: Identifier, tail: SqlBlock): SqlBlock = when (node) {
        is Identifier.Symbol -> tail concat node.sql()
        is Identifier.Qualified -> {
            val path = node.steps.fold(node.root.sql()) { p, step -> p + "." + step.sql() }
            tail concat path
        }
    }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, tail: SqlBlock): SqlBlock {
        var h = tail
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        return h
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(
                identifierSymbol(
                    symbol = (key.value as StringValue).value!!,
                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
                )
            )
            visitExprPathStepSymbol(symbol, tail)
        } else {
            super.visitExprPathStepIndex(node, tail)
        }
    }

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, tail: SqlBlock): SqlBlock {
        return tail concat ".${node.symbol.sql()}"
    }

    // Spark's equivalent for PartiQL's STRUCT type is struct type. Can use the `STRUCT` function to create Spark
    // structs: https://spark.apache.org/docs/latest/api/sql/#struct. Names are provided by using an `AS` alias.
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprStruct(node: Expr.Struct, tail: SqlBlock): SqlBlock {
        val fieldsAsSparkStructs = node.fields.map { field ->
            selectProjectItemExpression(
                expr = field.value, asAlias = identifierSymbol(
                    (((field.name as Expr.Lit).value) as StringValue).string!!,
                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                )
            )
        }
        return tail concat list("STRUCT(", ")") { fieldsAsSparkStructs }
    }

    override fun visitExprCall(node: Expr.Call, tail: SqlBlock): SqlBlock {
        return when {
            node.function is Identifier.Symbol && (node.function as Identifier.Symbol).symbol == "transform" -> {
                // Spark's transform function uses `->` to separate between the element variable and the element expr.
                val arrayExpr = node.args[0].sql(dialect = this)
                val elementVar = node.args[1].sql(dialect = this)
                val elementExpr = node.args[2].sql(dialect = this)
                var h = tail
                h = visitIdentifier(node.function, h)
                h = h concat "($arrayExpr, $elementVar -> $elementExpr)"
                h
            }
            else -> super.visitExprCall(node, tail)
        }
    }

    override fun visitTypeInt2(node: Type.Int2, tail: SqlBlock): SqlBlock = tail concat "SMALLINT"

    override fun visitTypeInt4(node: Type.Int4, tail: SqlBlock): SqlBlock = tail concat "INT"

    override fun visitTypeInt8(node: Type.Int8, tail: SqlBlock): SqlBlock = tail concat "BIGINT"

    override fun visitExprCollection(node: Expr.Collection, tail: SqlBlock): SqlBlock {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.ARRAY -> "array(" to ")"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.SEXP,
            Expr.Collection.Type.BAG,
            Expr.Collection.Type.LIST -> "(" to ")"
        }
        return tail concat list(start, end) { node.values }
    }

    // Spark, has no notion of case sensitivity
    // https://spark.apache.org/docs/latest/sql-ref-identifier.html
    private fun Identifier.Symbol.sql() = "`$symbol`"

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
