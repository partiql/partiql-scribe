package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.FromExpr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.parser.PartiQLParser
import org.partiql.planner.builder.PartiQLPlannerBuilder
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.problems.Severity
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.scribe.utils.ScribeTestProvider
import org.partiql.scribe.utils.SessionProvider
import org.partiql.scribe.utils.SqlEqualsNaive
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The conformance suite of the Redshift front end. Each case is a Redshift input in `redshift-frontend/inputs` and the
 * expected result of the same name in `redshift-frontend/outputs`: the input is converted to a PartiQL AST, planned
 * against the Scribe test catalogs, and compiled with the PartiQL target.
 *
 * An expected output is either PartiQL text, or the problem that stops the translation, as its code and a substring of
 * its message: `[UNSUPPORTED_OPERATION{message="function UPPER"}];`. A problem the planner reports (an unknown name, an
 * operation PartiQL does not define on the operand types) is `[PLANNER{message="..."}];`, with the planner's own code
 * in the message. A valid output is also parsed and planned again, as a check that the translation produced the query
 * it meant to.
 *
 * A case whose marker is `-- --#[name] TODO(redshift)` waits for the change that translates its construct; that change
 * enables it. Whatever its marker, every input is parsed by [grammar], so the grammar covers the whole suite.
 */
class RedshiftTranspilerSuite {
    private val inputs = load("/redshift-frontend/inputs")
    private val outputs = load("/redshift-frontend/outputs")
    private val session = SessionProvider().getSession()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlannerBuilder().forceInlineWithClause(replaceWith = false).build()

    /** Every input parses, except an input whose expected output is the syntax error. */
    @TestFactory
    fun grammar(): List<DynamicNode> =
        inputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { input ->
                        dynamicTest("$group/${input.key.name}") {
                            val expectedFailure = outputs[input.key]?.let { FAILURE.matchEntire(body(it.statement)) }
                            try {
                                RedshiftFrontend(ScribeContext.standard()).toAst(input.statement)
                            } catch (ex: ScribeException) {
                                val kind = ex.error.getOrNull(RedshiftFrontend.KIND, String::class.java)
                                if (kind != RedshiftFrontend.KIND_SYNTAX) return@dynamicTest
                                val (code, message) = expectedFailure?.destructured ?: fail("Input: ${input.statement}\n$ex")
                                assertEquals("UNSUPPORTED_OPERATION", code, "Input: ${input.statement}\n$ex")
                                assertTrue(ex.error.message().contains(message), "Expected message to contain '$message'; got: $ex")
                            }
                        }
                    },
                )
            }

    /** The tables of every input that converts are the tables its AST reads. */
    @TestFactory
    fun tables(): List<DynamicNode> =
        inputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { input ->
                        dynamicTest("$group/${input.key.name}") {
                            val frontend = RedshiftFrontend(ScribeContext.standard())
                            val tables =
                                try {
                                    frontend.tables(input.statement)
                                } catch (ex: ScribeException) {
                                    assertThrows<ScribeException> { frontend.toAst(input.statement) }
                                    return@dynamicTest
                                }
                            val ast = frontend.toAst(input.statement)
                            val read = mutableSetOf<List<String>>()
                            val visitor =
                                object : AstVisitor<Unit, Unit>() {
                                    override fun defaultReturn(
                                        node: AstNode,
                                        ctx: Unit,
                                    ) = Unit

                                    override fun visitExprQuerySet(
                                        node: ExprQuerySet,
                                        ctx: Unit,
                                    ) {
                                        // The children of a query set do not include its WITH clause.
                                        node.with?.accept(this, ctx)
                                        super.visitExprQuerySet(node, ctx)
                                    }

                                    override fun visitFromExpr(
                                        node: FromExpr,
                                        ctx: Unit,
                                    ) {
                                        // A FROM item that is a global name is a table; a local one (`@name`) is a common table expression.
                                        val ref = node.expr as? ExprVarRef
                                        if (ref != null && !ref.isQualified) {
                                            read.add(
                                                ref.identifier.qualifier.map { it.text } + ref.identifier.identifier.text,
                                            )
                                        }
                                        super.visitFromExpr(node, ctx)
                                    }
                                }
                            ast.accept(visitor, Unit)
                            assertEquals(read, tables.toSet(), "Input: ${input.statement}")
                        }
                    },
                )
            }

    @TestFactory
    fun suite(): List<DynamicNode> =
        outputs.iterator().asSequence()
            .groupBy { it.key.group }
            .toSortedMap()
            .map { (group, tests) ->
                dynamicContainer(
                    group,
                    tests.sortedBy { it.key.name }.map { expected ->
                        // The group is in the name too: a test report does not show the container of a dynamic test.
                        dynamicTest("$group/${expected.key.name}") {
                            val input = inputs[expected.key] ?: fail("No input for ${expected.key}")
                            check(input.statement, expected.statement)
                        }
                    },
                )
            }

    /** The test provider keeps one test for each group and name, so a repeated name would hide a test. */
    @Test
    fun testNamesAreUniqueInEachGroup() {
        for (dir in listOf("inputs", "outputs")) {
            val root = this::class.java.getResource("/redshift-frontend/$dir")!!.toURI().toPath().toFile()
            for (group in root.listFiles { f -> f.isDirectory }!!) {
                val names =
                    group.listFiles { f -> f.extension == "sql" }!!.flatMap { file ->
                        file.readLines().mapNotNull { NAME.matchEntire(it)?.groupValues?.get(1) }
                    }
                val repeated = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                assertTrue(repeated.isEmpty(), "Repeated test names in $dir/${group.name}: $repeated")
            }
        }
    }

    private fun check(
        input: String,
        expectedText: String,
    ) {
        val expected = body(expectedText)
        val expectedFailure = FAILURE.matchEntire(expected)
        val problems = mutableListOf<ScribeProblem>()
        val listener =
            object : ScribeProblemListener {
                override fun report(problem: ScribeProblem) {
                    problems.add(problem)
                }
            }
        val context =
            object : ScribeContext {
                override fun getProblemListener() = listener
            }
        val actual =
            try {
                val ast = RedshiftFrontend(context).toAst(input)
                val plannerProblems = PErrorCollector()
                val plan = planner.plan(ast, session, Context.of(plannerProblems)).plan
                val unusable = plannerProblems.errors + plannerProblems.warnings.filter { it.code() in UNRESOLVED }
                if (unusable.isNotEmpty()) {
                    val (code, message) = expectedFailure?.destructured ?: fail("Input: $input\nPlanner: $unusable")
                    assertEquals("PLANNER", code, "Input: $input\nPlanner: $unusable")
                    assertTrue(
                        unusable.first().toString().contains(message),
                        "Expected planner problem to contain '$message'; got: $unusable",
                    )
                    return
                }
                Scribe(context).compile(plan, session, PartiQLTarget).output.value
            } catch (ex: ScribeException) {
                val (code, message) =
                    expectedFailure?.destructured ?: fail("Input: $input\nUnexpected failure: ${ex.error}\nProblems: $problems")
                assertEquals(code, ex.error.name(), "Input: $input\nProblem: ${ex.error}")
                assertTrue(ex.error.message().contains(message), "Expected message to contain '$message'; got: ${ex.error}")
                return
            }
        if (expectedFailure != null) fail("Input: $input\nExpected [${expectedFailure.groupValues[1]}], got: $actual")
        val errors = problems.filter { it.severity.code() == Severity.ERROR }
        assertTrue(errors.isEmpty(), "Input: $input\nErrors reported without a failure: $errors")
        SqlEqualsNaive.assertEquals(expected, actual) {
            appendLine("Input: $input")
            appendLine("Problems: $problems")
        }
        verify(actual, session)
    }

    /** The output is valid PartiQL: it parses, and it plans against the same session without an error. */
    private fun verify(
        partiql: String,
        session: Session,
    ) {
        val collector = PErrorCollector()
        val context = Context.of(collector)
        val statements = parser.parse(partiql, context).statements
        assertEquals(1, statements.size, "Output is not one statement: $partiql\n${collector.errors}")
        planner.plan(statements[0], session, context)
        val errors = collector.errors + collector.warnings.filter { it.code() in UNRESOLVED }
        assertTrue(errors.isEmpty(), "Output does not plan: $partiql\n$errors")
    }

    private fun ScribeProblem.message(): String = getOrNull("MESSAGE", String::class.java) ?: toString()

    private fun body(statement: String): String = statement.lines().filterNot { it.startsWith("--#[") }.joinToString("\n").trim()

    private fun load(path: String) = ScribeTestProvider().apply { load(this::class.java.getResource(path)!!.toURI().toPath()) }

    private companion object {
        val FAILURE = Regex("""\[(\w+)(?:\{message="(.*)"})?];""")
        val NAME = Regex("""--#\[(.+)]""")

        /** Planner warnings that make a plan unusable: a name or an operation that did not resolve. */
        val UNRESOLVED =
            setOf(
                PError.VAR_REF_NOT_FOUND,
                PError.VAR_REF_AMBIGUOUS,
                PError.FUNCTION_NOT_FOUND,
                PError.FUNCTION_TYPE_MISMATCH,
                PError.FUNCTION_AMBIGUOUS,
                PError.UNDEFINED_CAST,
                PError.PATH_KEY_NEVER_SUCCEEDS,
                PError.ALWAYS_MISSING,
            )
    }
}
