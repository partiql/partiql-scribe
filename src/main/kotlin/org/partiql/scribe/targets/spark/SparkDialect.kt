package org.partiql.scribe.targets.spark

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect

public class SparkDialect : SqlDialect() {
    override fun visitFromExpr(node: FromExpr, tail: SqlBlock): SqlBlock {
        var h = tail
        h = when (node.fromType.code()) {
            FromType.SCAN -> h
            else -> error("${node.fromType.name()} is an unsupported feature by spark")
        }
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        // AT and BY should be stopped by feature validation.
        return h
    }

    override fun visitIdentifier(node: Identifier, tail: SqlBlock): SqlBlock {
        val path = when (node.hasQualifier()) {
            true -> {
                val qualifier = node.qualifier.fold("") { acc, part -> acc + "${part.sql()}." }
                qualifier + node.identifier.sql()
            }
            false -> node.identifier.sql()
        }
        return tail concat path
    }

    override fun visitSelectItemExpr(node: SelectItem.Expr, tail: SqlBlock): SqlBlock {
        var h = tail
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        return h
    }

    override fun visitPathStepElement(node: PathStep.Element, tail: SqlBlock): SqlBlock {
        val key = node.element
        return if (key is ExprLit && key.lit.code() == Literal.STRING) {
            val elemString = key.lit.stringValue()
            val stepField = exprPathStepField(Identifier.Simple.regular(elemString))
            visitPathStepField(stepField, tail)
        } else {
            super.visitPathStepElement(node, tail)
        }
    }

    override fun visitPathStepField(node: PathStep.Field, tail: SqlBlock): SqlBlock {
        return tail concat ".${node.field.sql()}"
    }

    /**
     * Type mappings for Spark
     * INT2 -> SMALLINT
     * INT4 -> INT
     * INT8 -> BIGINT
     * DOUBLE_PRECISION -> DOUBLE
     */
    override fun visitDataType(node: DataType, tail: SqlBlock): SqlBlock {
        return when (node.code()) {
            DataType.INT2 -> tail concat "SMALLINT"
            DataType.INT4 -> tail concat "INT"
            DataType.INT8 -> tail concat "BIGINT"
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE"
            else -> super.visitDataType(node, tail)
        }
    }

    override fun visitExprBag(node: ExprBag, tail: SqlBlock): SqlBlock {
        return tail concat list("(", ")") { node.values }
    }

    override fun visitLiteral(node: Literal, ctx: SqlBlock): SqlBlock {
        if (node.code() == Literal.TYPED_STRING) {
            if (node.dataType().code() == DataType.TIME || node.dataType().code() == DataType.TIME_WITH_TIME_ZONE) {
                error("Attempting to print ${node.stringValue()}. Time type is not supported in Spark.")
            }
        }
        return super.visitLiteral(node, ctx)
    }

    override fun visitExprTrim(node: ExprTrim, tail: SqlBlock): SqlBlock {
        var t = tail
        val trimSpec = node.trimSpec
        if (node.chars == null) {
            // special forms
            t = t concat when (trimSpec?.code()) {
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

    // Spark, has no notion of case sensitivity
    // https://spark.apache.org/docs/latest/sql-ref-identifier.html
    private fun Identifier.Simple.sql() = "`$text`"

    // Private utils (copied from SqlDialect)
    private infix fun SqlBlock.concat(rhs: String): SqlBlock {
        next = SqlBlock.Text(rhs)
        return next!!
    }

    private infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
        next = rhs
        return next!!
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
