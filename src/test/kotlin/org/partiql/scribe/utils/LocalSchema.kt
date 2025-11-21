package org.partiql.scribe.utils

import com.amazon.ionelement.api.BoolElement
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.ionListOf
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField

/**
 * Parses an IonElement to a [PType].
 * The format used is effectively Avro JSON, but with PartiQL type names.
 * TODO eventually use some generated serde
 */
internal fun IonElement.toPType(): PType = LocalSchema.load(this)

private object LocalSchema {
    @JvmStatic
    fun load(ion: IonElement): PType =
        when (ion) {
            is StringElement -> ion.atomic()
            is ListElement -> ion.union()
            is StructElement -> ion.type()
            else -> error("Invalid element type, expected Ion StringElement, ListElement, or StructElement")
        }

    /**
     * Atomic type `a` is just a string "a".
     */
    private fun StringElement.atomic(): PType =
        when (textValue) {
            "any" -> PType.dynamic()
            "bool" -> PType.bool()
            "int8" -> PType.tinyint()
            "int16" -> PType.smallint()
            "int32" -> PType.integer()
            "int64" -> PType.bigint()
            "decimal" -> PType.decimal()
            "float32" -> PType.real()
            "float64" -> PType.doublePrecision()
            "string" -> PType.string()
            "blob" -> PType.blob()
            "clob" -> PType.clob()
            "date" -> PType.date()
            "time" -> PType.time()
            "timez" -> PType.timez()
            "timestamp" -> PType.timestamp()
            "timestampz" -> PType.timestampz()
            // Interval Single
            "intervalY" -> PType.intervalYear(2)
            "intervalMon" -> PType.intervalMonth(2)
            "intervalD" -> PType.intervalDay(2)
            "intervalH" -> PType.intervalHour(2)
            "intervalMin" -> PType.intervalMinute(2)
            "intervalS" -> PType.intervalSecond(2, 6)
            // Interval Range
            "intervalY2Mon" -> PType.intervalYearMonth(2)
            "intervalD2H" -> PType.intervalDayHour(2)
            "intervalD2Min" -> PType.intervalDayMinute(2)
            "intervalD2S" -> PType.intervalDaySecond(2, 6)
            "intervalH2Min" -> PType.intervalHourMinute(2)
            "intervalH2S" -> PType.intervalHourSecond(2, 6)
            "intervalMin2S" -> PType.intervalMinuteSecond(2, 6)
            // other
            "bag" -> error("`bag` is not an atomic type")
            "list" -> error("`list` is not an atomic type")
            "sexp" -> error("`sexp` is not an atomic type")
            "struct" -> error("`struct` is not an atomic type")
            // unsupported
            "interval" -> error("`interval` is currently not supported")
            "symbol" -> error("`symbol` is currently not supported")
            "binary" -> error("`binary` is currently not supported")
            "byte" -> error("`byte` is currently not supported")
            "null", "missing" -> PType.unknown()
            else -> error("Invalid type `$textValue`")
        }

    /**
     * Union (a|b) is represented by Ion list [ a, b ].
     */
    private fun ListElement.union(): PType {
        val allTypes = values.map { load(it) }.toSet()
        return when (allTypes.size) {
            1 -> allTypes.first()
            else -> PType.dynamic()
        }
    }

    private fun IonElement.type(): PType =
        when (this) {
            is StringElement -> atomic()
            is ListElement -> union()
            is StructElement -> {
                when (val type = getAngry<StringElement>("type").textValue) {
                    "bool" -> bool()
                    "int8" -> int8()
                    "int16" -> int16()
                    "int32" -> int32()
                    "int64" -> int64()
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
                    "timez" -> time()
                    "timestamp" -> timestamp()
                    "timestampz" -> timestamp()
                    "interval" -> interval()
                    "bag" -> bag()
                    "list" -> list()
                    "struct" -> struct()
                    "any" -> PType.dynamic()
                    "null", "missing" -> PType.unknown()
                    else -> error("Invalid type `$this`")
                }
            }
            else -> error("Invalid element type, expected Ion StringElement, ListElement, or StructElement")
        }

    // constraints?
    private fun StructElement.bool(): PType = PType.bool()

    // constraints?
    private fun StructElement.int8(): PType = PType.tinyint()

    // constraints?
    private fun StructElement.int16(): PType = PType.smallint()

    // constraints?
    private fun StructElement.int32(): PType = PType.integer()

    // constraints?
    private fun StructElement.int64(): PType = PType.bigint()

    // constraints?
    private fun StructElement.decimal(): PType {
        val precision = getMaybe<IntElement>("precision")?.longValue
        val scale = getMaybe<IntElement>("scale")?.longValue
        return when (precision) {
            null -> PType.decimal()
            else -> PType.decimal(precision.toInt(), scale?.toInt() ?: 0)
        }
    }

    // constraints?
    private fun StructElement.float32(): PType = PType.real()

    // constraints?
    private fun StructElement.float64(): PType = PType.doublePrecision()

    // constraints?
    private fun StructElement.char(): PType {
        return when (val length = getMaybe<IntElement>("length")?.longValue) {
            null -> PType.character()
            else -> PType.character(length.toInt())
        }
    }

    // constraints?
    private fun StructElement.string(): PType {
        return when (val length = getMaybe<IntElement>("length")?.longValue) {
            null -> PType.varchar()
            else -> PType.varchar(length.toInt())
        }
    }

    // constraints?
    private fun StructElement.binary(): PType = error("Binary type not supported")

    // constraints?
    private fun StructElement.byte(): PType = error("Byte type not supported")

    // constraints?
    private fun StructElement.blob(): PType = PType.blob()

    // constraints?
    private fun StructElement.clob(): PType = PType.clob()

    // constraints?
    private fun StructElement.date(): PType = PType.date()

    // constraints?
    private fun StructElement.time(): PType {
        val precision = getMaybe<IntElement>("precision")?.longValue?.toInt()
        val withTimeZone = getMaybe<BoolElement>("withTimeZone")?.booleanValue ?: false
        return when (precision) {
            null ->
                when (withTimeZone) {
                    true -> PType.timez()
                    false -> PType.time()
                }
            else ->
                when (withTimeZone) {
                    true -> PType.timez(precision)
                    false -> PType.time(precision)
                }
        }
    }

    // constraints?
    private fun StructElement.timestamp(): PType {
        val precision = getMaybe<IntElement>("precision")?.longValue?.toInt()
        val withTimeZone = getMaybe<BoolElement>("withTimeZone")?.booleanValue ?: false
        return when (precision) {
            null ->
                when (withTimeZone) {
                    true -> PType.timestampz()
                    false -> PType.timestamp()
                }
            else ->
                when (withTimeZone) {
                    true -> PType.timestampz(precision)
                    false -> PType.timestamp(precision)
                }
        }
    }

    // constraints?
    private fun StructElement.interval(): PType = error("Interval not supported")

    // constraints?
    private fun StructElement.bag(): PType =
        PType.bag(
            load(getAngry<IonElement>("items")),
        )

    // constraints?
    private fun StructElement.list(): PType =
        PType.array(
            load(getAngry<IonElement>("items")),
        )

    private fun StructElement.struct(): PType {
// Constraints
        var isClosed = false
        var isOrdered = false
        val constraintsE = getOptional("constraints") ?: ionListOf()
        (constraintsE as ListElement).values.map {
            assert(it is SymbolElement)
            it as SymbolElement
            when (it.textValue) {
                "ordered" -> isOrdered = true
                "unique" -> {} // TODO do something with `unique`?
                "closed" -> {
                    isClosed = true
                }
                else -> error("unknown tuple constraint `${it.textValue}`")
            }
        }
        // Fields
        val fieldsE = getAngry<ListElement>("fields")
        val fields =
            fieldsE.values.map {
                assert(it is StructElement) { "field definition must be as struct" }
                it as StructElement
                val name = it.getAngry<StringElement>("name").textValue
                val type = it.getAngry<IonElement>("type").toPType()
                PTypeField.of(name, type)
            }
        return if (isClosed && isOrdered) {
            PType.row(fields)
        } else {
            PType.struct() // TODO plumb through other constraints
        }
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
