package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.PathStep
import org.partiql.ast.sql.SqlBlock
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.inferredAlias
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.removePathRoot
import org.partiql.scribe.sql.utils.type

public open class RedshiftAstToSql(context: ScribeContext) : AstToSql(context) {
    private val listener = context.getProblemListener()

    /**
     * Redshift does not support x['y'] syntax; replace with x.y
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
                        "Redshift does not support PartiQL's path element syntax (e.g. x['y']). " +
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

    // Redshift's equivalent for PartiQL's STRUCT type is SUPER OBJECT. Can use the `OBJECT` function to create SUPER
    // OBJECTs: https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html
    override fun visitExprStruct(
        node: ExprStruct,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list(this, "OBJECT(", ")") { node.fields }
    }

    override fun visitExprStructField(
        node: ExprStruct.Field,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ", "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
        val fn = node.function
        return when {
            // https://docs.aws.amazon.com/redshift/latest/dg/r_object_transform_function.html
            // Function has a special form:
            // OBJECT_TRANSFORM(
            //     <input SUPER OBJECT>                         // arg[0]
            //     KEEP                                         // arg[1]
            //         <keep path string 1>,
            //         ...
            //         <keep path string m>
            //     [SET (optional)                              // arg[2]
            //         <set path string 1>, <set path value 1>,
            //         ...
            //         <set path string n>, <set path value n>]
            // )
            fn.identifier.text == "OBJECT_TRANSFORM" -> {
                val input = node.args[0]
                val keepPaths = (node.args[1] as ExprArray).values
                val setPaths = (node.args[2] as ExprArray).values
                var t = tail
                t = t concat "OBJECT_TRANSFORM("
                t = visitExprWrapped(input, t)
                t = t concat list(this, start = " KEEP ", end = "") { keepPaths }
                if (setPaths.isNotEmpty()) {
                    // no need for an empty block, just don't link it
                    t = t concat list(this, start = " SET ", end = "") { setPaths }
                }
                t = t concat ")"
                t
            }
            else -> super.visitExprCall(node, tail)
        }
    }

    /**
     * Remove redundant `AS` aliases for SELECT project items.
     */
    override fun visitSelectItemExpr(
        node: SelectItem.Expr,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        // Check if we can omit the `AS` alias
        val expr = node.expr
        val asAlias = node.asAlias
        if (asAlias != null) {
            // only add the alias if the inferred alias is not equal to the `asName`
            if (expr.inferredAlias() != asAlias.text) {
                t = t concat " AS \"${asAlias.text}\""
            }
        }
        return t
    }

    /**
     * Redshift TRIM without specified chars does not need FROM.
     */
    override fun visitExprTrim(
        node: ExprTrim,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [LEADING|TRAILING|BOTH] [chars FROM]
        val trimSpec = node.trimSpec
        val chars = node.chars
        when {
            trimSpec != null && chars != null -> {
                t = t concat trimSpec.name()
                t = t concat " "
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
            trimSpec != null -> {
                t = t concat trimSpec.name()
                t = t concat " " // omit the FROM
            }
            chars != null -> {
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprBag(
        node: ExprBag,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat list(this, "(", ")") { node.values }
    }

    /**
     * Type mappings for Redshift
     * - STRING -> VARCHAR(65535)
     * - TINYINT -> error
     */
    override fun visitDataType(
        node: DataType,
        tail: SqlBlock,
    ): SqlBlock {
        return when (node.code()) {
            DataType.TINYINT ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                        message = "Redshift does not support TINYINT type.",
                    ),
                )
            DataType.STRING -> {
                listener.report(
                    ScribeProblem.simpleInfo(
                        code = ScribeProblem.TRANSLATION_INFO,
                        message =
                            "Redshift does not support PartiQL's STRING type. " +
                                "Replaced with VARCHAR(65535).",
                    ),
                )
                tail concat type("VARCHAR", 65535, gap = false)
            }
            else -> super.visitDataType(node, tail)
        }
    }

    /**
     * The PartiQL plan and thus AST will fully-qualify ORDER BY variable references and paths with an implicit
     * binding tuple name for set operations. For Redshift, there is no implicit binding tuple name for the set ops. So
     * we must remove the prefix binding tuple name from paths.
     *
     * For example, the query
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY a
     * will have an extra qualification
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY "_1".a
     * The additional qualification is invalid Redshift, hence why we remove it.
     *
     * Additionally, Redshift does not support path expressions in ORDER BY applied to set operations. See
     * #[order-by-10] for an example of such an unsupported query.
     */
    override fun visitExprQuerySet(
        node: ExprQuerySet,
        tail: SqlBlock,
    ): SqlBlock {
        if (node.body is QueryBody.SetOp && node.orderBy != null) {
            val orderBy = node.orderBy!!
            val newSorts =
                orderBy.sorts.map { sort ->
                    val sortExpr = sort.expr
                    if (sortExpr is ExprPath && sortExpr.steps.size > 1) {
                        listener.report(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                                message =
                                    "Redshift does not support path expressions in the ORDER BY clause when " +
                                        "applied to a set operation.",
                            ),
                        )
                    }
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
     * Redshift supports fractional precision for an interval which contains SECOND
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
                        "Redshift does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }

        var intervalField =  node.field.name()

        if(node.fractionalPrecision != null) {
            intervalField += " (${node.fractionalPrecision})"
        }

        return tail concat intervalField
    }

    /**
     * Redshift supports fractional precision for an interval which contains SECOND
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
                        "Redshift does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        datetimeField += " TO ${endField.name()}"

        if(node.endFieldFractionalPrecision != null) {
            datetimeField += " (${node.endFieldFractionalPrecision})"
        }
        return tail concat datetimeField
    }
}
