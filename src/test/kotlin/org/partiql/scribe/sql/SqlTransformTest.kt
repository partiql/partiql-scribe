package org.partiql.scribe.sql

import org.junit.jupiter.api.Test
import org.partiql.plan.*
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.scribe.ProblemCallback
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class SqlTransformTest {

    @Test
    fun sanity() {
        val (globals, statement) = run {
            val catalogs = listOf(
                catalog(
                    name = "Test",
                    symbols = listOf(
                        catalogSymbol(
                            path = listOf("T"),
                            type = StaticType.BAG
                        )
                    )
                ),
            )
            val scan = rel(
                type = schema("T" to StaticType.STRUCT), op = relOpScan(rex(StaticType.STRUCT, rexOpGlobal(
                    catalogSymbolRef(0,0))))
            )
            val project = rel(
                type = schema( "T" to StaticType.STRUCT),
                op = relOpProject(
                    input = scan,
                    projections = listOf(
                        rex(
                            type = StaticType.STRUCT,
                            op = rexOpStruct(
                                fields = listOf(
                                    rexOpStructField(
                                        k = rex(StaticType.STRING, rexOpLit(stringValue("a"))),
                                        v = rex(StaticType.INT,
                                            rexOpPathKey(
                                                root = rex(StaticType.INT, rexOpVar(0)),
                                                key = rex(StaticType.STRING, rexOpLit(stringValue("a")))
                                            )
                                        )
                                    ),
                                    rexOpStructField(
                                        k = rex(StaticType.STRING, rexOpLit(stringValue("b"))),
                                        v = rex(StaticType.INT,
                                            rexOpPathKey(
                                                root = rex(StaticType.INT, rexOpVar(0)),
                                                key = rex(StaticType.STRING, rexOpLit(stringValue("b")))
                                            )
                                        )
                                    ),
                                    rexOpStructField(
                                        k = rex(StaticType.STRING, rexOpLit(stringValue("c"))),
                                        v = rex(StaticType.INT,
                                            rexOpPathKey(
                                                root = rex(StaticType.INT, rexOpVar(0)),
                                                key = rex(StaticType.STRING, rexOpLit(stringValue("c")))
                                            )
                                        )
                                    ),
                                )
                            )
                        )
                    )
                )
            )

            val select = rex(
                type = StaticType.BAG,
                op = rexOpSelect(
                    constructor = rex(
                        type = StaticType.STRUCT,
                        op = rexOpVar(0)
                    ),
                    rel = project
                )
            )
            val statement = statementQuery(select)
            catalogs to statement
        }

        val case = Case(
            input = statement,
            expected = """
                SELECT VALUE {'a': "T"['a'], 'b': "T"['b'], 'c': "T"['c']} FROM "Test"."T" AS "T"
            """.trimIndent(),
            catalogs = globals,
        )
        case.assert()
    }

    @Test
    fun calls() {
        val (globals, statement) = run {
            val catalogs = listOf(
                catalog(
                    name = "Test",
                    symbols = listOf(
                        catalogSymbol(
                            path = listOf("T"),
                            type = StaticType.BAG
                        )
                    )
                ),
            )
            val plus = FunctionSignature.Scalar(
                name = "plus", returns = PartiQLValueType.INT,
                parameters = listOf(
                    FunctionParameter("lhs", PartiQLValueType.INT),
                    FunctionParameter("rhs", PartiQLValueType.INT),
                )
            )

            val abs = FunctionSignature.Scalar(
                name = "abs", returns = PartiQLValueType.INT,
                parameters = listOf(
                    FunctionParameter("value", PartiQLValueType.INT),
                )
            )
            val scan = rel(
                type = schema("T" to StaticType.STRUCT), op = relOpScan(rex(StaticType.STRUCT, rexOpGlobal(
                    catalogSymbolRef(0,0)
                )))
            )

            val var0 = rex(StaticType.STRUCT, rexOpVar(0))

            val project = rel(
                type = schema( "T" to StaticType.STRUCT),
                op = relOpProject(
                    input = scan,
                    projections = listOf(
                        rex(
                            type = StaticType.STRUCT,
                            op = rexOpStruct(
                                fields = listOf(
                                    rexOpStructField(
                                        k = rex(StaticType.STRING, rexOpLit(stringValue("a"))),
                                        v = rex(StaticType.INT,
                                            rexOpCallStatic(
                                                fn = fn(plus),
                                                args = listOf(
                                                    (rex(StaticType.INT, path(var0, "a"))),
                                                    (rex(StaticType.INT, path(var0, "b"))),
                                                )
                                            )
                                        )
                                    ),
                                    rexOpStructField(
                                        k = rex(StaticType.STRING, rexOpLit(stringValue("b"))),
                                        v = rex(StaticType.INT,
                                            rexOpCallStatic(
                                                fn = fn(abs),
                                                args = listOf(
                                                    (rex(StaticType.INT, path(var0, "c"))),
                                                )
                                            )
                                        )
                                    ),
                                )
                            )
                        ),
                    )
                )
            )

            val select = rex(
                type = StaticType.BAG,
                op = rexOpSelect(
                    constructor = rex(
                        type = StaticType.STRUCT,
                        op = rexOpVar(0)
                    ),
                    rel = project
                )
            )
            val statement = statementQuery(select)

            //
            catalogs to statement
        }
        val case = Case(
            input = statement,
            expected = """
                SELECT VALUE {'a': "T"['a'] + "T"['b'], 'b': "abs"("T"['c'])} FROM "Test"."T" AS "T"
            """.trimIndent(),
            catalogs = globals,
        )
        case.assert()
    }

    private fun schema(vararg vars: Pair<String, StaticType>): Rel.Type {
        val schema = vars.map { relBinding(it.first, it.second) }
        return relType(schema, emptySet())
    }

    companion object {

        val problemThrower: ProblemCallback = { it -> error(it.toString()) }
    }

    private class Case(
        private val input: Statement,
        private val expected: String,
        private val catalogs: List<Catalog>,
    ) {

        fun assert() {
            val transform = SqlTransform(catalogs, SqlCalls.DEFAULT, problemThrower)
            val ast = transform.apply(input)
            val actual = ast.sql(SqlLayout.ONELINE)
            assertEquals(expected, actual)
        }
    }

    private fun path(root: Rex, vararg steps: String) = rexOpPathIndex(
        root = root,
        key = rex(StaticType.STRING,rexOpLit(stringValue(steps[0])))
    )
}
