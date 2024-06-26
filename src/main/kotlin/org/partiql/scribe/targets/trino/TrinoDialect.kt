package org.partiql.scribe.targets.trino

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Type
import org.partiql.ast.exprCast
import org.partiql.ast.exprLit
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.typeDecimal
import org.partiql.scribe.sql.SqlBlock
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.scribe.sql.sql
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.stringValue
import java.math.BigInteger

public open class TrinoDialect : SqlDialect() {
    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, tail: SqlBlock): SqlBlock {
        return tail concat node.attribute.name.lowercase()
    }

    /**
     * Trino does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param tail
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(
                identifierSymbol(
                    symbol = (key.value as StringValue).value!!,
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
                )
            )
            visitExprPathStepSymbol(symbol, tail)
        } else {
            super.visitExprPathStepIndex(node, tail)
        }
    }


    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: Expr.Lit, tail: SqlBlock): SqlBlock {
        val v = node.value
        if (v is IntValue && intValueOutOfRange(v.value)) {
            // CAST('<v>' AS DECIMAL(38,0))
            val lit = stringValue(v.value?.toString())
            val ast = exprCast(exprLit(lit), typeDecimal(38, 0))
            return visitExprCast(ast, tail)
        }
        return super.visitExprLit(node, tail)
    }

    private fun intValueOutOfRange(value: BigInteger?): Boolean {
        if (value == null) {
            return false
        }
        return value < Constants.INT_LITERAL_MIN_VALUE || Constants.INT_LITERAL_MAX_VALUE < value
    }

    /**
     * Trino's equivalent for PartiQL's struct is the ROW type. It can be created either by
     * 1. a SELECT projection
     *  e.g. Trino: (SELECT 1 AS x, 2 AS y) == PartiQL: { 'x': 1, 'y': 2}
     * 2. creating a ROW and casting the ROW with the column names
     *  e.g. Trino: CAST(ROW(1, 2) AS ROW(x INTEGER, y INTEGER)) == PartiQL: { 'x': 1, 'y': 2 }
     *
     * Option 1 is often the easiest way to create a ROW but due to subquery scalar coercion, if only a single ROW is
     * projected, the output value will be the single value not in a ROW.
     *  e.g. Trino: (SELECT 1 AS x) => 1
     * We encode this as a `SELECT` list with field names defined using an `AS` alias.
     *
     * Option 2 requires additional type information for the CAST ROW call. In [TrinoTarget], the
     * `CAST(ROW(...) AS ROW(...))` is encoded as a scalar fn call, `cast_row` with the type information
     * (i.e. everything following the `AS` in the `CAST`) encoded as a string literal.
     * [TrinoTarget] should have already converted these singleton ROWs to CAST(ROW(...) AS ...) calls.
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprStruct(node: Expr.Struct, tail: SqlBlock): SqlBlock {
        // node.fields.size == 0 is currently an error since Trino does not currently allow empty ROWs
        // node.fields.size == 1 already covered by CAST(ROW(...) AS ...) calls
        assert(node.fields.size > 1)
        val fieldsAsItems = node.fields.map { field ->
            selectProjectItemExpression(
                expr = field.value, asAlias = identifierSymbol(
                    (((field.name as Expr.Lit).value) as StringValue).string!!,
                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                )
            )
        }
        return tail concat list("(SELECT ", ")") { fieldsAsItems }
    }

    override fun visitTypeCustom(node: Type.Custom, tail: SqlBlock): SqlBlock = tail concat node.name

    override fun visitExprCall(node: Expr.Call, tail: SqlBlock): SqlBlock {
        return when {
            // Trino's transform function uses `->` to separate between the element variable and the element expr.
            node.function is Identifier.Symbol && (node.function as Identifier.Symbol).symbol == "transform" -> {
                val arrayExpr = node.args[0].sql(dialect = this)
                val elementVar = node.args[1].sql(dialect = this)
                val elementExpr = node.args[2].sql(dialect = this)
                var t = tail
                t = visitIdentifier(node.function, t)
                t = t concat "($arrayExpr, $elementVar -> $elementExpr)"
                t
            }
            else -> super.visitExprCall(node, tail)
        }
    }

    override fun visitTypeInt2(node: Type.Int2, tail: SqlBlock): SqlBlock = tail concat "SMALLINT"

    override fun visitTypeInt4(node: Type.Int4, tail: SqlBlock): SqlBlock = tail concat "INT"

    override fun visitTypeInt8(node: Type.Int8, tail: SqlBlock): SqlBlock = tail concat "BIGINT"

    override fun visitTypeFloat64(node: Type.Float64, tail: SqlBlock): SqlBlock = tail concat "DOUBLE"

    override fun visitTypeString(node: Type.String, tail: SqlBlock): SqlBlock = tail concat "VARCHAR"

    override fun visitExprCollection(node: Expr.Collection, tail: SqlBlock): SqlBlock {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.ARRAY -> "ARRAY[" to "]"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.SEXP,
            Expr.Collection.Type.BAG,
            Expr.Collection.Type.LIST -> "(" to ")"
        }
        return tail concat list(start, end) { node.values }
    }

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

    private object Constants {
        val INT_LITERAL_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE)
        val INT_LITERAL_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE)
    }
}
