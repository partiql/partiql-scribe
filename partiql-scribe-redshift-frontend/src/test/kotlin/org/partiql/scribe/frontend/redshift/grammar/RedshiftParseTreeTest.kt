package org.partiql.scribe.frontend.redshift.grammar

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the shape of the parse tree where the grammar has to choose: operator precedence and associativity, UNION
 * associativity, join associativity, and the tokens that have more than one reading (keywords that are also
 * identifiers, `DATE` as a literal prefix and as a name, `||` before a comment, an alias after a literal).
 *
 * The trees are rendered by [render]: a rule with a single child collapses into it, a rule with several children is
 * `(label child ...)` where the label is the alternative's label in `Redshift.g4`.
 */
class RedshiftParseTreeTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun tree(
        sql: String,
        expected: String,
    ) {
        val result = parse(sql)
        assertTrue(result.isValid, "Errors: ${result.errors}")
        assertEquals(expected, result.tree.render())
    }

    companion object {
        private fun case(
            sql: String,
            expected: String,
        ) = Arguments.of(sql, expected)

        /**
         * The rendering of `SELECT item FROM table`.
         */
        private fun spec(
            item: String,
            table: String,
        ) = "(querySpecification SELECT $item (fromClause FROM $table))"

        private fun eq(
            a: String,
            b: String,
        ) = "(predicateComparison (qualifiedName $a) = (qualifiedName $b))"

        /**
         * The expression `e` in `SELECT e FROM t`, rendered.
         */
        private fun expr(
            e: String,
            tree: String,
        ) = case("SELECT $e FROM t", "(root ${spec(tree, "t")} <EOF>)")

        @JvmStatic
        fun cases() =
            listOf(
                // Arithmetic: `*` before `+`, left-associative, `^` before `*`, unary sign before `^`.
                expr("a + b * c", "(valueAdditive a + (valueMultiplicative b * c))"),
                expr("a - b - c", "(valueAdditive (valueAdditive a - b) - c)"),
                expr("a * b ^ c", "(valueMultiplicative a * (valueExponent b ^ c))"),
                expr("-a ^ b", "(valueExponent (valueSigned - a) ^ b)"),
                // `::` binds tightest: `-1::INT` is the negation of a cast, `t.a::DECIMAL` casts the column.
                expr("-1::INT", "(valueSigned - (valueCastShorthand 1 :: INT))"),
                expr("t.a::DECIMAL(10, 2)", "(valueCastShorthand (qualifiedName t . a) :: (typeDecimal DECIMAL ( 10 , 2 )))"),
                expr("a::INT::VARCHAR", "(valueCastShorthand (valueCastShorthand a :: INT) :: VARCHAR)"),
                // `||` and `&` bind below `+` and `-`, as in PostgreSQL.
                expr("a || b + c", "(valueOperator a || (valueAdditive b + c))"),
                expr("a || b || c", "(valueOperator (valueOperator a || b) || c)"),
                expr("a & b + c", "(valueOperator a & (valueAdditive b + c))"),
                // Comparison below arithmetic; NOT, AND, OR in that order; NOT binds tighter than AND.
                expr("a + b = c * d", "(predicateComparison (valueAdditive a + b) = (valueMultiplicative c * d))"),
                expr("a OR b AND c", "(or a OR (and b AND c))"),
                expr("NOT a AND b", "(and (not NOT a) AND b)"),
                expr("a AND b OR c AND d", "(or (and a AND b) OR (and c AND d))"),
                expr("NOT a = b", "(not NOT (predicateComparison a = b))"),
                // BETWEEN takes its own AND; the enclosing AND is a conjunction.
                expr("a BETWEEN 1 AND 10 AND b", "(and (predicateBetween a BETWEEN 1 AND 10) AND b)"),
                expr("a BETWEEN b + 1 AND c * 2", "(predicateBetween a BETWEEN (valueAdditive b + 1) AND (valueMultiplicative c * 2))"),
                // Predicates over operator expressions.
                expr("a || b LIKE 'x%'", "(predicateLike (valueOperator a || b) LIKE 'x%')"),
                expr("a IS NOT NULL", "(predicateNull a IS NOT NULL)"),
                expr("a ISNULL", "(predicateNullPostfix a ISNULL)"),
                expr("a NOT IN (1, 2)", "(predicateInList a NOT IN ( 1 , 2 ))"),
                expr("a IN (SELECT b FROM u)", "(predicateInQuery a IN ( ${spec("b", "u")} ))"),
                // Datetime arithmetic: an INTERVAL literal is an operand of `+`.
                expr("d + INTERVAL '1 day'", "(valueAdditive d + (literalInterval INTERVAL '1 day'))"),
                // Literal prefixes versus names: `DATE '...'` is a literal, `date` alone is a column.
                expr("DATE '2024-01-01'", "(literalDate DATE '2024-01-01')"),
                expr("date", "date"),
                expr(
                    "TIMESTAMP WITHOUT TIME ZONE '2024-01-01 00:00:00'",
                    "(literalTimestamp TIMESTAMP WITHOUT TIME ZONE '2024-01-01 00:00:00')",
                ),
                // Numeric literals; a bare `e3` after a literal is an alias.
                expr(".5", ".5"),
                expr("5.", "5."),
                expr("1 e3", "(selectItemExpr 1 e3)"),
                expr("a x", "(selectItemExpr a x)"),
                expr("a AS x", "(selectItemExpr a AS x)"),
                // `||` followed by a comment is a concatenation, not the cube root.
                expr("a ||/* comment */ b", "(valueOperator a || b)"),
                // Non-reserved keywords, including the function names, are identifiers when not followed by `(`.
                expr("year", "year"),
                expr("count", "count"),
                expr("max + min", "(valueAdditive max + min)"),
                // Names: a column, a qualified column, a qualified star.
                expr("s.t.a", "(qualifiedName s . t . a)"),
                case("SELECT t.* FROM t", "(root ${spec("(selectItemStar t . *)", "t")} <EOF>)"),
                // Functions: each has its own form.
                expr("COUNT(*)", "(atomCountStar COUNT ( * ))"),
                expr("COUNT(DISTINCT a)", "(atomAggregate COUNT ( DISTINCT a ))"),
                expr("SUM(a)", "(atomAggregate SUM ( a ))"),
                expr("NVL(a, 0)", "(atomFunctionCall NVL ( a , 0 ))"),
                expr("DATEADD(day, 1, d)", "(atomDateFunction DATEADD ( day , 1 , d ))"),
                expr("EXTRACT(EPOCH FROM ts)", "(atomExtractEpoch EXTRACT ( EPOCH FROM ts ))"),
                expr("GETDATE()", "(atomGetdate GETDATE ( ))"),
                expr("SYSDATE", "SYSDATE"),
                expr("CURRENT_DATE", "CURRENT_DATE"),
                expr("CAST(a AS VARCHAR(10))", "(atomCast CAST ( a AS (typeVarchar VARCHAR (typeLength ( 10 ))) ))"),
                expr("CAST(a AS DOUBLE PRECISION)", "(atomCast CAST ( a AS (typeDouble DOUBLE PRECISION) ))"),
                expr("CONVERT(INT, a)", "(atomConvert CONVERT ( INT , a ))"),
                expr("CASE WHEN a THEN 1 ELSE 0 END", "(atomCase CASE (caseWhen WHEN a THEN 1) ELSE 0 END)"),
                expr("CASE a WHEN 1 THEN 'x' END", "(atomCase CASE a (caseWhen WHEN 1 THEN 'x') END)"),
                expr("(SELECT MAX(b) FROM u)", "(atomSubquery ( ${spec("(atomAggregate MAX ( b ))", "u")} ))"),
                // UNION is left-associative; ORDER BY and LIMIT after it apply to its result.
                case(
                    "SELECT a FROM t UNION SELECT b FROM u UNION ALL SELECT c FROM v",
                    "(root (queryTermUnion (queryTermUnion ${spec("a", "t")} UNION ${spec("b", "u")}) UNION ALL ${spec("c", "v")}) <EOF>)",
                ),
                case(
                    "SELECT a FROM t UNION SELECT a FROM u ORDER BY 1 LIMIT 5",
                    "(root (query (queryTermUnion ${spec("a", "t")} UNION ${spec("a", "u")}) " +
                        "(orderByClause ORDER BY 1) (limitClause LIMIT 5)) <EOF>)",
                ),
                // Joins are left-associative; a parenthesized join groups.
                case(
                    "SELECT 1 FROM a JOIN b ON a.x = b.x LEFT JOIN c ON b.y = c.y",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableJoin (tableJoin a JOIN b ON ${eq("a . x", "b . x")}) " +
                        "LEFT JOIN c ON ${eq("b . y", "c . y")}))) <EOF>)",
                ),
                case(
                    "SELECT 1 FROM a JOIN (b JOIN c ON b.y = c.y) ON a.x = b.x",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableJoin a JOIN (tableParenthesized ( " +
                        "(tableJoin b JOIN c ON ${eq("b . y", "c . y")}) )) ON ${eq("a . x", "b . x")}))) <EOF>)",
                ),
                // A parenthesized query in FROM is a subquery, not a parenthesized table reference.
                case(
                    "SELECT 1 FROM (SELECT 1 FROM t) s",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableSubquery ( ${spec("1", "t")} ) s))) <EOF>)",
                ),
                // Clauses.
                case(
                    "WITH c AS (SELECT a FROM t) SELECT a FROM c",
                    "(root (query (withClause WITH (commonTableExpression c AS ( ${spec("a", "t")} ))) ${spec("a", "c")}) <EOF>)",
                ),
                case(
                    "SELECT TOP 10 a FROM t",
                    "(root (querySpecification SELECT (topClause TOP 10) a (fromClause FROM t)) <EOF>)",
                ),
                case(
                    "SELECT * EXCLUDE (a, b) FROM t",
                    "(root (querySpecification SELECT * (excludeClause EXCLUDE ( a , b )) (fromClause FROM t)) <EOF>)",
                ),
                case(
                    "SELECT a FROM t GROUP BY 1 HAVING COUNT(*) > 1",
                    "(root (querySpecification SELECT a (fromClause FROM t) (groupByClause GROUP BY 1) " +
                        "(havingClause HAVING (predicateComparison (atomCountStar COUNT ( * )) > 1))) <EOF>)",
                ),
                case(
                    "SELECT a FROM t ORDER BY a DESC NULLS LAST LIMIT 10 OFFSET 5",
                    "(root (query ${spec("a", "t")} (orderByClause ORDER BY (sortItem a DESC NULLS LAST)) " +
                        "(limitClause LIMIT 10 OFFSET 5)) <EOF>)",
                ),
                // A trailing semicolon is part of the statement.
                case("SELECT a FROM t;", "(root ${spec("a", "t")} ; <EOF>)"),
            )
    }
}
