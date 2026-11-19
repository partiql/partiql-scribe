package org.partiql.scribe.sql

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.parser.PartiQLParser
import org.partiql.plan.RoutineRef
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.extensions.functions.RedshiftFunctionExtensionCalls
import org.partiql.scribe.targets.duckdb.DuckDBAstToSql
import org.partiql.scribe.targets.duckdb.DuckDBTarget
import org.partiql.scribe.targets.redshift.RedshiftAstToSql
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.targets.spark.SparkAstToSql
import org.partiql.scribe.targets.spark.SparkTarget
import org.partiql.scribe.targets.trino.TrinoAstToSql
import org.partiql.scribe.targets.trino.TrinoTarget
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.scribe.utils.TestCatalog
import org.partiql.scribe.utils.assertNoSqlComments
import org.partiql.spi.Context
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UnaryOperatorRenderingTest {
    private val context = ScribeContext.standard()
    private val printers =
        mapOf(
            "redshift" to RedshiftAstToSql(context),
            "trino" to TrinoAstToSql(context),
            "duckdb" to DuckDBAstToSql(context),
            "spark" to SparkAstToSql(context),
            "partiql" to object : AstToSql(context) {},
        )
    private val layouts = mapOf("standard" to SqlLayout.STANDARD, "one-line" to SqlLayout.ONELINE)

    @TestFactory
    fun `unary signs parenthesize negative literals`() =
        printers.flatMap { (name, printer) ->
            layouts.flatMap { (layoutName, layout) ->
                listOf("-" to "-(-1)", "+" to "+(-1)").map { (sign, expected) ->
                    dynamicTest("$name $layoutName $sign negative literal") {
                        val sql = printer.transform(exprOperator(sign, null, integer(-1))).sql(layout)
                        assertEquals(expected, sql)
                        assertNoSqlComments(sql)
                    }
                }
            }
        }

    @TestFactory
    fun `all leading-minus operand shapes keep token boundaries`() =
        printers.flatMap { (name, printer) ->
            layouts.flatMap { (layoutName, layout) ->
                dangerousOperands().flatMap { (shape, operand) ->
                    listOf("-", "+").map { sign ->
                        dynamicTest("$name $layoutName $sign $shape") {
                            val sql = printer.transform(exprOperator(sign, null, operand)).sql(layout)
                            assertTrue(sql.startsWith("$sign("), sql)
                            assertNoSqlComments(sql)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `nested operators never join into comments`() =
        printers.flatMap { (name, printer) ->
            layouts.flatMap { (layoutName, layout) ->
                listOf(
                    negative(negative(integer(-1))),
                    exprOperator("+", null, negative(integer(-1))),
                    exprOperator("-", variable("x"), negative(integer(-1))),
                ).mapIndexed { index, expression ->
                    dynamicTest("$name $layoutName nesting $index") {
                        assertNoSqlComments(printer.transform(expression).sql(layout))
                    }
                }
            }
        }

    @TestFactory
    fun `line breaks in identifiers aliases and strings stay quoted`() =
        printers.flatMap { (name, printer) ->
            layouts.flatMap { (layoutName, layout) ->
                listOf("\n", "\r", "\r\n").map { newline ->
                    dynamicTest("$name $layoutName quoted newline ${newline.map { it.code }}") {
                        val nameWithLineBreak = "a${newline}value"
                        val path =
                            exprPath(variable("p"), listOf(exprPathStepField(Identifier.Simple.delimited(nameWithLineBreak))))
                        val query =
                            query(
                                negative(integer(-1)),
                                path,
                                exprLit(Literal.string("text$newline-- /* data")),
                                alias = Identifier.Simple.delimited(nameWithLineBreak),
                            )
                        assertNoSqlComments(printer.transform(query).sql(layout))
                    }
                }
            }
        }

    @Test
    fun `contains rewrite keeps line breaks inside quoted tokens`() {
        val routine = RoutineRef("test", Name.of("extensions", "contains"))
        val calls = RedshiftFunctionExtensionCalls(context, setOf(routine))
        for (newline in listOf("\n", "\r", "\r\n")) {
            val path =
                exprPath(
                    variable("p"),
                    listOf(
                        exprPathStepField(Identifier.Simple.delimited("doc")),
                        exprPathStepField(Identifier.Simple.delimited("a${newline}value")),
                    ),
                )
            val contains =
                calls.routineRules.getValue(routine)(
                    listOf(
                        SqlArg(path, PType.dynamic()),
                        SqlArg(exprLit(Literal.string("x$newline-- data")), PType.string()),
                    ),
                )
            for (layout in layouts.values) {
                assertNoSqlComments(printers.getValue("redshift").transform(query(negative(integer(-1)), contains)).sql(layout))
            }
        }
    }

    @TestFactory
    fun `where clauses retain token boundaries before quoted line breaks`() =
        printers.flatMap { (name, printer) ->
            layouts.flatMap { (layoutName, layout) ->
                listOf("\n", "\r", "\r\n").map { newline ->
                    dynamicTest("$name $layoutName where newline ${newline.map { it.code }}") {
                        val path =
                            exprPath(
                                variable("p"),
                                listOf(exprPathStepField(Identifier.Simple.delimited("a${newline}value"))),
                            )
                        val condition =
                            exprOperator(
                                "AND",
                                exprOperator("=", negative(integer(-1)), integer(1)),
                                exprOperator("=", path, exprLit(Literal.string("text${newline}value"))),
                            )
                        val query =
                            exprQuerySet(
                                queryBodySFW(
                                    select = selectList(listOf(selectItemExpr(integer(1), null))),
                                    from =
                                        from(
                                            listOf(
                                                fromExpr(
                                                    variable("payload"),
                                                    FromType.SCAN(),
                                                    Identifier.Simple.regular("p"),
                                                    null,
                                                ),
                                            ),
                                        ),
                                    where = condition,
                                ),
                            )
                        assertNoSqlComments(printer.transform(query).sql(layout))
                    }
                }
            }
        }

    @Test
    fun `planner folded negative literals are protected`() {
        val catalog =
            TestCatalog.builder().name("test")
                .createTables(listOf(Name.of("payload") to PType.bag(PType.integer())))
                .build()
        val session = Session.builder().catalog("test").catalogs(catalog).build()
        for (target in listOf(RedshiftTarget(), TrinoTarget(), DuckDBTarget(), SparkTarget())) {
            for (operand in listOf("- -1", "-(CASE WHEN true THEN -1 ELSE 1 END)", "-(-1.5)", "-(-1e999)")) {
                val errors = PErrorCollector()
                val ctx = Context.of(errors)
                val parsed = PartiQLParser.standard().parse("SELECT $operand AS c FROM payload AS p", ctx)
                assertTrue(errors.errors.isEmpty(), errors.errors.joinToString())
                val statement = parsed.statements.single()
                val plan = PartiQLPlanner.standard().plan(statement, session, ctx).plan
                assertTrue(errors.errors.isEmpty(), errors.errors.joinToString())
                val sql = Scribe(context).compile(plan, session, target).output.value
                assertNoSqlComments(sql)
            }
        }
    }

    @TestFactory
    fun `generated nested expressions do not introduce SQL comments`() =
        (0 until 200).map { seed ->
            dynamicTest("nested expression seed $seed") {
                val expression = generatedExpression(Random(seed), depth = 4)
                val query =
                    query(
                        negative(expression),
                        exprLit(Literal.string("text\n-- quoted data")),
                        alias = Identifier.Simple.delimited("label\rvalue"),
                    )
                for ((name, printer) in printers) {
                    for ((layoutName, layout) in layouts) {
                        val sql = printer.transform(query).sql(layout)
                        try {
                            assertNoSqlComments(sql)
                        } catch (failure: AssertionError) {
                            throw AssertionError("$name $layoutName seed $seed: ${failure.message}", failure)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `ordinary operator formatting is unchanged`() =
        printers.map { (name, printer) ->
            dynamicTest(name) {
                assertEquals("-1", printer.transform(negative(integer(1))).sql())
                assertEquals("+1", printer.transform(exprOperator("+", null, integer(1))).sql())
                assertEquals("1 - 2", printer.transform(exprOperator("-", integer(1), integer(2))).sql())
                val x = printer.transform(variable("x")).sql()
                assertEquals("-$x", printer.transform(negative(variable("x"))).sql())
                assertEquals("-(-$x)", printer.transform(negative(negative(variable("x")))).sql())
            }
        }

    @Test
    fun `nested blocks and omitted line breaks cannot hide the first sign`() {
        val printer =
            object : AstToSql(context) {
                override fun visitExprVarRef(
                    node: ExprVarRef,
                    tail: SqlBlock,
                ): SqlBlock {
                    val child = SqlBlock.line().apply { next = SqlBlock.Text("-1") }
                    val nested = SqlBlock.Nest(null, null, child)
                    tail.next = nested
                    return nested
                }
            }
        assertEquals("-(\n-1)", printer.transform(negative(variable("x"))).sql(SqlLayout.STANDARD))
        assertEquals("-(-1)", printer.transform(negative(variable("x"))).sql(SqlLayout.ONELINE))
    }

    @Test
    fun `comment token assertion distinguishes quoted data`() {
        for (sql in listOf("SELECT '--data /*data*/'", "SELECT \"a--b\"", "SELECT `a/*b*/`", "SELECT 'a''--b'")) {
            assertNoSqlComments(sql)
        }
        for (sql in listOf("SELECT --1", "SELECT 1 /* comment */", "SELECT 1 /* unfinished")) {
            assertFailsWith<AssertionError> { assertNoSqlComments(sql) }
        }
    }

    @Test
    fun `comment token assertion rejects lexer errors`() {
        for (sql in listOf("SELECT 'unterminated", "SELECT \"unterminated", "SELECT `unterminated")) {
            assertFailsWith<AssertionError> { assertNoSqlComments(sql) }
        }
    }

    private fun dangerousOperands(): List<Pair<String, Expr>> =
        listOf(
            "integer" to integer(-1),
            "decimal" to exprLit(Literal.exactNum("-1.5")),
            "float" to exprLit(Literal.approxNum("-1e999")),
            "field path" to exprPath(negative(variable("p")), listOf(exprPathStepField(Identifier.Simple.regular("y")))),
            "indexed path" to exprPath(negative(variable("p")), listOf(exprPathStepElement(integer(0)))),
            "like" to exprLike(negative(variable("x")), exprLit(Literal.string("a")), null, false),
            "between" to exprBetween(negative(variable("x")), integer(1), integer(2), false),
            "in" to exprInCollection(negative(variable("x")), exprArray(listOf(integer(1), integer(2))), false),
        )

    private fun generatedExpression(
        random: Random,
        depth: Int,
    ): Expr {
        if (depth == 0) {
            return when (random.nextInt(5)) {
                0 -> integer(-1)
                1 -> integer(1)
                2 -> exprLit(Literal.exactNum("-1.5"))
                3 -> exprLit(Literal.approxNum("-1e9"))
                else -> variable("x")
            }
        }

        fun child() = generatedExpression(random, depth - 1)
        return when (random.nextInt(8)) {
            0 -> negative(child())
            1 -> exprOperator("+", null, child())
            2 -> exprOperator(listOf("+", "-", "*", "/")[random.nextInt(4)], child(), child())
            3 -> exprPath(child(), listOf(exprPathStepField(Identifier.Simple.delimited("field\nvalue"))))
            4 -> exprPath(child(), listOf(exprPathStepElement(integer(0))))
            5 -> exprLike(child(), exprLit(Literal.string("a\rvalue")), null, false)
            6 -> exprBetween(child(), integer(1), integer(2), false)
            else -> exprInCollection(child(), exprArray(listOf(integer(1), integer(2))), false)
        }
    }

    private fun integer(value: Int) = exprLit(Literal.intNum(value))

    private fun variable(name: String) = exprVarRef(Identifier.regular(name), false)

    private fun negative(operand: Expr) = exprOperator("-", null, operand)

    private fun query(
        vararg expressions: Expr,
        alias: Identifier.Simple? = null,
    ) = exprQuerySet(
        queryBodySFW(
            select = selectList(expressions.map { selectItemExpr(it, alias) }),
            from = from(listOf(fromExpr(variable("payload"), FromType.SCAN(), Identifier.Simple.regular("p"), null))),
        ),
    )
}
