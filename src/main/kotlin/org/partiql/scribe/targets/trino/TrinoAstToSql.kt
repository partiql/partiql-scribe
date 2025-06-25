package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.removePathRoot
import java.math.BigDecimal

public open class TrinoAstToSql(context: ScribeContext) : AstToSql(context) {
    private val listener = context.getProblemListener()

    override fun visitExprSessionAttribute(
        node: ExprSessionAttribute,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat node.sessionAttribute.name().lowercase()
    }

    /**
     * Trino does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param tail
     * @return
     */
    override fun visitPathStepElement(
        node: PathStep.Element,
        tail: SqlBlock,
    ): SqlBlock {
        val key = node.element
        return if (key is ExprLit && key.lit.code() == Literal.STRING) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Trino does not support PartiQL's path element syntax (e.g. x['y']). " +
                            "Replaced with path step field syntax (e.g. x.y)",
                ),
            )
            val elemString = key.lit.stringValue()
            val stepField = exprPathStepField(Identifier.Simple.delimited(elemString))
            visitPathStepField(stepField, tail)
        } else {
            super.visitPathStepElement(node, tail)
        }
    }

    override fun visitExprLit(
        node: ExprLit,
        tail: SqlBlock,
    ): SqlBlock {
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

    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
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
            t = t concat list(this, start) { node.args }
            return t
        }
        return super.visitExprCall(node, tail)
    }

    /**
     * Trino-specific type conversions
     * BOOL -> BOOLEAN
     * INT2 -> SMALLINT
     * INT4 -> INT
     * INT8 -> BIGINT
     * DOUBLE PRECISION -> DOUBLE
     * STRING -> VARCHAR
     */
    override fun visitDataType(
        node: DataType,
        tail: SqlBlock,
    ): SqlBlock {
        return when (node.code()) {
            DataType.BOOL -> tail concat "BOOLEAN"
            DataType.INT2 -> tail concat "SMALLINT"
            DataType.INT4 -> tail concat "INT"
            DataType.INT8 -> tail concat "BIGINT"
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE"
            DataType.STRING -> tail concat "VARCHAR"
            else -> super.visitDataType(node, tail)
        }
    }

    override fun visitExprBag(
        node: ExprBag,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list(this, "(", ")") { node.values }
    }

    /**
     * The PartiQL plan and thus AST will fully-qualify ORDER BY variable references and paths with an implicit
     * binding tuple name for set operations. For Trino, there is no implicit binding tuple name for the set ops. So
     * we must remove the prefix binding tuple name from paths.
     *
     * For example, the query
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY a
     * will have an extra qualification
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY "_1".a
     * The additional qualification is invalid Trino, hence why we remove it.
     */
    override fun visitExprQuerySet(
        node: ExprQuerySet,
        tail: SqlBlock,
    ): SqlBlock {
        if (node.body is QueryBody.SetOp && node.orderBy != null) {
            val orderBy = node.orderBy!!
            val newSorts =
                orderBy.sorts.map { sort ->
                    val newExpr = removePathRoot(sort.expr)
                    sort(newExpr, sort.order, sort.nulls)
                }
            val newNode =
                exprQuerySet(
                    body = node.body,
                    limit = node.limit,
                    offset = node.offset,
                    orderBy = orderBy(newSorts),
                )
            return super.visitExprQuerySet(newNode, tail)
        }
        return super.visitExprQuerySet(node, tail)
    }
}
