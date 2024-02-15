package org.partiql.plugins.local

import com.amazon.ionelement.api.BoolElement
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.plugins.local.LocalSchema.int
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLTimestampExperimental

// Use some generated serde eventually

/**
 * Parses an IonElement to a StaticType.
 *
 * The format used is effectively Avro JSON, but with PartiQL type names.
 */
internal fun IonElement.toStaticType(): StaticType = LocalSchema.load(this)

private object LocalSchema {

    @JvmStatic
    fun load(ion: IonElement): StaticType = when (ion) {
        is StringElement -> ion.atomic()
        is ListElement -> ion.union()
        is StructElement -> ion.type()
        else -> error("Invalid element type, expected Ion StringElement, ListElement, or StructElement")
    }

    /**
     * Atomic type `a` is just a string "a".
     */
    private fun StringElement.atomic(): StaticType = when (textValue) {
        "any" -> StaticType.ANY
        "bool" -> StaticType.BOOL
        "int8" -> error("`int8` is not representable with StaticType")
        "int16" -> StaticType.INT2
        "int32" -> StaticType.INT4
        "int64" -> StaticType.INT8
        "int" -> StaticType.INT
        "decimal" -> StaticType.DECIMAL
        "float32" -> StaticType.FLOAT
        "float64" -> StaticType.FLOAT
        "string" -> StaticType.STRING
        "symbol" -> StaticType.SYMBOL
        "binary" -> error("`binary` is currently not supported")
        "byte" -> error("`byte` is currently not supported")
        "blob" -> StaticType.BLOB
        "clob" -> StaticType.CLOB
        "date" -> StaticType.DATE
        "time" -> StaticType.TIME
        "timestamp" -> StaticType.TIMESTAMP
        "interval" -> error("`interval` is currently not supported")
        "bag" -> error("`bag` is not an atomic type")
        "list" -> error("`list` is not an atomic type")
        "sexp" -> error("`sexp` is not an atomic type")
        "struct" -> error("`struct` is not an atomic type")
        "null" -> StaticType.NULL
        "missing" -> StaticType.MISSING
        else -> error("Invalid type `$textValue`")
    }

    /**
     * Union (a|b) is represented by Ion list [ a, b ].
     */
    private fun ListElement.union(): StaticType = StaticType.unionOf(values.map { load(it) }.toSet())

    private fun IonElement.type(): StaticType = when (this) {
        is StringElement -> atomic()
        is ListElement -> union()
        is StructElement -> {
            when (getAngry<StringElement>("type").textValue) {
                "null" -> StaticType.NULL
                "missing" -> StaticType.MISSING
                "bool" -> bool()
                "int8" -> int8()
                "int16" -> int16()
                "int32" -> int32()
                "int64" -> int64()
                "int" -> int()
                "decimal" -> decimal()
                "float32" -> float32()
                "float64" -> float64()
                "char" -> char()
                "string" -> string()
                "binary" -> binary()
                "byte" -> byte()
                "blob" -> blob()
                "clob" -> clob()
                "date" -> date()
                "time" -> time()
                "timestamp" -> timestamp()
                "interval" -> interval()
                "bag" -> bag()
                "list" -> list()
                "struct" -> struct()
                "any" -> StaticType.ANY
                else -> error("Invalid type `$this`")
            }
        }
        else -> error("Invalid element type, expected Ion StringElement, ListElement, or StructElement")
    }


    // constraints?
    private fun StructElement.bool(): StaticType = StaticType.BOOL

    // constraints?
    private fun StructElement.int8(): StaticType = error("Int8 not supported in StaticType")

    // constraints?
    private fun StructElement.int16(): StaticType = StaticType.INT2

    // constraints?
    private fun StructElement.int32(): StaticType = StaticType.INT4

    // constraints?
    private fun StructElement.int64(): StaticType = StaticType.INT8

    // constraints?
    private fun StructElement.int(): StaticType = StaticType.INT

    // constraints?
    private fun StructElement.decimal(): StaticType {
        val precision = getMaybe<IntElement>("precision")?.longValue
        val scale = getMaybe<IntElement>("scale")?.longValue
        val constraint = when (precision) {
            null -> DecimalType.PrecisionScaleConstraint.Unconstrained
            else -> DecimalType.PrecisionScaleConstraint.Constrained(
                precision = precision.toInt(),
                scale = scale?.toInt() ?: 0,
            )
        }
        return DecimalType(constraint)
    }

    // constraints?
    private fun StructElement.float32(): StaticType = StaticType.FLOAT

    // constraints?
    private fun StructElement.float64(): StaticType = StaticType.FLOAT

    // constraints?
    private fun StructElement.char(): StaticType {
        val length = getMaybe<IntElement>("length")?.longValue
        val constraint = when (length) {
            null -> StringType.StringLengthConstraint.Unconstrained
            else -> StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(length.toInt()))
        }
        return StringType(constraint)
    }

    // constraints?
    private fun StructElement.string(): StaticType {
        val length = getMaybe<IntElement>("length")?.longValue
        val constraint = when (length) {
            null -> StringType.StringLengthConstraint.Unconstrained
            else -> StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(length.toInt()))
        }
        return StringType(constraint)
    }

    // constraints?
    private fun StructElement.binary(): StaticType = error("Binary type not supported")

    // constraints?
    private fun StructElement.byte(): StaticType = error("Byte type not supported")

    // constraints?
    private fun StructElement.blob(): StaticType = error("Blob type not supported")

    // constraints?
    private fun StructElement.clob(): StaticType = StaticType.CLOB

    // constraints?
    private fun StructElement.date(): StaticType = StaticType.DATE

    // constraints?
    private fun StructElement.time(): StaticType {
        val precision = getMaybe<IntElement>("precision")?.longValue?.toInt()
        val withTimeZone = getMaybe<BoolElement>("withTimeZone")?.booleanValue ?: false
        return TimeType(precision, withTimeZone)
    }

    // constraints?
    @OptIn(PartiQLTimestampExperimental::class)
    private fun StructElement.timestamp(): StaticType {
        val precision = getMaybe<IntElement>("precision")?.longValue?.toInt()
        val withTimeZone = getMaybe<BoolElement>("withTimeZone")?.booleanValue ?: false
        return TimestampType(precision, withTimeZone)
    }

    // constraints?
    private fun StructElement.interval(): StaticType = error("Interval not supported")

    // constraints?
    private fun StructElement.bag(): StaticType = BagType(
        elementType = load(getAngry<IonElement>("items")),
    )

    // constraints?
    private fun StructElement.list(): StaticType = ListType(
        elementType = load(getAngry<IonElement>("items")),
    )

    private fun StructElement.struct(): StaticType {
// Constraints
        var contentClosed = false
        val constraintsE = getOptional("constraints") ?: ionListOf()
        val constraints = (constraintsE as ListElement).values.map {
            assert(it is SymbolElement)
            it as SymbolElement
            when (it.textValue) {
                "ordered" -> TupleConstraint.Ordered
                "unique" -> TupleConstraint.UniqueAttrs(true)
                "closed" -> {
                    contentClosed = true
                    TupleConstraint.Open(false)
                }
                else -> error("unknown tuple constraint `${it.textValue}`")
            }
        }.toSet()
        // Fields
        val fieldsE = getAngry<ListElement>("fields")
        val fields = fieldsE.values.map {
            assert(it is StructElement) { "field definition must be as struct" }
            it as StructElement
            val name = it.getAngry<StringElement>("name").textValue
            val type = it.getAngry<IonElement>("type").toStaticType()
            StructType.Field(name, type)
        }
        return StructType(fields, contentClosed, constraints = constraints)
    }
}

private inline fun <reified T : IonElement> StructElement.getAngry(name: String): T {
    val f = getOptional(name) ?: error("Expected field `$name`")
    if (f !is T) {
        error("Expected field `name` to be of type ${T::class.simpleName}")
    }
    return f
}

private inline fun <reified T : IonElement> StructElement.getMaybe(name: String): T? {
    val f = getOptional(name)
    return f as? T
}

internal fun StaticType.toIon(): IonElement = when (this) {
    is AnyOfType -> this.toIon()
    is AnyType -> ionString("any")
    is BlobType -> ionString("blob")
    is BoolType -> ionString("bool")
    is ClobType -> ionString("clob")
    is BagType -> this.toIon()
    is ListType -> this.toIon()
    is SexpType -> this.toIon()
    is DateType -> ionString("date")
    is DecimalType -> ionString("decimal")
    is FloatType -> ionString("float64")
    is GraphType -> ionString("graph")
    is IntType -> when (this.rangeConstraint) {
        IntType.IntRangeConstraint.SHORT -> ionString("int16")
        IntType.IntRangeConstraint.INT4 -> ionString("int32")
        IntType.IntRangeConstraint.LONG -> ionString("int64")
        IntType.IntRangeConstraint.UNCONSTRAINED -> ionString("int")
    }
    MissingType -> ionString("missing")
    is NullType -> ionString("null")
    is StringType -> ionString("string") // TODO char
    is StructType -> this.toIon()
    is SymbolType -> ionString("symbol")
    is TimeType -> ionString("time")
    is TimestampType -> ionString("timestamp")
}

private fun AnyOfType.toIon(): IonElement {
    // create some predictable ordering
    val sorted = this.types.sortedWith { t1, t2 -> t1::class.java.simpleName.compareTo(t2::class.java.simpleName) }
    val elements = sorted.map { it.toIon() }
    return ionListOf(elements)
}

private fun BagType.toIon(): IonElement = ionStructOf(
    "type" to ionString("bag"),
    "items" to elementType.toIon()
)

private fun ListType.toIon(): IonElement = ionStructOf(
    "type" to ionString("list"),
    "items" to elementType.toIon()
)

private fun SexpType.toIon(): IonElement = ionStructOf(
    "type" to ionString("sexp"),
    "items" to elementType.toIon()
)

private fun StructType.toIon(): IonElement {
    val constraintSymbols = mutableListOf<SymbolElement>()
    for (constraint in constraints) {
        val c = when (constraint) {
            is TupleConstraint.Open -> if (constraint.value) null else ionSymbol("closed")
            TupleConstraint.Ordered -> ionSymbol("ordered")
            is TupleConstraint.UniqueAttrs -> ionSymbol("unique")
        }
        if (c != null) constraintSymbols.add(c)
    }
    val fieldTypes = this.fields.map {
        ionStructOf(
            "name" to ionString(it.key),
            "type" to it.value.toIon(),
        )
    }
    return ionStructOf(
        "type" to ionString("struct"),
        "fields" to ionListOf(fieldTypes),
        "constraints" to ionListOf(constraintSymbols),
    )
}

