package org.partiql.scribe.targets.duckdb

import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.sql
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.utils.concat
import org.partiql.scribe.sql.utils.list
import java.math.BigDecimal

/**
 * AST -> SQL text rendering for the DuckDB dialect.
 */
public open class DuckDBAstToSql(context: ScribeContext) : AstToSql(context) {
    override fun visitExprSessionAttribute(
        node: ExprSessionAttribute,
        tail: SqlBlock,
    ): SqlBlock {
        return tail concat node.sessionAttribute.name().lowercase()
    }

    /**
     * DuckDB has no `IS <type>` predicate. Rewrite it to a `typeof(<value>)` comparison, e.g.
     * `x IS INT` -> `typeof(x) = 'INTEGER'`. For STRUCT, DuckDB's `typeof` reports a parameterized name
     * (e.g. `STRUCT(a INTEGER)`), so match by prefix with `starts_with(typeof(x), 'STRUCT')`. Types we cannot
     * map fall back to the base `IS <type>` rendering.
     */
    override fun visitExprIsType(
        node: ExprIsType,
        tail: SqlBlock,
    ): SqlBlock {
        val typeName = duckDBTypeofName(node.type) ?: return super.visitExprIsType(node, tail)
        var t = tail
        // STRUCT: typeof yields a parameterized name, so compare by prefix.
        if (node.type.code() == DataType.STRUCT) {
            if (node.isNot) t = t concat "NOT "
            t = t concat "starts_with(typeof("
            t = visitExprWrapped(node.value, t)
            t = t concat "), 'STRUCT')"
            return t
        }
        t = t concat "typeof("
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) ") <> '$typeName'" else ") = '$typeName'"
        return t
    }

    /**
     * Maps a PartiQL [DataType] to the string DuckDB's `typeof` returns for that type, or null if we have no
     * faithful mapping.
     */
    private fun duckDBTypeofName(type: DataType): String? =
        when (type.code()) {
            DataType.INT, DataType.INTEGER, DataType.INT4, DataType.INTEGER4 -> "INTEGER"
            DataType.BIGINT, DataType.INT8, DataType.INTEGER8 -> "BIGINT"
            DataType.SMALLINT, DataType.INT2, DataType.INTEGER2 -> "SMALLINT"
            DataType.TINYINT -> "TINYINT"
            DataType.REAL -> "FLOAT"
            DataType.DOUBLE_PRECISION -> "DOUBLE"
            DataType.BOOL, DataType.BOOLEAN -> "BOOLEAN"
            DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR,
            DataType.CHARACTER_VARYING, DataType.CHAR_VARYING, DataType.STRING,
            -> "VARCHAR"
            DataType.DATE -> "DATE"
            DataType.TIME -> "TIME"
            DataType.TIMESTAMP -> "TIMESTAMP"
            DataType.STRUCT -> "STRUCT"
            else -> null
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

    override fun visitExprCall(
        node: ExprCall,
        tail: SqlBlock,
    ): SqlBlock {
        var t = tail
        val f = node.function
        // Render DATE_DIFF('<part>', <lhs>, <rhs>) without quoting the datetime-field part identifier.
        // (DATE_ADD is rejected upstream in DuckDBCalls.dateAdd, so it never reaches here.)
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
}
