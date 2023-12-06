package org.partiql.scribe.test

import java.io.File
import java.nio.file.Path
import kotlin.io.path.toPath

/**
 * Builds a little test input database over a directory.
 *
 * root/
 *  group1/
 *    tests.sql
 *  group2/
 *    tests.sql
 *
 */
class ScribeTestProvider {

    /**
     * Backing map for test input lookup.
     */
    private val map: MutableMap<ScribeTest.Key, ScribeTest> = mutableMapOf()

    /**
     * Default database of test inputs.
     */
    private val default = this::class.java.getResource("/inputs")!!.toURI().toPath()

    /**
     * Load test groups from a directory.
     */
    public fun load(root: Path? = null) {
        val dir = (root ?: default).toFile()
        dir.listFiles { f -> f.isDirectory }!!.map {
            for (test in load(it)) {
                map[test.key] = test
            }
        }
    }

    /**
     * Returns an iterator of all inputs from the provider.
     */
    public fun iterator(): Iterator<ScribeTest> = map.values.iterator()

    /**
     * Lookup a test by key
     *
     * @param key
     * @return
     */
    public operator fun get(key: ScribeTest.Key): ScribeTest? = map[key]

    /**
     * Lookup a test by key parts
     *
     * @param group
     * @param name
     * @return
     */
    public fun get(group: String, name: String): ScribeTest? = get(ScribeTest.Key(group, name))

    // load all tests in a directory
    private fun load(dir: File) = dir.listFiles()!!
        .filter { it.extension == "sql" }
        .flatMap { load(dir.name, it) }

    // load all tests in a file
    private fun load(group: String, file: File): List<ScribeTest> {
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
