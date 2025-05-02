package org.partiql.scribe.targets.trino

import org.partiql.types.BagType
import org.partiql.types.BoolType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import kotlin.reflect.KClass

/**
 * Trino type comparability verified via Athena.
 *
 * Find and replace the `SELECT CAST('a' AS varchar)` depending on the type.
 *
 * -- SELECT CAST('a' AS varchar) = true
 * -- Numbers
 * -- SELECT CAST('a' AS varchar) = CAST(1 AS tinyint)
 * -- SELECT CAST('a' AS varchar) = CAST(1 AS smallint)
 * -- SELECT CAST('a' AS varchar) = CAST(1 AS int)
 * -- SELECT CAST('a' AS varchar) = CAST(1 AS bigint)
 * -- SELECT CAST('a' AS varchar) = CAST(1.0 AS real)
 * -- SELECT CAST('a' AS varchar) = CAST(1.0 AS double)
 * -- SELECT CAST('a' AS varchar) = CAST(1.0 AS decimal(2,1))
 * -- Strings
 * -- SELECT CAST('a' AS varchar) = CAST('a' AS varchar)
 * -- SELECT CAST('a' AS varchar) = CAST('a' AS char)
 * -- Datetime
 * -- SELECT CAST('a' AS varchar) = DATE '2001-08-22'
 * -- SELECT CAST('a' AS varchar) = TIME '01:02:03.456 -08:00'
 * -- SELECT CAST('a' AS varchar) = TIMESTAMP '2020-06-10 15:55:23'
 * -- Structural
 * -- SELECT CAST('a' AS varchar) = ARRAY[0]
 * -- SELECT CAST('a' AS varchar) = MAP(ARRAY['foo', 'bar'], ARRAY[1, 2])
 * -- SELECT CAST('a' AS varchar) = ROW(true)
 */
internal object TrinoTypes {

    fun comparable(t1: StaticType, t2: StaticType): Boolean {
        if (t1 == t2 || t1::class == t2::class) {
            return true
        }
        val lhs = candidates(t1)
        val rhs = candidates(t2)
        for (t in rhs) {
            if (t in lhs) {
                return true
            }
        }
        return false
    }

    private fun candidates(t: StaticType): Set<KClass<*>> = t.flatten().allTypes
        .filter { it !is NullType && it !is MissingType }
        .flatMap { types[it::class]?.toList() ?: emptyList() }
        .toSet()

    private val types: Map<KClass<*>, Array<KClass<*>>> = mapOf(
        BoolType::class to arrayOf(BoolType::class),
        IntType::class to arrayOf(IntType::class,FloatType::class,DecimalType::class),
        FloatType::class to arrayOf(IntType::class,FloatType::class,DecimalType::class),
        DecimalType::class to arrayOf(IntType::class,FloatType::class,DecimalType::class),
        StringType::class to arrayOf(StringType::class),
        DateType::class to arrayOf(DateType::class,TimestampType::class),
        TimeType::class to arrayOf(TimeType::class),
        TimestampType::class to arrayOf(TimestampType::class,DateType::class),
        ListType::class to arrayOf(ListType::class,BagType::class),
        BagType::class to arrayOf(BagType::class,ListType::class),
        StructType::class to arrayOf(StructType::class),
    )
}
