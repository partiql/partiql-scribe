package org.partiql.scribe.targets.trino

import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.sql
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
        return when {
            node.function.identifier.text == "transform" -> {
                // Trino's transform function uses `->` to separate between the element variable and the element expr.
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
            DataType.TIME, DataType.TIME_WITH_TIME_ZONE -> tail concat type("TIME", node.precision, gap = true)
            DataType.TIMESTAMP, DataType.TIMESTAMP_WITH_TIME_ZONE -> tail concat type("TIMESTAMP", node.precision, gap = true)
            else -> super.visitDataType(node, tail)
        }
    }

    override fun visitIntervalQualifier(
        node: IntervalQualifier?,
        ctx: SqlBlock?,
    ): SqlBlock? {
        return super.visitIntervalQualifier(node, ctx)
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

    /**
     * Trino does not support precision and fractional precision components in the output SQL.
     */
    override fun visitIntervalQualifierSingle(
        node: IntervalQualifier.Single,
        tail: SqlBlock,
    ): SqlBlock {
        if (node.precision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Trino does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        if (node.fractionalPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Trino does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output.",
                ),
            )
        }
        return tail concat node.field.name()
    }

    /**
     * Trino does not support precision and fractional precision components in the output SQL.
     */
    override fun visitIntervalQualifierRange(
        node: IntervalQualifier.Range,
        tail: SqlBlock,
    ): SqlBlock {
        val startField = node.startField
        val endField = node.endField
        var datetimeField = startField.name()
        if (node.startFieldPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Trino does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        datetimeField += " TO ${endField.name()}"
        if (node.endFieldFractionalPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Trino does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output. " +
                            "Trino has a default fractional precision `3` for INTERVAL second.",
                ),
            )
        }
        return tail concat datetimeField
    }

    override fun visitExprCast(
        node: ExprCast,
        tail: SqlBlock,
    ): SqlBlock {
        val asType = node.asType
        if (asType.code() == DataType.INTERVAL) {
            val intervalQualifier = asType.intervalQualifier!!
            val isSingle = intervalQualifier is IntervalQualifier.Single
            val isYearMonth =
                intervalQualifier is IntervalQualifier.Range &&
                    intervalQualifier.startField.code() == DatetimeField.YEAR &&
                    intervalQualifier.endField.code() == DatetimeField.MONTH
            val isDaySecond =
                intervalQualifier is IntervalQualifier.Range &&
                    intervalQualifier.startField.code() == DatetimeField.DAY &&
                    intervalQualifier.endField.code() == DatetimeField.SECOND
            if (isSingle || !(isYearMonth || isDaySecond)) {
                listener.report(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                        message =
                            "Trino only supports casting to INTERVAL YEAR TO MONTH and INTERVAL DAY TO SECOND. " +
                                "Receieved: $intervalQualifier",
                    ),
                )
            }
        }
        var t = tail
        t = t concat "CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitDataType(node.asType, t)
        t = t concat ")"
        return t
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
        // types are modeled as text; as we don't want to reflow
        return SqlBlock.Text(t)
    }
}
