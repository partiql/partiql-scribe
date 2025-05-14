package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import java.math.BigDecimal

public open class TrinoDialect : SqlDialect() {
    override fun visitExprSessionAttribute(node: ExprSessionAttribute, tail: SqlBlock): SqlBlock {
        return tail concat node.sessionAttribute.name().lowercase()
    }

    /**
     * Trino does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param tail
     * @return
     */
    override fun visitPathStepElement(node: PathStep.Element, tail: SqlBlock): SqlBlock {
        val key = node.element
        return if (key is ExprLit && key.lit.code() == Literal.STRING) {
            val elemString = key.lit.stringValue()
            val stepField = exprPathStepField(Identifier.Simple.delimited(elemString))
            visitPathStepField(stepField, tail)
        } else {
            super.visitPathStepElement(node, tail)
        }
    }

    override fun visitExprLit(node: ExprLit, tail: SqlBlock): SqlBlock {
        val v = node.lit
        if (v.code() == Literal.INT_NUM && intValueOutOfRange(v.bigDecimalValue())) {
            // CAST('<v>' AS DECIMAL(38,0))
            val lit = Literal.string(v.bigDecimalValue().toString())
            val ast = exprCast(exprLit(lit), DataType.DECIMAL(38, 0))
            return visitExprCast(ast, tail)
        }
        return super.visitExprLit(node, tail)
    }

    private fun intValueOutOfRange(value: BigDecimal): Boolean {
        return value < Long.MIN_VALUE.toBigDecimal() || Long.MAX_VALUE.toBigDecimal() < value
    }

    override fun visitExprCall(node: ExprCall, tail: SqlBlock): SqlBlock {
        var t = tail
        val f = node.function
        // Special case -- DATE_ADD('<datetime_field>', <lhs>, <rhs>) -> DATE_ADD(<datetime_field>, <lhs>, <rhs>)
        // Special case -- DATE_DIFF('<datetime_field>', <lhs>, <rhs>) -> DATE_DIFF(<datetime_field>, <lhs>, <rhs>)
        if (!f.hasQualifier() &&
            (f.identifier.text.uppercase() == "DATE_ADD" || f.identifier.text.uppercase() == "DATE_DIFF") &&
            node.args.size == 3
        ) {
            val start = "("
            t = visitIdentifier(f, t)
            t = t concat list(start) { node.args }
            return t
        }
        return super.visitExprCall(node, tail)
    }

//    override fun visitExprLit(node: ExprLit, tail: SqlBlock): SqlBlock {
//        val v = node.lit
//        if (v is IntValue && intValueOutOfRange(v.value)) {
//            // CAST('<v>' AS DECIMAL(38,0))
//            val lit = stringValue(v.value?.toString())
//            val ast = exprCast(exprLit(lit), typeDecimal(38, 0))
//            return visitExprCast(ast, tail)
//        }
//        return super.visitExprLit(node, tail)
//    }

    override fun visitDataType(node: DataType, tail: SqlBlock): SqlBlock {
        return when (node.code()) {
            DataType.INT2 -> tail concat "SMALLINT"
            DataType.INT4 -> tail concat "INT"
            DataType.INT8 -> tail concat "BIGINT"
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE"
            DataType.STRING -> tail concat "VARCHAR"
            else -> super.visitDataType(node, tail)
        }
    }

    override fun visitExprBag(node: ExprBag, tail: SqlBlock): SqlBlock {
        return tail concat list("(", ")") { node.values }
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

    private fun type(symbol: String, vararg args: Int?, gap: Boolean = false): SqlBlock {
        val p = args.filterNotNull()
        val t = when {
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
