package org.partiql.scribe.integ.executor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.partiql.scribe.integ.loader.Target
import java.io.File

data class SkipEntry(
    val test: String,
    val reason: String,
    val issue: String? = null,
)

data class SkipListData(
    val expected_failures: List<SkipEntry> = emptyList(),
    val skip: List<SkipEntry> = emptyList(),
)

class SkipList private constructor(
    private val expectedFailures: Set<String>,
    private val skipped: Set<String>,
) {

    fun isExpectedFailure(testName: String): Boolean = testName in expectedFailures

    fun isSkipped(testName: String): Boolean = testName in skipped

    companion object {
        private val mapper = ObjectMapper().registerKotlinModule()

        fun empty() = SkipList(emptySet(), emptySet())

        fun load(skipListDir: File, target: Target): SkipList {
            val file = skipListDir.resolve("${target.name.lowercase()}.json")
            if (!file.exists()) return empty()

            val data: SkipListData = mapper.readValue(file)
            return SkipList(
                expectedFailures = data.expected_failures.map { it.test }.toSet(),
                skipped = data.skip.map { it.test }.toSet(),
            )
        }
    }
}
