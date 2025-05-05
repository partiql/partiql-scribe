package org.partiql.scribe.targets

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.partiql.scribe.Scribe
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.sql.SqlTarget
import org.partiql.scribe.utils.ScribeTest
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import org.partiql.scribe.utils.SqlEqualsNaive
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Produces a junit suite from a directory of expected outputs.
 */
abstract class SqlTargetSuite {
    /**
     * The [SqlTarget] to compile with.
     */
    abstract val target: SqlTarget

    /**
     * The expected output root
     */
    abstract val root: Path

    /**
     * Produce a session for each test key.
     */
    abstract val sessions: SessionProvider

    /**
     * Mapping of test inputs.
     */
    private val inputs = ScribeTestProvider().apply { load() }

    /**
     * [SqlEquals] for assertions
     */
    private val comparator = SqlEqualsNaive

    /**
     * Each dir becomes a container and file becomes a container; each expected output is a test node.
     */
    @TestFactory
    public fun factory(): Stream<DynamicNode> {
        val r = root.toFile()
        return r
            .listFiles { f -> f.isDirectory }!!
            .mapNotNull { load(r, it) }
            .stream()
    }

    private fun load(
        parent: File,
        file: File,
    ): DynamicNode? =
        when {
            file.isDirectory -> loadD(parent, file)
            file.extension == "sql" -> loadF(parent, file)
            else -> null
        }

    private fun loadD(
        parent: File,
        file: File,
    ): DynamicContainer {
        val name = file.name
        val children = file.listFiles()!!.map { load(file, it) }
        return dynamicContainer(name, children)
    }

    // load all tests in a file
    private fun loadF(
        parent: File,
        file: File,
    ): DynamicContainer {
        val group = parent.name
        val tests = parse(group, file)
        val scribe = Scribe.STANDARD

        val children =
            tests.map { test ->
                // Prepare
                val displayName = test.key.toString()
//            val session = sessions.get(test.key)
                val session = sessions.getSession()
                val statement = (inputs[test.key] ?: error("No test with key ${test.key}")).statement

                // Assert
                dynamicTest(displayName) {
                    try {
                        val result = scribe.compile(statement, target, session)
                        val actual = result.output.value
                        val expected = test.statement
                    /*
                    val problems = result.problems.filter { it.level == ScribeProblem.Level.ERROR }
                    if (problems.isNotEmpty()) {
                        fail {
                            buildString {
                                for (error in problems) {
                                    appendLine(error)
                                }
                                PlanPrinter.append(this, result.input)
                            }
                        }
                    }
                     */
                        comparator.assertEquals(expected, actual) {
                            this.appendLine("Input Query: $statement")
                            this.appendLine("Expected result: $expected")
                            this.appendLine("Actual result: $actual")
                            // debug dump
//                        PlanPrinter.append(this, result.input)
                        }
                    } catch (ex: ScribeException) {
                        fail {
                            buildString {
                                appendLine(ex.message)
//                            for (problem in ex.problems) {
//                                appendLine(problem)
//                            }
                            }
                        }
                    }
                }
            }
        //
        return dynamicContainer(file.nameWithoutExtension, children)
    }

    // from org.partiql.planner.testFixtures
    private fun parse(
        group: String,
        file: File,
    ): List<ScribeTest> {
        val tests = mutableListOf<ScribeTest>()
        var name = ""
        val statement = StringBuilder()
        for (line in file.readLines()) {
            // start of test
            if (line.startsWith("--#[") and line.endsWith("]")) {
                name = line.substring(4, line.length - 1)
                statement.clear()
            }
            if (name.isNotEmpty() && line.isNotBlank()) {
                // accumulating test statement
                statement.appendLine(line)
            } else {
                // skip these lines
                continue
            }
            // Finish & Reset
            if (line.endsWith(";")) {
                val key = ScribeTest.Key(group, name)
                tests.add(ScribeTest(key, statement.toString()))
                name = ""
                statement.clear()
            }
        }
        return tests
    }
}
