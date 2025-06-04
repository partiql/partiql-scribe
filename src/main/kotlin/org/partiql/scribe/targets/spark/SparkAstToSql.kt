package org.partiql.scribe.targets.spark

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.sql.SqlBlock
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.removePathRoot

public open class SparkAstToSql(context: ScribeContext) : AstToSql(context) {
    private val listener = context.getProblemListener()

    override fun visitFromExpr(
        node: FromExpr,
        tail: SqlBlock,
    ): SqlBlock {
        var h = tail
        h =
            when (node.fromType.code()) {
                FromType.SCAN -> h
                else ->
                    // Filter out unsupported `FromType`s such as `FromType.UNPIVOT`
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                            "${node.fromType.name()} is an unsupported feature by spark",
                        ),
                    )
            }
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        // AT and BY should be stopped by feature validation.
        return h
    }

    /**
     * For quoted identifiers change the rewriter to use backticks rather than double-quotes.
     */
    override fun visitIdentifier(
        node: Identifier,
        tail: SqlBlock,
    ): SqlBlock {
        val path =
            when (node.hasQualifier()) {
                true -> {
                    val qualifier = node.qualifier.fold("") { acc, part -> acc + "${part.sql()}." }
                    qualifier + node.identifier.sql()
                }
                false -> node.identifier.sql()
            }
        return tail concat path
    }

    override fun visitSelectItemExpr(
        node: SelectItem.Expr,
        tail: SqlBlock,
    ): SqlBlock {
        var h = tail
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        return h
    }

    /**
     * Spark does not support x['y'] syntax; replace with x.y
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
                        "SparkSQL does not support PartiQL's path element syntax (e.g. x['y']). " +
                            "Replaced with path step field syntax (e.g. x.y)",
                ),
            )
            val elemString = key.lit.stringValue()
            val stepField = exprPathStepField(Identifier.Simple.regular(elemString))
            visitPathStepField(stepField, tail)
        } else {
            super.visitPathStepElement(node, tail)
        }
    }

    override fun visitPathStepField(
        node: PathStep.Field,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat ".${node.field.sql()}"
    }

    /**
     * Type mappings for Spark
     * INT2 -> SMALLINT
     * INT4 -> INT
     * INT8 -> BIGINT
     * DOUBLE_PRECISION -> DOUBLE
     */
    override fun visitDataType(
        node: DataType,
        tail: SqlBlock,
    ): SqlBlock {
        return when (node.code()) {
            DataType.INT2 -> tail concat "SMALLINT"
            DataType.INT4 -> tail concat "INT"
            DataType.INT8 -> tail concat "BIGINT"
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE"
            else -> super.visitDataType(node, tail)
        }
    }

    override fun visitExprBag(
        node: ExprBag,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list(this, "(", ")") { node.values }
    }

    override fun visitLiteral(
        node: Literal,
        ctx: SqlBlock,
    ): SqlBlock {
        if (node.code() == Literal.TYPED_STRING) {
            if (node.dataType().code() == DataType.TIME || node.dataType().code() == DataType.TIME_WITH_TIME_ZONE) {
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                        "Time type is not supported in Spark. Trying to convert literal ${node.stringValue()}",
                    ),
                )
            }
        }
        return super.visitLiteral(node, ctx)
    }

    override fun visitExprTrim(
        node: ExprTrim,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        val trimSpec = node.trimSpec
        if (node.chars == null) {
            // special forms
            t = t concat
                when (trimSpec?.code()) {
                    TrimSpec.BOTH -> "trim("
                    TrimSpec.LEADING -> "ltrim("
                    TrimSpec.TRAILING -> "rtrim("
                    else -> "trim("
                }
        } else {
            t = t concat "trim("
            t = t concat if (trimSpec == null) "BOTH" else trimSpec.name()
            t = t concat " "
            t = visitExprWrapped(node.chars!!, t)
            t = t concat " FROM "
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    /**
     * The PartiQL plan and thus AST will fully-qualify ORDER BY variable references and paths with an implicit
     * binding tuple name for set operations. For Spark, there is no implicit binding tuple name for the set ops. So
     * we must remove the prefix binding tuple name from paths.
     *
     * For example, the query
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY a
     * will have an extra qualification
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY "_1".a
     * The additional qualification is invalid SparkSQL, hence why we remove it.
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

    // Spark, has no notion of case sensitivity
    // https://spark.apache.org/docs/latest/sql-ref-identifier.html
    private fun Identifier.Simple.sql() = "`$text`"
}
