package org.partiql.scribe.sql

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprBetween
import org.partiql.ast.exprBinary
import org.partiql.ast.exprCall
import org.partiql.ast.exprCast
import org.partiql.ast.exprInCollection
import org.partiql.ast.exprIsType
import org.partiql.ast.exprLike
import org.partiql.ast.exprLit
import org.partiql.ast.exprSessionAttribute
import org.partiql.ast.exprUnary
import org.partiql.ast.identifierSymbol
import org.partiql.ast.typeAny
import org.partiql.ast.typeBag
import org.partiql.ast.typeBlob
import org.partiql.ast.typeBool
import org.partiql.ast.typeChar
import org.partiql.ast.typeClob
import org.partiql.ast.typeDate
import org.partiql.ast.typeDecimal
import org.partiql.ast.typeFloat32
import org.partiql.ast.typeFloat64
import org.partiql.ast.typeInt
import org.partiql.ast.typeInt2
import org.partiql.ast.typeInt4
import org.partiql.ast.typeInt8
import org.partiql.ast.typeInterval
import org.partiql.ast.typeList
import org.partiql.ast.typeMissing
import org.partiql.ast.typeNullType
import org.partiql.ast.typeSexp
import org.partiql.ast.typeString
import org.partiql.ast.typeStruct
import org.partiql.ast.typeSymbol
import org.partiql.ast.typeTime
import org.partiql.ast.typeTimestamp
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.symbolValue

/**
 * Transform the call args to the special form.
 */
public typealias SqlCallFn = (SqlArgs) -> Expr

/**
 * List of arguments.
 */
public typealias SqlArgs = List<SqlArg>

/**
 * Maps a function name to basic rewrite logic.
 *
 * For target implementors, extend this and leverage the type-annotated function arguments to perform desired rewrite.
 */
@OptIn(PartiQLValueExperimental::class)
public abstract class SqlCalls {

    public companion object {
        @JvmStatic
        public val DEFAULT: SqlCalls = object : SqlCalls() {}
    }

    /**
     * SqlCalls uses the SqlTypes hooks to emit type AST nodes.
     */
    public open val types: SqlTypes = SqlTypes.DEFAULT

    /**
     * List of special form rules. See [org.partiql.planner.Header] for the derivations.
     */
    public open val rules: Map<String, SqlCallFn> = mapOf(
        "not" to ::notFn,
        "pos" to ::posFn,
        "neg" to ::negFn,
        "eq" to ::eqFn,
        "ne" to ::neFn,
        "and" to ::andFn,
        "or" to ::orFn,
        "lt" to ::ltFn,
        "lte" to ::lteFn,
        "gt" to ::gtFn,
        "gte" to ::gteFn,
        "plus" to ::plusFn,
        "minus" to ::minusFn,
        "times" to ::timesFn,
        "divide" to ::divFn,
        "modulo" to ::modFn,
        "concat" to ::concatFn,
        "bitwise_and" to ::bitwiseAnd,
        // CASTS
        "cast_bool" to { args -> rewriteCast(PartiQLValueType.BOOL, args) },
        "cast_int8" to { args -> rewriteCast(PartiQLValueType.INT8, args) },
        "cast_int16" to { args -> rewriteCast(PartiQLValueType.INT16, args) },
        "cast_int32" to { args -> rewriteCast(PartiQLValueType.INT32, args) },
        "cast_int64" to { args -> rewriteCast(PartiQLValueType.INT64, args) },
        "cast_int" to { args -> rewriteCast(PartiQLValueType.INT, args) },
        "cast_decimal" to { args -> rewriteCast(PartiQLValueType.DECIMAL, args) },
        "cast_float32" to { args -> rewriteCast(PartiQLValueType.FLOAT32, args) },
        "cast_float64" to { args -> rewriteCast(PartiQLValueType.FLOAT64, args) },
        "cast_char" to { args -> rewriteCast(PartiQLValueType.CHAR, args) },
        "cast_string" to { args -> rewriteCast(PartiQLValueType.STRING, args) },
        "cast_symbol" to { args -> rewriteCast(PartiQLValueType.SYMBOL, args) },
        "cast_binary" to { args -> rewriteCast(PartiQLValueType.BINARY, args) },
        "cast_byte" to { args -> rewriteCast(PartiQLValueType.BYTE, args) },
        "cast_blob" to { args -> rewriteCast(PartiQLValueType.BLOB, args) },
        "cast_clob" to { args -> rewriteCast(PartiQLValueType.CLOB, args) },
        "cast_date" to { args -> rewriteCast(PartiQLValueType.DATE, args) },
        "cast_time" to { args -> rewriteCast(PartiQLValueType.TIME, args) },
        "cast_timestamp" to { args -> rewriteCast(PartiQLValueType.TIMESTAMP, args) },
        "cast_interval" to { args -> rewriteCast(PartiQLValueType.INTERVAL, args) },
        "cast_bag" to { args -> rewriteCast(PartiQLValueType.BAG, args) },
        "cast_list" to { args -> rewriteCast(PartiQLValueType.LIST, args) },
        "cast_sexp" to { args -> rewriteCast(PartiQLValueType.SEXP, args) },
        "cast_struct" to { args -> rewriteCast(PartiQLValueType.STRUCT, args) },
        "cast_null" to { args -> rewriteCast(PartiQLValueType.NULL, args) },
        "cast_missing" to { args -> rewriteCast(PartiQLValueType.MISSING, args) },
        // DATE_ADD
        "date_add_year" to { args -> dateAdd(DatetimeField.YEAR, args) },
        "date_add_month" to { args -> dateAdd(DatetimeField.MONTH, args) },
        "date_add_day" to { args -> dateAdd(DatetimeField.DAY, args) },
        "date_add_hour" to { args -> dateAdd(DatetimeField.HOUR, args) },
        "date_add_minute" to { args -> dateAdd(DatetimeField.MINUTE, args) },
        "date_add_second" to { args -> dateAdd(DatetimeField.SECOND, args) },
        // DATE_DIFF
        "date_diff_year" to { args -> dateDiff(DatetimeField.YEAR, args) },
        "date_diff_month" to { args -> dateDiff(DatetimeField.MONTH, args) },
        "date_diff_day" to { args -> dateDiff(DatetimeField.DAY, args) },
        "date_diff_hour" to { args -> dateDiff(DatetimeField.HOUR, args) },
        "date_diff_minute" to { args -> dateDiff(DatetimeField.MINUTE, args) },
        "date_diff_second" to { args -> dateDiff(DatetimeField.SECOND, args) },
        // LIKE
        "like" to { args -> like(args, escape = false) },
        "like_escape" to { args -> like(args, escape = true) },
        // IS
        "is_bool" to { args -> isType(PartiQLValueType.BOOL, args) },
        "is_int8" to { args -> isType(PartiQLValueType.INT8, args) },
        "is_int16" to { args -> isType(PartiQLValueType.INT16, args) },
        "is_int32" to { args -> isType(PartiQLValueType.INT32, args) },
        "is_int64" to { args -> isType(PartiQLValueType.INT64, args) },
        "is_int" to { args -> isType(PartiQLValueType.INT, args) },
        "is_decimal" to { args -> isType(PartiQLValueType.DECIMAL, args) },
        "is_float32" to { args -> isType(PartiQLValueType.FLOAT32, args) },
        "is_float64" to { args -> isType(PartiQLValueType.FLOAT64, args) },
        "is_char" to { args -> isType(PartiQLValueType.CHAR, args) },
        "is_string" to { args -> isType(PartiQLValueType.STRING, args) },
        "is_symbol" to { args -> isType(PartiQLValueType.SYMBOL, args) },
        "is_binary" to { args -> isType(PartiQLValueType.BINARY, args) },
        "is_byte" to { args -> isType(PartiQLValueType.BYTE, args) },
        "is_blob" to { args -> isType(PartiQLValueType.BLOB, args) },
        "is_clob" to { args -> isType(PartiQLValueType.CLOB, args) },
        "is_date" to { args -> isType(PartiQLValueType.DATE, args) },
        "is_time" to { args -> isType(PartiQLValueType.TIME, args) },
        "is_timestamp" to { args -> isType(PartiQLValueType.TIMESTAMP, args) },
        "is_interval" to { args -> isType(PartiQLValueType.INTERVAL, args) },
        "is_bag" to { args -> isType(PartiQLValueType.BAG, args) },
        "is_list" to { args -> isType(PartiQLValueType.LIST, args) },
        "is_sexp" to { args -> isType(PartiQLValueType.SEXP, args) },
        "is_struct" to { args -> isType(PartiQLValueType.STRUCT, args) },
        "is_null" to { args -> isType(PartiQLValueType.NULL, args) },
        "is_missing" to { args -> isType(PartiQLValueType.MISSING, args) },
        // Session Attributes
        "current_user" to sessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER),
        "current_date" to sessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_DATE),
        // in collection
        "in_collection" to { args -> inCollection(args) },
        // between
        "between" to {args -> between(args)},
    )

    public fun retarget(name: String, args: SqlArgs): Expr {
        val rule = rules[name]
        return if (rule == null) {
            // use default translations
            default(name, args)
        } else {
            // use special rule
            rule(args)
        }
    }

    private fun default(name: String, args: SqlArgs): Expr {
        return exprCall(
            function = identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE),
            args = args.map { it.expr },
        )
    }

    private fun sessionAttribute(attribute: Expr.SessionAttribute.Attribute): SqlCallFn {
        return { _ -> exprSessionAttribute(attribute) }
    }

    public open fun unary(op: Expr.Unary.Op, args: SqlArgs): Expr {
        assert(args.size == 1) { "Unary operator $op requires exactly 1 argument" }
        return exprUnary(op, args[0].expr)
    }

    public open fun binary(op: Expr.Binary.Op, args: SqlArgs): Expr {
        assert(args.size == 2) { "Binary operator $op requires exactly 2 arguments" }
        return exprBinary(op, args[0].expr, args[1].expr)
    }

    public open fun notFn(args: SqlArgs): Expr {
        // Check to replace NOT (x = y) with x <> y
        val arg = args[0].expr
        if (arg is Expr.Binary && arg.op == Expr.Binary.Op.EQ) {
            return exprBinary(Expr.Binary.Op.NE, arg.lhs, arg.rhs)
        }
        return unary(Expr.Unary.Op.NOT, args)
    }

    public open fun posFn(args: SqlArgs): Expr = unary(Expr.Unary.Op.POS, args)

    public open fun negFn(args: SqlArgs): Expr = unary(Expr.Unary.Op.NEG, args)

    public open fun eqFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.EQ, args)

    public open fun neFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.NE, args)

    public open fun andFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.AND, args)

    public open fun orFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.OR, args)

    public open fun ltFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.LT, args)

    public open fun lteFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.LTE, args)

    public open fun gtFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.GT, args)

    public open fun gteFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.GTE, args)

    public open fun plusFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.PLUS, args)

    public open fun minusFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.MINUS, args)

    public open fun timesFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.TIMES, args)

    public open fun divFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.DIVIDE, args)

    public open fun modFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.MODULO, args)

    public open fun concatFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.CONCAT, args)

    public open fun bitwiseAnd(args: SqlArgs): Expr = binary(Expr.Binary.Op.BITWISE_AND, args)

    public open fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        val call = identifierSymbol("DATE_ADD", Identifier.CaseSensitivity.INSENSITIVE)
        val arg0 = exprLit(symbolValue(part.name.uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    public open fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        val call = identifierSymbol("DATE_DIFF", Identifier.CaseSensitivity.INSENSITIVE)
        val arg0 = exprLit(symbolValue(part.name.uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(call, listOf(arg0, arg1, arg2))
    }

    // functions to operator
    public open fun inCollection(args: SqlArgs) : Expr = exprInCollection(args[0].expr, args[1].expr, false)

    public open fun between(args: SqlArgs) : Expr = exprBetween(args[0].expr, args[1].expr, args[2].expr, false)

    public open fun like(args: SqlArgs, escape: Boolean): Expr {
        val arg0 = args[0].expr
        val arg1 = args[1].expr
        val arg2 = if (escape) args[2].expr else null
        return exprLike(arg0, arg1, arg2, false)
    }

    /**
     * This function is responsible for translating CAST calls.
     *
     * In PartiQL 0.14 implementations, the CAST plan nodes are just a Rex.Op.Call.
     * However, these are modeled as their own nodes in PartiQL 1.0. This improved modeling enables us to preserve
     * the original type through to execution.
     */
    @Deprecated("This will be removed for PartiQL 1.0 compatibility.")
    public open fun rewriteCast(type: PartiQLValueType, args: SqlArgs): Expr {
        assert(args.size == 1) { "CAST should only have 1 argument" }
        val value = args[0].expr
        val asType = when (type) {
            PartiQLValueType.ANY -> types.any()
            PartiQLValueType.BOOL -> types.bool()
            PartiQLValueType.INT8 -> types.int8()
            PartiQLValueType.INT16 -> types.int16()
            PartiQLValueType.INT32 -> types.int32()
            PartiQLValueType.INT64 -> types.int64()
            PartiQLValueType.INT -> types.int()
            PartiQLValueType.DECIMAL -> types.decimal(null, null)
            PartiQLValueType.DECIMAL_ARBITRARY -> types.decimalArbitrary()
            PartiQLValueType.FLOAT32 -> types.float32()
            PartiQLValueType.FLOAT64 -> types.float64()
            PartiQLValueType.CHAR -> types.char(null)
            PartiQLValueType.STRING -> types.string(null)
            PartiQLValueType.SYMBOL -> types.symbol()
            PartiQLValueType.BINARY -> error("Unsupported")
            PartiQLValueType.BYTE -> error("Unsupported")
            PartiQLValueType.BLOB -> types.blob(null)
            PartiQLValueType.CLOB -> types.clob(null)
            PartiQLValueType.DATE -> types.date()
            PartiQLValueType.TIME -> types.time(null)
            PartiQLValueType.TIMESTAMP -> types.timestamp(null)
            PartiQLValueType.INTERVAL -> types.interval(null)
            PartiQLValueType.BAG -> types.bag()
            PartiQLValueType.LIST -> types.list()
            PartiQLValueType.SEXP -> types.sexp()
            PartiQLValueType.STRUCT -> types.struct()
            PartiQLValueType.NULL -> error("CAST AS NULL not supported.")
            PartiQLValueType.MISSING -> error("CAST AS MISSING not supported.")
        }
        return exprCast(value, asType)
    }

    public open fun isType(type: PartiQLValueType, args: SqlArgs): Expr {
        // leverage the fact that we have at most 2 type parameters, a little hacky but ok for now
        val (typeArg0, typeArg1, value) = when(args.size) {
            1 -> Triple(null, null, args[0].expr)
            2 -> Triple(null, args.first(), args[1].expr)
            else -> Triple(args[0], args[1], args[2].expr)
        }
        val asType = when (type) {
            PartiQLValueType.ANY -> typeAny()
            PartiQLValueType.BOOL -> typeBool()
            PartiQLValueType.INT8 -> error("unsupported")
            PartiQLValueType.INT16 -> typeInt2()
            PartiQLValueType.INT32 -> typeInt4()
            PartiQLValueType.INT64 -> typeInt8()
            PartiQLValueType.INT -> typeInt()
            PartiQLValueType.DECIMAL_ARBITRARY -> typeDecimal(null, null)
            PartiQLValueType.DECIMAL -> typeDecimal(typeArg0?.toInt(), typeArg1?.toInt())
            PartiQLValueType.FLOAT32 -> typeFloat32()
            PartiQLValueType.FLOAT64 -> typeFloat64()
            PartiQLValueType.CHAR -> typeChar(typeArg1?.toInt())
            PartiQLValueType.STRING -> typeString(typeArg1?.toInt())
            PartiQLValueType.SYMBOL -> typeSymbol()
            PartiQLValueType.BINARY -> error("Unsupported")
            PartiQLValueType.BYTE -> error("Unsupported")
            PartiQLValueType.BLOB -> typeBlob(null)
            PartiQLValueType.CLOB -> typeClob(null)
            PartiQLValueType.DATE -> typeDate()
            PartiQLValueType.TIME -> typeTime(null)
            PartiQLValueType.TIMESTAMP -> typeTimestamp(null)
            PartiQLValueType.INTERVAL -> typeInterval(null)
            PartiQLValueType.BAG -> typeBag()
            PartiQLValueType.LIST -> typeList()
            PartiQLValueType.SEXP -> typeSexp()
            PartiQLValueType.STRUCT -> typeStruct()
            PartiQLValueType.NULL -> typeNullType()
            PartiQLValueType.MISSING -> typeMissing()
        }
        return exprIsType(value, asType, null)
    }

    private fun SqlArg.toInt() = ((this.expr as Expr.Lit).value as NumericValue<*>).int
}
