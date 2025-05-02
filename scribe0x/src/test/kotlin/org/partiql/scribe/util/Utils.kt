package org.partiql.scribe.util

import org.partiql.types.StaticType


fun <T> cartesianProduct(a: List<T>, b: List<T>, vararg lists: List<T>): Set<List<T>> =
    (listOf(a, b).plus(lists))
        .fold(listOf(listOf<T>())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }.toSet()

val allSupportedType = StaticType.ALL_TYPES.filterNot { it == StaticType.GRAPH }

val allSupportedTypeNotUnknown = allSupportedType.filterNot { it == StaticType.MISSING || it == StaticType.NULL }

val allCollectionType = listOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP)

val allTextType = listOf(StaticType.SYMBOL, StaticType.STRING)

val allDateTimeType = listOf(StaticType.TIME, StaticType.TIMESTAMP, StaticType.DATE)

val allNumberType = StaticType.NUMERIC.allTypes

val allIntType = listOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT)