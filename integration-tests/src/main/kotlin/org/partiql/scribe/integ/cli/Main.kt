package org.partiql.scribe.integ.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.partiql.scribe.integ.config.Config
import org.partiql.scribe.integ.engine.AthenaSparkClient
import org.partiql.scribe.integ.engine.AthenaTrinoClient
import org.partiql.scribe.integ.engine.EngineClient
import org.partiql.scribe.integ.engine.RedshiftClient
import org.partiql.scribe.integ.engine.Status
import org.partiql.scribe.integ.executor.SkipList
import org.partiql.scribe.integ.executor.TestExecutor
import org.partiql.scribe.integ.loader.Target
import org.partiql.scribe.integ.loader.TestLoader
import org.partiql.scribe.integ.report.ReportGenerator
import java.io.File
import java.nio.file.Path

class ScribeInteg : CliktCommand(
    name = "scribe-integ",
    help = "Scribe integration test runner"
) {
    override fun run() = Unit
}

class TestCommand : CliktCommand(
    name = "test",
    help = "Run integration tests against target engines"
) {
    private val scribePath by option("--scribe-path", help = "Path to partiql-scribe root")
        .path(mustExist = true)
        .default(Path.of("."))

    private val targets by option("--targets", help = "Comma-separated targets: trino,redshift,spark")
        .split(",")
        .default(listOf("trino"))

    private val groups by option("--groups", help = "Filter by test group (directory name)")
        .split(",")

    private val timeout by option("--timeout", help = "Per-query timeout in seconds")
        .int()
        .default(60)

    private val concurrency by option("--concurrency", help = "Parallel queries per engine")
        .int()
        .default(5)

    private val output by option("--output", help = "Report output directory")
        .path()
        .default(Path.of("./build/reports/integ"))

    private val profile by option("--profile", help = "AWS profile name")
        .default("scribe-dev")

    override fun run() {
        val config = Config(profile = profile)
        val scribeDir = scribePath.toFile()
        val skipListDir = scribeDir.resolve("skip-lists")

        val requestedTargets = targets.map { Target.valueOf(it.uppercase()) }
        echo("Targets: ${requestedTargets.joinToString { it.name.lowercase() }}")
        echo("Groups: ${groups?.joinToString() ?: "all"}")

        // Check data freshness
        val catalogDir = scribeDir.resolve("src/test/resources/catalogs/default")
        val dataLoader = org.partiql.scribe.integ.data.DataLoader(config)
        if (dataLoader.isStale(catalogDir)) {
            echo("WARNING: Test data is stale. Run 'load-data' to refresh.")
        }

        // Load test cases
        val loader = TestLoader(scribeDir)
        val testCases = requestedTargets.flatMap { target ->
            loader.load(target, groups)
        }
        echo("Loaded ${testCases.size} test cases")

        if (testCases.isEmpty()) {
            echo("No test cases found. Check --scribe-path and --targets.")
            return
        }

        // Build engine clients
        val engines = buildEngines(requestedTargets, config)
        echo("Engines: ${engines.keys.joinToString { it.name.lowercase() }}")

        // Build skip lists
        val skipLists = requestedTargets.associateWith { target ->
            SkipList.load(skipListDir, target)
        }

        // Execute
        echo("Running tests (concurrency=$concurrency, timeout=${timeout}s)...")
        val executor = TestExecutor(
            engines = engines,
            concurrency = concurrency,
            skipLists = skipLists,
        )
        val results = executor.execute(testCases)

        // Report
        val reporter = ReportGenerator(output.toFile())
        val scribeVersion = resolveScribeVersion(scribeDir)
        reporter.generate(results, scribeVersion)

        // Exit code
        val failures = results.count {
            it.execution.status == Status.FAILED || it.execution.status == Status.TIMEOUT
        }
        if (failures > 0) {
            echo("$failures unexpected failure(s) detected.")
            throw ProgramResult(1)
        }
    }

    private fun buildEngines(targets: List<Target>, config: Config): Map<Target, EngineClient> {
        val engines = mutableMapOf<Target, EngineClient>()
        for (target in targets) {
            when (target) {
                Target.TRINO -> engines[target] = AthenaTrinoClient(
                    athenaConfig = config.athena,
                    appConfig = config,
                    timeoutMs = timeout * 1000L,
                )
                Target.REDSHIFT -> engines[target] = RedshiftClient(
                    redshiftConfig = config.redshift,
                    appConfig = config,
                    timeoutMs = timeout * 1000L,
                )
                Target.SPARK -> engines[target] = AthenaSparkClient(
                    athenaConfig = config.athena,
                    appConfig = config,
                    timeoutMs = timeout * 1000L,
                )
            }
        }
        return engines
    }

    private fun resolveScribeVersion(scribeDir: File): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(scribeDir)
                .redirectErrorStream(true)
                .start()
            val commit = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (commit.isNotEmpty()) "main@$commit" else null
        } catch (e: Exception) {
            null
        }
    }
}

class LoadDataCommand : CliktCommand(
    name = "load-data",
    help = "Load or refresh test data in AWS engines"
) {
    private val scribePath by option("--scribe-path", help = "Path to partiql-scribe root")
        .path(mustExist = true)
        .default(Path.of("."))

    private val targets by option("--targets", help = "Comma-separated targets to load: glue,redshift")
        .split(",")
        .default(listOf("glue"))

    private val check by option("--check", help = "Only check staleness, don't reload")
        .flag()

    private val force by option("--force", help = "Force reload even if data is fresh")
        .flag()

    private val profile by option("--profile", help = "AWS profile name")
        .default("scribe-dev")

    override fun run() {
        val config = Config(profile = profile)
        val scribeDir = scribePath.toFile()
        val catalogDir = scribeDir.resolve("src/test/resources/catalogs/default")

        echo("Parsing catalogs from: $catalogDir")

        val parser = org.partiql.scribe.integ.loader.CatalogParser(scribeDir)
        val schemas = parser.parse()
        echo("Found ${schemas.size} table schemas:")
        for (schema in schemas) {
            echo("  ${schema.name} (${schema.columns.size} columns)")
        }

        val loader = org.partiql.scribe.integ.data.DataLoader(config)

        if (check) {
            val stale = loader.isStale(catalogDir)
            echo(if (stale) "Data is STALE — reload needed." else "Data is FRESH — no reload needed.")
            return
        }

        if (!force && !loader.isStale(catalogDir)) {
            echo("Data is fresh, skipping reload. Use --force to override.")
            return
        }

        echo("Generating synthetic data...")
        val generator = org.partiql.scribe.integ.data.DataGenerator()
        val tables = generator.generate(schemas)

        if ("glue" in targets) {
            echo("Uploading to S3 and creating Glue tables...")
            loader.load(tables, catalogDir)
            echo("Glue: Loaded ${tables.size} tables with ${tables.sumOf { it.rows.size }} total rows.")
        }

        if ("redshift" in targets) {
            echo("Loading data into Redshift (scribe-cluster)...")
            val rsLoader = org.partiql.scribe.integ.data.RedshiftDataLoader(config)
            rsLoader.load(tables)
            echo("Redshift: Loaded ${tables.size} tables with ${tables.sumOf { it.rows.size }} total rows.")
        }

        echo("Done.")
    }
}

class HealthCommand : CliktCommand(
    name = "health",
    help = "Health check all configured engines"
) {
    private val targets by option("--targets", help = "Comma-separated targets: trino,redshift,spark")
        .split(",")
        .default(listOf("trino"))

    private val profile by option("--profile", help = "AWS profile name")
        .default("scribe-dev")

    override fun run() {
        val config = Config(profile = profile)
        val requestedTargets = targets.map { Target.valueOf(it.uppercase()) }

        for (target in requestedTargets) {
            val client: EngineClient? = when (target) {
                Target.TRINO -> AthenaTrinoClient(athenaConfig = config.athena, appConfig = config)
                Target.REDSHIFT -> RedshiftClient(redshiftConfig = config.redshift, appConfig = config)
                Target.SPARK -> AthenaSparkClient(athenaConfig = config.athena, appConfig = config)
            }
            if (client == null) {
                echo("${target.name.lowercase()}: NOT IMPLEMENTED")
                continue
            }
            val healthy = client.healthCheck()
            echo("${target.name.lowercase()}: ${if (healthy) "OK" else "FAILED"}")
        }
    }
}

fun main(args: Array<String>) {
    ScribeInteg()
        .subcommands(TestCommand(), LoadDataCommand(), HealthCommand())
        .main(args)
}
