package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprMap
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.sql
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list
import org.partiql.scribe.sql.utils.removePathRoot
import java.math.BigDecimal

/**
 * AST -> SQL text rendering for the DuckDB dialect.
 */
public open class DuckDBAstToSql(context: ScribeContext) : AstToSql(context) {
    private val listener = context.getProblemListener()

    override fun visitFrom(
        node: org.partiql.ast.From,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        node.tableRefs.forEachIndexed { i, ref ->
            if (i > 0) {
                val isUnnest =
                    ref is org.partiql.ast.FromExpr && ref.expr is ExprCall &&
                        (ref.expr as ExprCall).function.identifier.getText() == DuckDBRelConverter.MARKER_UNNEST
                if (isUnnest) {
                    t = t concat " CROSS JOIN "
                } else {
                    t = t concat ", "
                }
            }
            t = ref.accept(this, t)
        }
        return t
    }

    override fun visitFromExpr(
        node: org.partiql.ast.FromExpr,
        tail: SqlBlock,
    ): SqlBlock {
        var h = tail
        val expr = node.expr
        // UNNEST: render as UNNEST(expr) AS _alias(item)
        if (expr is ExprCall && expr.function.identifier.getText() == DuckDBRelConverter.MARKER_UNNEST) {
            h = visitExprWrapped(expr, h)
            val tableAlias = node.asAlias!!.sql()
            val itemAlias = Identifier.Simple.delimited(node.asAlias!!.getText().removePrefix("_")).sql()
            h = h concat " AS $tableAlias($itemAlias)"
            return h
        }
        // LATERAL: render as LATERAL (subquery) AS alias
        if (expr is ExprCall && expr.function.identifier.getText() == DuckDBRelConverter.MARKER_LATERAL) {
            h = h concat "LATERAL "
            h = visitExprWrapped(expr.args[0], h)
            h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
            return h
        }
        // Default rendering
        h = visitExprWrapped(node.expr, h)
        h = if (node.asAlias != null) h concat " AS ${node.asAlias!!.sql()}" else h
        return h
    }

    override fun visitExprSessionAttribute(
        node: ExprSessionAttribute,
        tail: SqlBlock,
    ): SqlBlock {
        val name = node.sessionAttribute.name()
        // `current_user` / `current_date` as bare keywords collide with an auto-derived output alias of the
        // same name ("Column ... cannot be referenced before it is defined"). DuckDB accepts the parenthesized
        // function forms `current_user()` / `current_date()` which avoid the collision. Note `current_timestamp`
        // does NOT accept parentheses, so only these two are rewritten.
        return when (name) {
            "CURRENT_USER", "CURRENT_DATE" -> tail concat "${name.lowercase()}()"
            else -> tail concat name.lowercase()
        }
    }

    /**
     * DuckDB has no `IS <type>` predicate, and `typeof`-based rewrites cannot faithfully match PartiQL type semantics
     * (e.g. parameterized and nested types), so `IS <type>` is rejected.
     */
    override fun visitExprIsType(
        node: ExprIsType,
        tail: SqlBlock,
    ): SqlBlock {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                "DuckDB does not support `IS ${node.type.name()}`.",
            ),
        )
    }

    override fun visitExprLit(
        node: ExprLit,
        tail: SqlBlock,
    ): SqlBlock {
        val v = node.lit
        var t = tail
        if (v.code() == Literal.INT_NUM && intValueOutOfRange(v.bigDecimalValue())) {
            // CAST('<v>' AS DECIMAL(38,0))
            val lit = Literal.string(v.bigDecimalValue().toString())
            val ast = exprCast(exprLit(lit), DataType.DECIMAL(38, 0))
            return visitExprCast(ast, tail)
        }

        if (v.code() == Literal.TYPED_STRING) {
            val lit = node.lit
            val dataType = lit.dataType().code()
            // DuckDB rejects range interval literals of the form `INTERVAL '..' <field> TO <field>`. Rewrite
            // them to a single verbose interval string, e.g. `INTERVAL '10 years 3 months'`. Single-field
            // interval literals (e.g. `INTERVAL '3' YEAR`) are accepted as-is and fall through to super.
            if (dataType == DataType.INTERVAL) {
                val qualifier = lit.dataType().intervalQualifier
                if (qualifier is IntervalQualifier.Range) {
                    val verbose = duckDBRangeIntervalString(lit.stringValue(), qualifier)
                    if (verbose != null) {
                        reportOmittedRangePrecision(qualifier)
                        return t concat "INTERVAL '$verbose'"
                    }
                }
            }
            // A time-with-timezone value must use the `TIMETZ` keyword: DuckDB's `TIME` keyword silently
            // drops the offset and yields a plain TIME, which then fails to compare against a TIMETZ value.
            if (dataType == DataType.TIME_WITH_TIME_ZONE) {
                t = t concat String.format("TIMETZ '%s'", lit.stringValue())
                return t
            }
            if (dataType == DataType.TIME) {
                // DuckDB does not support precision in TIME literal.
                t = t concat String.format("TIME '%s'", lit.stringValue())
                return t
            }

            // Likewise, a timestamp-with-timezone value must use the `TIMESTAMPTZ` keyword.
            if (dataType == DataType.TIMESTAMP_WITH_TIME_ZONE) {
                t = t concat String.format("TIMESTAMPTZ '%s'", lit.stringValue())
                return t
            }
            if (dataType == DataType.TIMESTAMP) {
                // DuckDB does not support precision in TIMESTAMP literal.
                t = t concat String.format("TIMESTAMP '%s'", lit.stringValue())
                return t
            }
        }
        return super.visitExprLit(node, tail)
    }

    private fun intValueOutOfRange(value: BigDecimal): Boolean {
        return value < Long.MIN_VALUE.toBigDecimal() || Long.MAX_VALUE.toBigDecimal() < value
    }

    /**
     * Ordered datetime fields and their DuckDB verbose-interval unit names.
     */
    private val intervalUnits =
        listOf(
            DatetimeField.YEAR to "years",
            DatetimeField.MONTH to "months",
            DatetimeField.DAY to "days",
            DatetimeField.HOUR to "hours",
            DatetimeField.MINUTE to "minutes",
            DatetimeField.SECOND to "seconds",
        )

    /**
     * Converts a range interval literal value + qualifier into a DuckDB verbose interval string.
     * e.g. ("10-3", YEAR TO MONTH) -> "10 years 3 months"; ("-10 3", DAY TO HOUR) -> "-10 days -3 hours".
     * A leading '-' applies the sign to every component. Returns null if the value cannot be parsed to the
     * exact number of fields in the qualifier, so the caller can fall back to the default rendering.
     */
    private fun duckDBRangeIntervalString(
        value: String,
        qualifier: IntervalQualifier.Range,
    ): String? {
        val startCode = qualifier.startField.code()
        val endCode = qualifier.endField.code()
        val startIdx = intervalUnits.indexOfFirst { it.first == startCode }
        val endIdx = intervalUnits.indexOfFirst { it.first == endCode }
        if (startIdx < 0 || endIdx < 0 || endIdx < startIdx) {
            return null
        }
        val fields = intervalUnits.subList(startIdx, endIdx + 1)

        val negative = value.startsWith("-")
        val body = if (negative) value.substring(1) else value

        val amounts: List<String> =
            when (startCode) {
                DatetimeField.YEAR -> body.split("-")
                DatetimeField.DAY -> {
                    // "<days> <hh[:mm[:ss]]>"
                    val parts = body.trim().split(Regex("\\s+"), limit = 2)
                    val rest = if (parts.size > 1) parts[1].split(":") else emptyList()
                    listOf(parts[0]) + rest
                }
                // HOUR/MINUTE start: colon-separated time components
                else -> body.split(":")
            }

        if (amounts.size != fields.size) {
            return null
        }
        return fields.mapIndexed { i, (_, unit) ->
            val amt = amounts[i].trim()
            val signed = if (negative && !amt.startsWith("-")) "-$amt" else amt
            "$signed $unit"
        }.joinToString(" ")
    }

    /**
     * DuckDB does not support precision and fractional precision components in the output SQL.
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
                        "DuckDB does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        if (node.fractionalPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "DuckDB does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output.",
                ),
            )
        }
        return tail concat node.field.name()
    }

    /**
     * DuckDB does not support precision and fractional precision components in the output SQL.
     */
    override fun visitIntervalQualifierRange(
        node: IntervalQualifier.Range,
        tail: SqlBlock,
    ): SqlBlock {
        reportOmittedRangePrecision(node)
        return tail concat "${node.startField.name()} TO ${node.endField.name()}"
    }

    /**
     * Reports a [ScribeProblem.TRANSLATION_INFO] for each range INTERVAL qualifier precision that is omitted from the
     * DuckDB output.
     */
    private fun reportOmittedRangePrecision(node: IntervalQualifier.Range) {
        if (node.startFieldPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "DuckDB does not support a datetime field INTERVAL precision. " +
                            "Precision has been omitted in the output.",
                ),
            )
        }
        if (node.endFieldFractionalPrecision != null) {
            listener.report(
                ScribeProblem.simpleInfo(
                    code = ScribeProblem.TRANSLATION_INFO,
                    message =
                        "DuckDB does not support a fractional second INTERVAL precision. " +
                            "Fractional second precision has been omitted in the output. " +
                            "DuckDB has a default fractional precision `3` for INTERVAL second.",
                ),
            )
        }
    }

    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        val f = node.function
        // Keep the DATE_DIFF datetime part as a quoted string (`date_diff('year', a, b)`); the base renderer would
        // unquote it. DATE_ADD is handled with the builtins/datetime cases in a later PR.
        if (!f.hasQualifier() &&
            f.identifier.text.uppercase() == "DATE_DIFF" &&
            node.args.size == 3
        ) {
            val start = "("
            t = visitIdentifier(f, t)
            t = t concat list(this, start) { node.args }
            return t
        }
        return when {
            node.function.identifier.text == "transform" -> {
                // DuckDB's list-transform function is `list_transform` (Trino calls it `transform`); it uses
                // `->` to separate the element variable and the element expr.
                val arrayExpr = node.args[0].sql(dialect = this)
                val elementVar = node.args[1].sql(dialect = this)
                val elementExpr = node.args[2].sql(dialect = this)
                var h = tail
                h = h concat "list_transform"
                h = h concat "($arrayExpr, $elementVar -> $elementExpr)"
                h
            }
            else -> super.visitExprCall(node, tail)
        }
    }

    // DuckDB's native struct constructor is the struct literal `{'field': value, ...}`. It preserves field names
    // and types (inferred from the values) and works inside a `list_transform` lambda, so we emit it directly
    // rather than `CAST(ROW(...) AS ROW(...))`. https://duckdb.org/docs/current/sql/data_types/struct
    override fun visitExprStruct(
        node: ExprStruct,
        tail: SqlBlock,
    ): SqlBlock {
        // DuckDB cannot represent an empty struct: `{}`, `ROW()`, and `struct_pack()` all error.
        if (node.fields.isEmpty()) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_AST_TO_TEXT_CONVERSION,
                    "DuckDB does not support empty struct values.",
                ),
            )
        }
        return tail concat list(this, "{", "}") { node.fields }
    }

    override fun visitExprStructField(
        node: ExprStruct.Field,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        // The field name is a string-literal expr, rendered as `'name'`.
        t = visitExprWrapped(node.name, t)
        t = t concat ": "
        t = visitExprWrapped(node.value, t)
        return t
    }

    /**
     * The PartiQL plan and thus AST will fully-qualify ORDER BY variable references and paths with an implicit
     * binding tuple name for set operations. For DuckDB, there is no implicit binding tuple name for the set ops. So
     * we must remove the prefix binding tuple name from paths.
     *
     * For example, the query
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY a
     * will have an extra qualification
     *   (SELECT a FROM ...) UNION (SELECT a FROM ...) ORDER BY "_1".a
     * The additional qualification is invalid DuckDB, hence why we remove it.
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
                    // DuckDB only allows ordering a set operation by an output column (or an expression present
                    // in every SELECT), not by a nested path into an output column (e.g. `flds.c.field_x`).
                    if (newExpr is ExprPath) {
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                ScribeProblem.UNSUPPORTED_OPERATION,
                                "DuckDB does not support ORDER BY a nested path on a set operation (UNION/INTERSECT/" +
                                    "EXCEPT) result; only output columns may be used.",
                            ),
                        )
                    }
                    sort(newExpr, sort.order, sort.nulls)
                }
            val newNode =
                exprQuerySet(
                    body = node.body,
                    limit = node.limit,
                    offset = node.offset,
                    orderBy = orderBy(newSorts),
                )
            return visitExprQuerySetLimitOffsetTReorder(newNode, tail)
        }
        return visitExprQuerySetLimitOffsetTReorder(node, tail)
    }

    private fun visitExprQuerySetLimitOffsetTReorder(
        node: ExprQuerySet,
        tail: SqlBlock,
    ): SqlBlock {
        var t = super.visitExprQuerySet(node, tail)

        if (node.limit != null && node.offset != null) {
            var current = tail
            var limitBlock: SqlBlock? = null
            var offsetBlock: SqlBlock? = null

            // locate last occurence of LIMIT and OFFSET node
            while (current.next != null) {
                val next = current.next!!
                if (next is SqlBlock.Text) {
                    if (next.text.trim() == "LIMIT") {
                        limitBlock = current
                    } else if (next.text.trim() == "OFFSET") {
                        offsetBlock = current
                    }
                }
                current = next
            }

            if (limitBlock != null && offsetBlock != null) {
                // swap the order of LIMIT and OFFSET clauses
                val limit = limitBlock.next!!
                val offset = offsetBlock.next!!
                val end = offset.next!!.next
                limitBlock.next = offset
                offset.next!!.next = limit
                limit.next!!.next = end

                // update the tail to point to the new end if needed
                if (end == null) {
                    t = limit.next!!
                }
            }
        }

        return t
    }

    /**
     * DuckDB-specific type conversions
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
            DataType.TIME -> tail concat "TIME"

            // According to https://duckdb.org/docs/current/language/types.html#timestamp-p-with-time-zone,
            // DuckDB does not support precision and `WITH TIME ZONE` in TIME/TIMESTAMP in time literal,
            // but support them in the scenarios like cast or table creation.
            // e.g. SELECT cast(TIMESTAMP '2020-06-10 15:55:23.383345' as TIMESTAMP(12));
            // However, due to limit of Datum limitation, unspecified precision information is not preserved. So we ignore
            // the precision information here.
            // TODO precision https://github.com/partiql/partiql-scribe/issues/145
            DataType.TIME_WITH_TIME_ZONE -> tail concat "TIME WITH TIME ZONE"
            DataType.TIMESTAMP -> tail concat "TIMESTAMP"
            DataType.TIMESTAMP_WITH_TIME_ZONE -> tail concat "TIMESTAMP WITH TIME ZONE"
            // DuckDB has a single unified INTERVAL type and ignores the qualifier in a type position (e.g. a
            // CAST target), so we drop it: `CAST(x AS INTERVAL YEAR)` -> `CAST(x AS INTERVAL)`. Interval
            // *literals* keep their qualifier (they render via visitIntervalQualifier, not here) since DuckDB
            // needs the unit there.
            DataType.INTERVAL -> tail concat "INTERVAL"
            DataType.MAP -> {
                var t = tail concat "MAP("
                t = visitDataType(node.keyType, t)
                t = t concat ", "
                t = visitDataType(node.elementType, t)
                t = t concat ")"
                t
            }
            else -> super.visitDataType(node, tail)
        }
    }

    // DuckDB's MAP constructor: MAP(ARRAY[key1, key2, ...], ARRAY[val1, val2, ...])
    // https://duckdb.org/docs/current/functions/map.html
    @Suppress("DEPRECATION")
    override fun visitExprMap(
        node: ExprMap,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail concat "MAP(ARRAY["
        node.entries.forEachIndexed { index, entry ->
            t = visitExprWrapped(entry.key, t)
            if (index < node.entries.size - 1) {
                t = t concat ", "
            }
        }
        t = t concat "], ARRAY["
        node.entries.forEachIndexed { index, entry ->
            t = visitExprWrapped(entry.value, t)
            if (index < node.entries.size - 1) {
                t = t concat ", "
            }
        }
        t = t concat "])"
        return t
    }
}
