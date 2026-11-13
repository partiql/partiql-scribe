package org.partiql.scribe.sql.utils

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.let
import org.partiql.ast.Ast.letBinding
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.FromType
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.duckdb.DuckDBAstToSql
import org.partiql.scribe.targets.partiql.PartiQLTarget
import org.partiql.scribe.targets.redshift.RedshiftAstToSql
import org.partiql.scribe.targets.spark.SparkAstToSql
import org.partiql.scribe.targets.trino.TrinoAstToSql
import kotlin.test.assertEquals

class IdentifierUtilsTest {
    @Test
    fun `quoteIdentifier doubles the quote character`() {
        assertEquals("\"\"", quoteIdentifier(""))
        assertEquals("\"abc\"", quoteIdentifier("abc"))
        assertEquals("\"a\"\"b\"", quoteIdentifier("a\"b"))
        assertEquals("\"\"\"\"\"\"", quoteIdentifier("\"\""))
        assertEquals("\"a`b\"", quoteIdentifier("a`b"))
        assertEquals("`a``b`", quoteIdentifier("a`b", '`'))
        assertEquals("`a\"b`", quoteIdentifier("a\"b", '`'))
    }

    @Test
    fun `every target escapes its quote character in a delimited identifier`() {
        val context = ScribeContext.standard()
        val field = "e\"`),f"
        val expr =
            exprPath(
                exprVarRef(Identifier.delimited("t\"`"), false),
                listOf(exprPathStepField(Identifier.Simple.delimited(field))),
            )
        val doubleQuoted = "\"t\"\"`\".\"e\"\"`),f\""
        val backtickQuoted = "`t\"```.`e\"``),f`"
        assertEquals(doubleQuoted, RedshiftAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(doubleQuoted, TrinoAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(doubleQuoted, DuckDBAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(backtickQuoted, SparkAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
    }

    /**
     * A Scribe plan never produces `AT`, `LET`, `GROUP AS`, or a `GROUP BY` key alias, so this hand-built AST checks
     * that every alias position still escapes the delimiter.
     */
    @Test
    fun `every alias position escapes the quote character`() {
        val context = ScribeContext.standard()
        val query =
            exprQuerySet(
                queryBodySFW(
                    select = selectList(listOf(selectItemExpr(v("x"), Identifier.Simple.delimited("c\"`")))),
                    from =
                        from(
                            listOf(
                                fromExpr(
                                    v("T"),
                                    FromType.SCAN(),
                                    asAlias = Identifier.Simple.delimited("t\"`"),
                                    atAlias = Identifier.Simple.delimited("i\"`"),
                                ),
                            ),
                        ),
                    let = let(listOf(letBinding(v("y"), Identifier.Simple.delimited("l\"`")))),
                    groupBy =
                        groupBy(
                            GroupByStrategy.FULL(),
                            listOf(groupByKey(v("z"), Identifier.Simple.delimited("k\"`"))),
                            asAlias = Identifier.Simple.delimited("g\"`"),
                        ),
                ),
            )
        val doubleQuoted =
            "SELECT x AS \"c\"\"`\" FROM T AS \"t\"\"`\" AT \"i\"\"`\" LET y AS \"l\"\"`\" " +
                "GROUP BY z AS \"k\"\"`\" GROUP AS \"g\"\"`\""
        assertEquals(doubleQuoted, RedshiftAstToSql(context).transform(query).sql(SqlLayout.ONELINE))
        assertEquals(doubleQuoted, PartiQLTarget.getAstToSql(context).transform(query).sql(SqlLayout.ONELINE))
        // The Trino, DuckDB, and Spark FROM printers omit AT, which feature validation rejects for those targets.
        val noAt = doubleQuoted.replace(" AT \"i\"\"`\"", "")
        assertEquals(noAt, TrinoAstToSql(context).transform(query).sql(SqlLayout.ONELINE))
        assertEquals(noAt, DuckDBAstToSql(context).transform(query).sql(SqlLayout.ONELINE))
        // Spark also prints every identifier delimited.
        assertEquals(
            "SELECT `x` AS `c\"``` FROM `T` AS `t\"``` LET `y` AS `l\"``` GROUP BY `z` AS `k\"``` GROUP AS `g\"```",
            SparkAstToSql(context).transform(query).sql(SqlLayout.ONELINE),
        )
    }

    @Test
    fun `a regular identifier that is not a valid unquoted identifier is quoted`() {
        val context = ScribeContext.standard()
        val cases =
            listOf(
                "a" to "a",
                "_a1\$" to "_a1\$",
                "Abc" to "Abc",
                "a b" to "\"a b\"",
                "1a" to "\"1a\"",
                "\$a" to "\"\$a\"",
                "a\"b" to "\"a\"\"b\"",
                "f(1)" to "\"f(1)\"",
                "a.b" to "\"a.b\"",
                "" to "\"\"",
            )
        for ((text, expected) in cases) {
            val expr = exprVarRef(Identifier.regular(text), false)
            assertEquals(expected, RedshiftAstToSql(context).transform(expr).sql(SqlLayout.ONELINE), text)
            assertEquals(expected, TrinoAstToSql(context).transform(expr).sql(SqlLayout.ONELINE), text)
            assertEquals(expected, DuckDBAstToSql(context).transform(expr).sql(SqlLayout.ONELINE), text)
            assertEquals(expected, PartiQLTarget.getAstToSql(context).transform(expr).sql(SqlLayout.ONELINE), text)
        }
        // Spark delimits every identifier.
        val spark = SparkAstToSql(context).transform(exprVarRef(Identifier.regular("a b`"), false))
        assertEquals("`a b```", spark.sql(SqlLayout.ONELINE))
    }

    private fun v(name: String) = exprVarRef(Identifier.regular(name), false)
}
