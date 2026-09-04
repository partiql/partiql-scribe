package org.partiql.scribe.extensions.functions

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeException
import org.partiql.scribe.sql.AstToSql
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.targets.redshift.RedshiftAstToSql
import org.partiql.scribe.targets.spark.SparkAstToSql
import org.partiql.scribe.targets.trino.TrinoAstToSql
import org.partiql.spi.catalog.Name
import org.partiql.spi.types.PType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FunctionExtensionCallsTest {
    private val routines = NAMES.map(::ref).toSet()

    @Test
    fun `rejects an unsupported binding`() {
        assertFailsWith<IllegalArgumentException> {
            TestSparkCalls(ScribeContext.standard(), setOf(ref("missing")))
        }
    }

    @Test
    fun `date add rejects a dynamic datetime part`() {
        val calls = TestSparkCalls(ScribeContext.standard(), routines)
        val args =
            args("date_add").toMutableList().apply {
                this[0] = SqlArg(exprVarRef(Identifier.regular("part"), false), PType.string())
            }

        val error = assertFailsWith<ScribeException> { calls.translate("date_add", args) }

        assertEquals("UNSUPPORTED_OPERATION", error.error.name())
    }

    @Test
    fun `date add rejects an unsupported datetime part`() {
        val calls = TestSparkCalls(ScribeContext.standard(), routines)
        val args =
            args("date_add").toMutableList().apply {
                this[0] = SqlArg(exprLit(Literal.string("week")), PType.string())
            }

        val error = assertFailsWith<ScribeException> { calls.translate("date_add", args) }

        assertEquals("UNSUPPORTED_OPERATION", error.error.name())
    }

    @Test
    fun `redshift contains rejects an invalid collection`() {
        val calls = TestRedshiftCalls(ScribeContext.standard(), routines)
        val args =
            args("contains").toMutableList().apply {
                this[0] = SqlArg(exprVarRef(Identifier.regular("items"), false), PType.array())
            }

        val error = assertFailsWith<ScribeException> { calls.translate("contains", args) }

        assertEquals("UNSUPPORTED_OPERATION", error.error.name())
    }

    @Test
    fun `redshift contains rejects an unnamed path`() {
        val calls = TestRedshiftCalls(ScribeContext.standard(), routines)
        val args =
            args("contains").toMutableList().apply {
                this[0] =
                    SqlArg(
                        exprPath(
                            exprVarRef(Identifier.regular("items"), false),
                            listOf(exprPathStepElement(exprVarRef(Identifier.regular("index"), false))),
                        ),
                        PType.array(),
                    )
            }

        val error = assertFailsWith<ScribeException> { calls.translate("contains", args) }

        assertEquals("UNSUPPORTED_OPERATION", error.error.name())
    }

    @Test
    fun `spark extension translations`() {
        val context = ScribeContext.standard()
        val calls = TestSparkCalls(context, routines)

        assertTranslations(
            SparkAstToSql(context),
            calls::translate,
            mapOf(
                "date_add" to "`ts` + `make_interval`(0, 0, 0, 1, 0, 0, 0)",
                "utcnow" to "`convert_timezone`('UTC', `current_timestamp`())",
                "contains" to "`array_contains`(`t`.`items`, 'x')",
                "hex_to_bigint" to "CAST(`conv`('00C10300', 16, 10) AS BIGINT)",
                "to_unixtime" to "CAST(`unix_timestamp`(`ts`) AS BIGINT)",
                "pow" to "`pow`(2, 3)",
            ),
        )
    }

    @Test
    fun `trino extension translations`() {
        val context = ScribeContext.standard()
        val calls = TestTrinoCalls(context, routines)

        assertTranslations(
            TrinoAstToSql(context),
            calls::translate,
            mapOf(
                "date_add" to "date_add('day', 1, ts)",
                "utcnow" to "at_timezone(current_timestamp, 'UTC')",
                "contains" to "\"contains\"(t.items, 'x')",
                "hex_to_bigint" to "from_base('00C10300', 16)",
                "to_unixtime" to "CAST(to_unixtime(ts) AS BIGINT)",
                "pow" to "\"pow\"(2, 3)",
            ),
        )
    }

    @Test
    fun `redshift extension translations`() {
        val context = ScribeContext.standard()
        val calls = TestRedshiftCalls(context, routines)

        assertTranslations(
            RedshiftAstToSql(context),
            calls::translate,
            mapOf(
                "date_add" to "DATEADD(DAY, 1, ts)",
                "utcnow" to "sysdate",
                "contains" to "1 <= (SELECT COUNT(items) FROM t.items AS items WHERE items IN ('x'))",
                "hex_to_bigint" to "STRTOL('00C10300', 16)",
                "to_unixtime" to "CAST(DATE_PART(EPOCH, ts) AS BIGINT)",
                "pow" to "\"pow\"(2, 3)",
            ),
        )
    }

    private fun assertTranslations(
        dialect: AstToSql,
        translate: (String, SqlArgs) -> Expr,
        expected: Map<String, String>,
    ) {
        expected.forEach { (name, expectedSql) ->
            val sql = dialect.transform(translate(name, args(name))).sql(SqlLayout.STANDARD)
            assertEquals(expectedSql, sql, name)
        }
    }

    private fun args(name: String): SqlArgs =
        when (name) {
            "date_add" ->
                listOf(
                    SqlArg(exprLit(Literal.string("day")), PType.string()),
                    SqlArg(exprLit(Literal.intNum(1)), PType.integer()),
                    SqlArg(exprVarRef(Identifier.regular("ts"), false), PType.timestamp()),
                )
            "utcnow" -> emptyList()
            "contains" ->
                listOf(
                    SqlArg(
                        exprPath(
                            exprVarRef(Identifier.regular("t"), false),
                            listOf(exprPathStepField(Identifier.Simple.regular("items"))),
                        ),
                        PType.array(),
                    ),
                    SqlArg(exprLit(Literal.string("x")), PType.string()),
                )
            "hex_to_bigint" -> listOf(SqlArg(exprLit(Literal.string("00C10300")), PType.string()))
            "to_unixtime" -> listOf(SqlArg(exprVarRef(Identifier.regular("ts"), false), PType.timestamp()))
            "pow" ->
                listOf(
                    SqlArg(exprLit(Literal.intNum(2)), PType.integer()),
                    SqlArg(exprLit(Literal.intNum(3)), PType.integer()),
                )
            else -> error("Unknown function extension: $name")
        }

    private class TestSparkCalls(
        context: ScribeContext,
        routines: Set<RoutineRef>,
    ) : SparkFunctionExtensionCalls(context, routines) {
        override fun default(
            name: String,
            args: SqlArgs,
        ): Expr = error("Extension translation fell through to the host default: $name")

        fun translate(
            name: String,
            args: SqlArgs,
        ): Expr = routineRules.getValue(ref(name))(args)
    }

    private class TestTrinoCalls(
        context: ScribeContext,
        routines: Set<RoutineRef>,
    ) : TrinoFunctionExtensionCalls(context, routines) {
        fun translate(
            name: String,
            args: SqlArgs,
        ): Expr = routineRules.getValue(ref(name))(args)
    }

    private class TestRedshiftCalls(
        context: ScribeContext,
        routines: Set<RoutineRef>,
    ) : RedshiftFunctionExtensionCalls(context, routines) {
        fun translate(
            name: String,
            args: SqlArgs,
        ): Expr = routineRules.getValue(ref(name))(args)
    }

    private companion object {
        val NAMES = listOf("date_add", "utcnow", "contains", "hex_to_bigint", "to_unixtime", "pow")

        fun ref(name: String): RoutineRef = RoutineRef("test", Name.of(name))
    }
}
