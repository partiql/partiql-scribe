package org.partiql.scribe.targets.redshift

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Type
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.identifierSymbol
import org.partiql.scribe.sql.SqlBlock
import org.partiql.scribe.sql.SqlDialect
import org.partiql.scribe.sql.concat
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Redshift SQL dialect for PartiQL Scribe.
 */
public open class RedshiftDialect : SqlDialect() {

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS \"${node.asAlias!!.symbol}\"" else t
        return t
    }

    /**
     * Redshift does not support x['y'] syntax; replace with x.y.
     *
     * @param node
     * @param tail
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: SqlBlock): SqlBlock {
        val key = node.key
        return if (key is Expr.Lit && key.value is StringValue) {
            val symbol = exprPathStepSymbol(identifierSymbol(
                symbol = (key.value as StringValue).value!!,
                caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
            ))
            visitExprPathStepSymbol(symbol, tail)
        } else {
            super.visitExprPathStepIndex(node, tail)
        }
    }

    // Redshift's equivalent for PartiQL's STRUCT type is SUPER OBJECT. Can use the `OBJECT` function to create SUPER
    // OBJECTs: https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html
    override fun visitExprStruct(node: Expr.Struct, tail: SqlBlock): SqlBlock {
        return tail concat list("OBJECT(", ")") { node.fields }
    }

    override fun visitExprStructField(node: Expr.Struct.Field, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ", "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprCall(node: Expr.Call, tail: SqlBlock): SqlBlock {
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
            fn is Identifier.Symbol && fn.symbol == "OBJECT_TRANSFORM" -> {
                var t = tail
                t = visitIdentifier(fn, t) concat "("
                val input = node.args[0]
                val keepPaths = list(start = " KEEP ", end = "" ) { (node.args[1] as Expr.Collection).values }
                val setPathExprs = (node.args[2] as Expr.Collection).values
                val setPaths = if (setPathExprs.isEmpty()) {
                    // No empty structs, so no `SET` argument
                    SqlBlock.Text("")
                } else {
                    list(start = " SET ", end = "") { setPathExprs }
                }
                t = visitExprWrapped(input, t) concat keepPaths concat setPaths concat ")"
                t
            }
            else -> super.visitExprCall(node, tail)
        }
    }

    override fun visitExprTrim(node: Expr.Trim, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [[LEADING|TRAILING|BOTH] [chars FROM]
        when {
            node.spec != null && node.chars != null -> {
                t = t concat node.spec!!.name
                t = t concat " "
                t = visitExprWrapped(node.chars!!, t)
                t = t concat " FROM "
            }
            node.spec != null -> {
                t = t concat node.spec!!.name
                t = t concat " "
            }
            node.chars != null -> {
                t = visitExprWrapped(node.chars!!, t)
                t = t concat " FROM "
            }
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    /**
     * - LIST   -> https://docs.aws.amazon.com/redshift/latest/dg/r_expression_lists.html
     * - ARRAY  -> https://docs.aws.amazon.com/redshift/latest/dg/r_array.html
     */
    override fun visitExprCollection(node: Expr.Collection, tail: SqlBlock): SqlBlock {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.ARRAY -> "ARRAY(" to ")"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.SEXP,
            Expr.Collection.Type.BAG,
            Expr.Collection.Type.LIST -> "(" to ")"
        }
        return tail concat list(start, end) { node.values }
    }

    override fun visitTypeString(node: Type.String, tail: SqlBlock): SqlBlock = tail concat "VARCHAR"

    // Essentially the same as `SqlDialect`. For SQL time and timestamp literals, Redshift (like Postgresql) requires
    // specifying if the time/timestamp has a timezone in the type name. Otherwise, the offset will be ignored.
    // Postgresql reference -- https://www.postgresql.org/docs/16/datatype-datetime.html#DATATYPE-DATETIME-INPUT-TIME-STAMPS
    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: Expr.Lit, tail: SqlBlock): SqlBlock {
        val value = when (val v = node.value) {
            is TimeValue -> {
                when (val t = v.value) {
                    null -> "null"  // null.time
                    else -> sqlString(t)
                }
            }
            is TimestampValue -> {
                when (val t = v.value) {
                    null -> "null" // null.timestamp
                    else -> sqlString(t)
                }
            }
            else -> return super.visitExprLit(node, tail)
        }
        return tail concat value
    }

    private fun padZeros(v: Int, totalDigits: Int): String = String.format("%0${totalDigits}d", v)

    private fun sqlString(tz: TimeZone?): String {
        return when (tz) {
            null -> ""
            is TimeZone.UnknownTimeZone -> "-00:00" // could consider giving an error for unknown time zone offset
            is TimeZone.UtcOffset -> {
                val sign = if (tz.totalOffsetMinutes < 0) {
                    "-"
                } else {
                    "+"
                }
                val hh = padZeros(abs(tz.tzHour), 2)
                val mm = padZeros(abs(tz.tzMinute), 2)
                "$sign$hh:$mm"
            }
        }
    }

    private fun sqlString(t: Time): String {
        val hh = padZeros(t.hour, 2)
        val mm = padZeros(t.minute, 2)
        val ss = padZeros(t.decimalSecond.toInt(), 2)
        val frac = t.decimalSecond.remainder(BigDecimal.ONE).toString().substring(1) // drop leading 0
        val timeZone = sqlString(t.timeZone)
        val timeType = when (t.timeZone) {
            null -> "TIME"
            else -> "TIMETZ"    // `TIMETZ` is shorter than `TIME WITH TIME ZONE`
        }
        return "$timeType '$hh:$mm:$ss$frac$timeZone'"
    }

    private fun sqlString(t: Timestamp): String {
        val yyyy = padZeros(t.year, 4)
        val mon = padZeros(t.month, 2)
        val dd = padZeros(t.day, 2)
        val hh = padZeros(t.hour, 2)
        val min = padZeros(t.minute, 2)
        val ss = padZeros(t.decimalSecond.toInt(), 2)
        val frac = t.decimalSecond.remainder(BigDecimal.ONE).toString().substring(1) // drop leading 0
        val timeZone = sqlString(t.timeZone)
        val timestampType = when (t.timeZone) {
            null -> "TIMESTAMP"
            else -> "TIMESTAMPTZ"   // `TIMESTAMPTZ` is shorter than `TIMESTAMP WITH TIME ZONE`
        }
        return "$timestampType '$yyyy-$mon-$dd $hh:$min:$ss$frac$timeZone'"
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
}
