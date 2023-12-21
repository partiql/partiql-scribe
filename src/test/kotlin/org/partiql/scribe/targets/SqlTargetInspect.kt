package org.partiql.scribe.targets

import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.partiql.plugins.local.LocalConnector
import org.partiql.plugins.local.LocalPlugin
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeCompiler
import org.partiql.scribe.ScribeException
import org.partiql.scribe.sql.SqlTarget
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.test.ScribeTest
import org.partiql.scribe.test.ScribeTestProvider
import org.partiql.scribe.test.SessionProvider
import org.partiql.spi.connector.ConnectorSession
import java.lang.StringBuilder
import java.util.stream.Stream
import kotlin.io.path.toPath
import kotlin.streams.asStream

/**
 * This is a special non-test suite which runs *EVERY* input query against the target for inspection purposes.
 */
class SqlTargetInspect {

    /**
     * CHANGE ME
     */
    private val target: SqlTarget = PartiQLTarget

    /**
     * Mapping of test inputs.
     */
    private val inputs = ScribeTestProvider().apply { load() }

    /**
     * Produce a session for each test key.
     */
    private val sessions = SessionProvider(
        mapOf(
            "default" to LocalConnector.Metadata(this::class.java.getResource("/catalogs/default")!!.toURI().toPath())
        )
    )

    /**
     * Each dir becomes a container and file becomes a container; each expected output is a test node.
     */
    @TestFactory
    public fun factory(): Stream<DynamicNode> {
        val scribe = ScribeCompiler.builder()
            .build()

        return inputs.iterator().asSequence().map {
            //
            val displayName = it.key.toString()
            val statement = it.statement
            val session = sessions.get(it.key)
            //
            dynamicTest(displayName) {
                try {
                    val result = scribe.compile(statement, target, session)
                    dump(it, result)
                } catch (ex: ScribeException) {
                    fail {
                        val sb = StringBuilder()
                        sb.appendLine(ex.message)
                        for (problem in ex.problems) {
                            sb.appendLine(problem)
                        }
                        sb.toString()
                    }
                }
                // val actual = result.output.value
                // val expected = it.statement
                // //
                // PlanPrinter.append(this, result.plan)
            }
        }.asStream()
    }

    @Test
    public fun subquery() {
        val plugin = LocalPlugin()
        val connector = plugin.factory.create("local", ionStructOf(
            "connector_name" to ionString("local"),
            "root" to ionString(this::class.java.getResource("/catalogs/default")!!.path)
        ))
        val connectorSession = object : ConnectorSession {
            override fun getQueryId(): String = ""
            override fun getUserId(): String = ""
        }
        val connectorMetadata = connector.getMetadata(connectorSession)
        val scribe = ScribeCompiler.builder()
            .build()
        //
        val key = ScribeTest.Key("basics", "subquery-03")
        val test = inputs.get(key)!!
        val session = sessions.get(key)
        //
        try {
            val result = scribe.compile(test.statement, target, session)
            dump(test, result)
        } catch (ex: ScribeException) {
            fail {
                val sb = StringBuilder()
                sb.appendLine(ex.message)
                for (problem in ex.problems) {
                    sb.appendLine(problem)
                }
                sb.toString()
            }
        }
    }

    private fun dump(test: ScribeTest, result: Scribe.Result<String>) {
        val message = buildString {
            appendLine("-[START]---------------------------")
            appendLine()
            appendLine(test.statement)
            appendLine()
            appendLine("-----------------------------------")
            // PlanPrinter.append(this, result.plan)
            if (result.problems.isNotEmpty()) {
                appendLine("--- HINTS --------------")
                for (problem in result.problems) {
                    appendLine(problem)
                }
                appendLine()
            }
            appendLine(result.output.toDebugString())
            appendLine("-[END]---------------------------")
        }
        println(message)
    }
}
