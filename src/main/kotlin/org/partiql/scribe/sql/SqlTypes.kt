package org.partiql.scribe.sql

import org.partiql.ast.Type

/**
 * The [SqlTypes] class is responsible for creating an AST node from a type constructor function.
 *
 * Usages:
 *   - Translating types within a CAST.
 */
public interface SqlTypes {

    public fun any(): Type
    public fun bool(): Type
    public fun int8(): Type
    public fun int16(): Type
    public fun int32(): Type
    public fun int64(): Type
    public fun int(): Type
    public fun decimal(precision: Int?, scale: Int?): Type
    public fun decimalArbitrary(): Type
    public fun float32(): Type
    public fun float64(): Type
    public fun char(length: Int?): Type
    public fun string(length: Int?): Type
    public fun symbol(): Type
    public fun binary(length: Int?): Type
    public fun byte(length: Int?): Type
    public fun blob(length: Int?): Type
    public fun clob(length: Int?): Type
    public fun date(): Type
    public fun time(precision: Int?): Type
    public fun timestamp(precision: Int?): Type
    public fun interval(precision: Int?): Type
    public fun bag(): Type
    public fun list(): Type
    public fun sexp(): Type
    public fun struct(): Type

    public companion object {
        @JvmStatic
        public val DEFAULT: SqlTypes = object : Base() {}
    }

    public abstract class Base : SqlTypes {

        override fun any(): Type = Type.Any()
        override fun bool(): Type = Type.Bool()
        override fun int8(): Type = Type.Custom("INT1")
        override fun int16(): Type = Type.Int2()
        override fun int32(): Type = Type.Int4()
        override fun int64(): Type = Type.Int8()
        override fun int(): Type = Type.Int()
        override fun decimal(precision: Int?, scale: Int?): Type = Type.Decimal(precision, scale)
        override fun decimalArbitrary(): Type = Type.Decimal(null, null)
        override fun float32(): Type = Type.Float32()
        override fun float64(): Type = Type.Float64()
        override fun char(length: Int?): Type = Type.Char(length)
        override fun string(length: Int?): Type = Type.String(length)
        override fun symbol(): Type = Type.Symbol()
        override fun binary(length: Int?): Type = Type.BitVarying(length)
        override fun byte(length: Int?): Type = Type.ByteString(length)
        override fun blob(length: Int?): Type = Type.Blob(length)
        override fun clob(length: Int?): Type = Type.Clob(length)
        override fun date(): Type = Type.Date()
        override fun time(precision: Int?): Type = Type.Time(precision)
        override fun timestamp(precision: Int?): Type = Type.Timestamp(precision)
        override fun interval(precision: Int?): Type = Type.Interval(precision)
        override fun bag(): Type = Type.Bag()
        override fun list(): Type = Type.List()
        override fun sexp(): Type = Type.Sexp()
        override fun struct(): Type = Type.Struct()
    }
}
