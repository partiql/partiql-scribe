// TODO see if these tests are needed anymore
//package org.partiql.scribe.targets.trino
//
//import org.junit.jupiter.api.Test
//import org.partiql.scribe.asAbsent
//import org.partiql.scribe.asMissable
//import org.partiql.types.StaticType
//import kotlin.test.assertTrue
//
//class TrinoTypesTest {
//
//    private fun test(t1: StaticType, t2: StaticType) {
//        // t1 = t2
//        assertTrue(TrinoTypes.comparable(t1, t2))
//        // t1 = t2|null
//        assertTrue(TrinoTypes.comparable(t1, t2.asNullable()))
//        // t1|null = t2
//        assertTrue(TrinoTypes.comparable(t1.asNullable(), t2))
//        // t1|null = t2|null
//        assertTrue(TrinoTypes.comparable(t1.asNullable(), t2.asNullable()))
//        // t1 = t2|missing
//        assertTrue(TrinoTypes.comparable(t1, t2.asMissable()))
//        // t1|missing = t2
//        assertTrue(TrinoTypes.comparable(t1.asMissable(), t2))
//        // t1|missing = t2|missing
//        assertTrue(TrinoTypes.comparable(t1.asMissable(), t2.asMissable()))
//        // t1 = t2|null|missing
//        assertTrue(TrinoTypes.comparable(t1, t2.asAbsent()))
//        // t1|null|missing = t2
//        assertTrue(TrinoTypes.comparable(t1.asAbsent(), t2))
//        // t1|null|missing = t2|null|missing
//        assertTrue(TrinoTypes.comparable(t1.asAbsent(), t2.asAbsent()))
//    }
//
//    @Test
//    fun bool() {
//        val t1 = StaticType.BOOL
//        val t2 = StaticType.BOOL
//        test(t1, t2)
//    }
//
//    @Test
//    fun numeric() {
//        val nums = arrayOf(StaticType.INT2,
//            StaticType.INT4,
//            StaticType.INT8,
//            StaticType.INT,
//            StaticType.FLOAT,
//            StaticType.DECIMAL,
//        )
//        for (t1 in nums) {
//            for (t2 in nums) {
//                test(t1, t2)
//            }
//        }
//        // pairs
//        for (i in 0 until (nums.size - 1)) {
//            for (j in 0 until (nums.size - 1)) {
//                val t1 = StaticType.unionOf(nums[i], nums[i + 1])
//                val t2 = StaticType.unionOf(nums[j], nums[j + 1])
//                test(t1, t2)
//            }
//        }
//    }
//
//    @Test
//    fun text() {
//        val t1 = StaticType.STRING
//        val t2 = StaticType.STRING
//        test(t1, t2)
//    }
//
//    @Test
//    fun datetime() {
//        test(StaticType.DATE, StaticType.DATE)
//        test(StaticType.TIME, StaticType.TIME)
//        test(StaticType.TIMESTAMP, StaticType.TIMESTAMP)
//        test(StaticType.TIMESTAMP, StaticType.DATE)
//        test(StaticType.DATE, StaticType.TIMESTAMP)
//    }
//
//    @Test
//    fun structural() {
//        test(StaticType.STRUCT, StaticType.STRUCT)
//        test(StaticType.LIST, StaticType.LIST)
//        test(StaticType.BAG, StaticType.BAG)
//        test(StaticType.LIST, StaticType.BAG)
//        test(StaticType.LIST, StaticType.LIST)
//    }
//
//    @Test
//    fun unionOfTypes() {
//        test(StaticType.unionOf(StaticType.INT, StaticType.INT2), StaticType.DECIMAL)
//        test(StaticType.unionOf(StaticType.INT, StaticType.STRING), StaticType.STRING)
//        // below should have been flattened at some point
//        test(StaticType.unionOf(StaticType.unionOf(StaticType.INT, StaticType.INT2)), StaticType.DECIMAL)
//        test(StaticType.unionOf(StaticType.unionOf(StaticType.INT, StaticType.STRING)), StaticType.DECIMAL)
//        test(StaticType.unionOf(StaticType.unionOf(StaticType.unionOf(StaticType.INT, StaticType.INT2))), StaticType.DECIMAL)
//        test(StaticType.unionOf(StaticType.unionOf(StaticType.unionOf(StaticType.INT))), StaticType.DECIMAL)
//    }
//}
