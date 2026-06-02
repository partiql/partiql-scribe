package org.partiql.scribe.integ.loader

import java.io.File

enum class Target { TRINO, REDSHIFT, SPARK }

data class TestCase(
    val name: String,
    val group: String,
    val sql: String,
    val target: Target,
    val isSkipped: Boolean,
)

class TestLoader(private val scribePath: File) {

    private val outputsDir = scribePath.resolve("src/test/resources/outputs")

    fun load(target: Target, groups: List<String>? = null): List<TestCase> {
        val targetDir = outputsDir.resolve(target.name.lowercase())
        if (!targetDir.exists()) return emptyList()

        return targetDir.walkTopDown()
            .filter { it.extension == "sql" }
            .filter { file ->
                groups == null || groups.any { file.parentFile.name == it }
            }
            .flatMap { file -> parseFile(file, target) }
            .toList()
    }

    private fun parseFile(file: File, target: Target): List<TestCase> {
        val group = file.parentFile.name
        val cases = mutableListOf<TestCase>()
        var currentName: String? = null
        var currentSql = StringBuilder()
        var isSkipped = false

        for (line in file.readLines()) {
            val activeMatch = ACTIVE_TEST_PATTERN.matchEntire(line)
            val skippedMatch = SKIPPED_TEST_PATTERN.matchEntire(line)

            when {
                activeMatch != null -> {
                    flushTest(cases, currentName, currentSql, group, target, isSkipped)
                    currentName = activeMatch.groupValues[1]
                    currentSql = StringBuilder()
                    isSkipped = false
                }
                skippedMatch != null -> {
                    flushTest(cases, currentName, currentSql, group, target, isSkipped)
                    currentName = skippedMatch.groupValues[1]
                    currentSql = StringBuilder()
                    isSkipped = true
                }
                else -> {
                    if (currentName != null && !isSkipped) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("--")) {
                            currentSql.appendLine(line)
                        }
                    }
                }
            }
        }
        flushTest(cases, currentName, currentSql, group, target, isSkipped)
        return cases
    }

    private fun flushTest(
        cases: MutableList<TestCase>,
        name: String?,
        sql: StringBuilder,
        group: String,
        target: Target,
        isSkipped: Boolean,
    ) {
        if (name == null) return
        val sqlText = sql.toString().trim()
        cases.add(
            TestCase(
                name = name,
                group = group,
                sql = sqlText,
                target = target,
                isSkipped = isSkipped || sqlText.isEmpty(),
            )
        )
    }

    companion object {
        private val ACTIVE_TEST_PATTERN = Regex("""^--#\[(.+)]$""")
        private val SKIPPED_TEST_PATTERN = Regex("""^-- #\[(.+)]$""")
    }
}
