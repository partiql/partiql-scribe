package org.partiql.scribe.targets.spark

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.identifierSimple
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.WithListElement
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.sql
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.removePathRoot
import org.partiql.scribe.sql.utils.type

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
     * Use the Spark backticks as the delimiter for the With query name.
     */
    override fun visitWithListElement(
        node: WithListElement,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = t concat node.queryName.sql()
        t = node.columnList?.let { columns -> list(this, " (", ") ") { columns } } ?: t
        t = t concat " AS "
        t = visitExprWrapped(node.asQuery, t)
        return t
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

    /**
     * For quoted identifiers, change the rewriter to use backticks rather than double-quotes.
     */
    override fun visitIdentifierSimple(
        node: Identifier.Simple,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat node.sql()
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

    // Spark's equivalent for PartiQL's STRUCT type is struct type. Can use the `STRUCT` function to create Spark
    // structs: https://spark.apache.org/docs/latest/api/sql/#struct. Names are provided by using an `AS` alias.
    override fun visitExprStruct(
        node: ExprStruct,
        tail: SqlBlock,
    ): SqlBlock {
        val fieldsAsSparkStructs =
            node.fields.map { field ->
                selectItemExpr(
                    expr = field.value,
                    asAlias =
                        identifierSimple(
                            symbol = (field.name as ExprLit).lit.stringValue(),
                            isRegular = true,
                        ),
                )
            }
        return tail concat list(this, "STRUCT(", ")") { fieldsAsSparkStructs }
    }

    override fun visitExprArray(
        node: ExprArray,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list(this, "ARRAY(", ")") { node.values }
    }

    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
        return when {
            node.function.identifier.text == "transform" -> {
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

    /**
     * Type mappings for Spark
     * INT2 -> SMALLINT
     * INT4 -> INT
     * INT8 -> BIGINT
     * DOUBLE PRECISION -> DOUBLE
     * VARCHAR -> no params given default of 20 (https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/types/VarcharType.html#defaultSize())
     * CHAR -> no params given default of 20 (https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/types/CharType.html#defaultSize())
     * TIMESTAMP -> TIMESTAMP_NTZ; parameters removed
     * TIMESTAMP WITH TIME ZONE -> TIMESTAMP; parameters removed
     * TIME + TIME WITH TIME ZONE -> error since Spark does not have a TIME type
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
            DataType.VARCHAR ->
                when (node.length) {
                    null -> {
                        listener.report(
                            ScribeProblem.simpleInfo(
                                code = ScribeProblem.TRANSLATION_INFO,
                                message =
                                    "SparkSQL requires a precision argument for VARCHAR. " +
                                        "Set to default of 20.",
                            ),
                        )
                        tail concat "VARCHAR(20)"
                    }
                    else -> tail concat "VARCHAR(${node.length})"
                }
            DataType.CHAR ->
                when (node.length) {
                    null -> {
                        listener.report(
                            ScribeProblem.simpleInfo(
                                code = ScribeProblem.TRANSLATION_INFO,
                                message =
                                    "SparkSQL requires a precision argument for CHAR. " +
                                        "Set to default of 20.",
                            ),
                        )
                        tail concat "CHAR(20)"
                    }
                    else -> tail concat "CHAR(${node.length})"
                }
            DataType.TIMESTAMP -> {
                if (node.precision != null) {
                    listener.report(
                        ScribeProblem.simpleInfo(
                            code = ScribeProblem.TRANSLATION_INFO,
                            message =
                                "SparkSQL does not support PartiQL's timestamp precision. " +
                                    "Replaced with TIMESTAMP_NTZ type.",
                        ),
                    )
                }
                tail concat type("TIMESTAMP_NTZ", null, gap = true)
            }
            DataType.TIMESTAMP_WITH_TIME_ZONE -> {
                if (node.precision != null) {
                    listener.report(
                        ScribeProblem.simpleInfo(
                            code = ScribeProblem.TRANSLATION_INFO,
                            message =
                                "SparkSQL does not support PartiQL's timestamp with time zone precision. " +
                                    "Replaced with TIMESTAMP type.",
                        ),
                    )
                }
                tail concat type("TIMESTAMP", null, gap = true)
            }
            DataType.TIME ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                        "TIME type is not supported in Spark.",
                    ),
                )
            DataType.TIME_WITH_TIME_ZONE ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                        "TIME WITH TIME ZONE type is not supported in Spark.",
                    ),
                )
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

    /**
     * Spark does not support precision and fractional precision components in the output SQL.
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
                        "Spark does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        if (node.fractionalPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "Spark does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output.",
                ),
            )
        }
        return tail concat node.field.name()
    }

    /**
     * Spark does not support precision and fractional precision components in the output SQL.
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
                        "Spark does not support a datetime field INTERVAL precision. " +
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
                        "Spark does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output.",
                ),
            )
        }
        return tail concat datetimeField
    }

    // Spark, has no notion of case sensitivity
    // https://spark.apache.org/docs/latest/sql-ref-identifier.html
    private fun Identifier.Simple.sql() = "`$text`"
}
