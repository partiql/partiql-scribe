@file:Suppress("ktlint:standard:no-empty-file")
//package org.partiql.scribe.targets.spark
//
//import org.partiql.scribe.targets.SqlTargetSuite
//import org.partiql.scribe.utils.Functions
//import org.partiql.scribe.utils.SessionProvider
//import kotlin.io.path.toPath
//
//class SparkTargetSuite : SqlTargetSuite() {
//    override val target = SparkTarget()
//
//    override val root = this::class.java.getResource("/outputs/spark")!!.toURI().toPath()
//
//    override val sessions =
//        SessionProvider(
//            scalarOverloads =
//                mapOf(
//                    "split" to Functions.scalarSplit,
//                ),
//        )
//}
