package org.partiql.scribe.sql.utils

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Identifier
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.duckdb.DuckDBAstToSql
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
        val field = "x\"`); DROP TABLE target--"
        val expr =
            exprPath(
                exprVarRef(Identifier.delimited("t\"`"), false),
                listOf(exprPathStepField(Identifier.Simple.delimited(field))),
            )
        val doubleQuoted = "\"t\"\"`\".\"x\"\"`); DROP TABLE target--\""
        val backtickQuoted = "`t\"```.`x\"``); DROP TABLE target--`"
        assertEquals(doubleQuoted, RedshiftAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(doubleQuoted, TrinoAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(doubleQuoted, DuckDBAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
        assertEquals(backtickQuoted, SparkAstToSql(context).transform(expr).sql(SqlLayout.STANDARD))
    }
}
