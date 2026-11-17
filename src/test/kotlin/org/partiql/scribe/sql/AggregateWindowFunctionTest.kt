package org.partiql.scribe.sql

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.WindowFunctionNode
import org.partiql.plan.rel.RelWindow
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.AggOverload
import org.partiql.spi.types.PType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Aggregate window functions in plans that PartiQL's planner does not produce. [RelWindow] and [WindowFunctionNode] are
 * public, so a plan built by other means may contain them.
 */
@Suppress("DEPRECATION")
class AggregateWindowFunctionTest {
    private val session =
        Session.builder()
            .catalog("default")
            .catalogs(Catalog.builder().name("default").build())
            .build()

    @Test
    fun `rejects an aggregate other than COUNT, SUM, AVG, MIN, and MAX`() {
        val window = planWindow("SELECT SUM(x) OVER () AS s FROM << 1, 2 >> AS x")
        val sum = window.windowFunctions.single()
        val median =
            AggOverload.Builder("median")
                .addParameter(PType.integer())
                .returns(PType.integer())
                .build()
                .getInstance(arrayOf(PType.integer()))
        val node = WindowFunctionNode(sum.signature, sum.arguments, median, false, null)
        assertEquals(
            "Unsupported aggregate window function MEDIAN",
            convertError(window, node),
        )
    }

    @Test
    fun `rejects a set quantifier without an argument`() {
        val window = planWindow("SELECT COUNT(x) OVER () AS n FROM << 1, 2 >> AS x")
        val count = window.windowFunctions.single()
        val node = WindowFunctionNode(count.signature, emptyList(), count.aggregate, true, count.routineRef)
        val message = convertError(window, node)
        assertTrue(message.startsWith("Unsupported aggregate window function COUNT: "), message)
    }

    private fun planWindow(query: String): RelWindow {
        val errors = PErrorCollector()
        val context = Context.of(errors)
        val statement = PartiQLParser.standard().parse(query, context).statements.single()
        val plan = PartiQLPlanner.standard().plan(statement, session, context).plan
        assertTrue(errors.errors.isEmpty(), errors.errors.joinToString())
        return findWindow((plan.action as Action.Query).rex) ?: error("No window in the plan of $query")
    }

    private fun findWindow(op: Operator): RelWindow? {
        if (op is RelWindow) {
            return op
        }
        val children = op.operands.flatMap { it.toList() }
        return children.firstNotNullOfOrNull { findWindow(it) }
    }

    /**
     * Converts [node] as a window function of [window], and returns the message of the expected error.
     */
    private fun convertError(
        window: RelWindow,
        node: WindowFunctionNode,
    ): String {
        val context = ScribeContext.standard()
        val transform = PlanToAst(session, SqlCalls.standard(context), context)
        val rexConverter = transform.getRexConverter(Locals(window.input.type.fields.toList()))
        val ex =
            assertFailsWith<ScribeException> {
                transform.getRelConverter().createWindowFunctionType(node, window, rexConverter)
            }
        return ex.error.getOrNull("MESSAGE", String::class.java) ?: ""
    }
}
