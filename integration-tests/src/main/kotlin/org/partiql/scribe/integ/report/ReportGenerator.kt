package org.partiql.scribe.integ.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.partiql.scribe.integ.engine.Status
import org.partiql.scribe.integ.executor.TestResult
import org.partiql.scribe.integ.loader.Target
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReportGenerator(private val outputDir: File) {

    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .enable(SerializationFeature.INDENT_OUTPUT)

    fun generate(results: List<TestResult>, scribeVersion: String? = null) {
        outputDir.mkdirs()
        val markdown = buildMarkdown(results, scribeVersion)
        val json = buildJson(results, scribeVersion)

        outputDir.resolve("report.md").writeText(markdown)
        outputDir.resolve("report.json").writeText(json)

        println(markdown)
    }

    private fun buildMarkdown(results: List<TestResult>, scribeVersion: String?): String {
        val now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        val byTarget = results.groupBy { it.testCase.target }
        val sb = StringBuilder()

        sb.appendLine("# Scribe Integration Test Report")
        sb.appendLine("**Date:** $now")
        if (scribeVersion != null) sb.appendLine("**Scribe version:** $scribeVersion")
        sb.appendLine("**Targets:** ${byTarget.keys.joinToString { it.name.lowercase() }}")
        sb.appendLine()

        sb.appendLine("## Summary")
        sb.appendLine("| Target | Total | Pass | Fail | Expected Fail | Skip | Pass Rate |")
        sb.appendLine("|--------|-------|------|------|---------------|------|-----------|")

        for ((target, targetResults) in byTarget) {
            val total = targetResults.size
            val pass = targetResults.count { it.execution.status == Status.SUCCESS }
            val fail = targetResults.count { it.execution.status == Status.FAILED || it.execution.status == Status.TIMEOUT }
            val expectedFail = targetResults.count { it.execution.status == Status.EXPECTED_FAIL }
            val skip = targetResults.count { it.execution.status == Status.SKIPPED }
            val executed = total - skip
            val passRate = if (executed > 0) "%.1f%%".format(pass.toDouble() / executed * 100) else "N/A"
            sb.appendLine("| ${target.name.lowercase()} | $total | $pass | $fail | $expectedFail | $skip | $passRate |")
        }
        sb.appendLine()

        // Failures detail
        val failures = results.filter { it.execution.status == Status.FAILED || it.execution.status == Status.TIMEOUT }
        if (failures.isNotEmpty()) {
            sb.appendLine("## Failures")
            for ((target, targetFailures) in failures.groupBy { it.testCase.target }) {
                sb.appendLine("### ${target.name.lowercase()}")
                sb.appendLine("| Test | Group | Error |")
                sb.appendLine("|------|-------|-------|")
                for (f in targetFailures) {
                    val error = f.execution.error?.take(100)?.replace("|", "\\|") ?: "unknown"
                    sb.appendLine("| ${f.testCase.name} | ${f.testCase.group} | $error |")
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    private fun buildJson(results: List<TestResult>, scribeVersion: String?): String {
        val byTarget = results.groupBy { it.testCase.target }
        val summary = byTarget.mapKeys { it.key.name.lowercase() }.mapValues { (_, v) ->
            mapOf(
                "total" to v.size,
                "pass" to v.count { it.execution.status == Status.SUCCESS },
                "fail" to v.count { it.execution.status == Status.FAILED || it.execution.status == Status.TIMEOUT },
                "expected_fail" to v.count { it.execution.status == Status.EXPECTED_FAIL },
                "skip" to v.count { it.execution.status == Status.SKIPPED },
            )
        }

        val resultEntries = results.map { r ->
            buildMap {
                put("name", r.testCase.name)
                put("group", r.testCase.group)
                put("target", r.testCase.target.name.lowercase())
                put("status", r.execution.status.name)
                put("durationMs", r.execution.durationMs)
                if (r.execution.error != null) put("error", r.execution.error)
            }
        }

        val report = mapOf(
            "timestamp" to Instant.now().toString(),
            "scribeVersion" to scribeVersion,
            "summary" to summary,
            "results" to resultEntries,
        )

        return mapper.writeValueAsString(report)
    }
}
