package org.partiql.scribe.frontend.redshift.grammar

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the shape of the parse tree where the grammar has to choose: operator precedence and associativity, set
 * operation precedence, and the tokens that have more than one reading (keywords that are also identifiers, `DATE`
 * as a literal prefix and as a name, `||/` before a comment, `1e3`).
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
        ) = "(predicateComparison (pathField $a) = (pathField $b))"

        /**
         * The expression `e` in `SELECT e`, rendered.
         */
        private fun expr(
            e: String,
            tree: String,
        ) = case("SELECT $e", "(root (querySpecification SELECT $tree) <EOF>)")

        @JvmStatic
        fun cases() =
            listOf(
                // Arithmetic: `*` before `+`, left-associative, `^` before `*`, unary sign before `^`.
                expr("a + b * c", "(valueAdditive a + (valueMultiplicative b * c))"),
                expr("a - b - c", "(valueAdditive (valueAdditive a - b) - c)"),
                expr("a * b ^ c", "(valueMultiplicative a * (valueExponent b ^ c))"),
                expr("-a ^ b", "(valueExponent (valueSigned - a) ^ b)"),
                expr("a / b % c", "(valueMultiplicative (valueMultiplicative a / b) % c)"),
                // `::` binds tightest: `-1::INT` is the negation of a cast, `a.b::INT` casts the path.
                expr("-1::INT", "(valueSigned - (valueCastShorthand 1 :: INT))"),
                expr("t.a::DECIMAL(10, 2)", "(valueCastShorthand (pathField t . a) :: (typeNamed DECIMAL (typeParameters ( 10 , 2 ))))"),
                expr("a::INT::VARCHAR", "(valueCastShorthand (valueCastShorthand a :: INT) :: VARCHAR)"),
                // `||`, `&`, `|`, `<<`, `>>` and the prefix operators bind below `+` and `-`, as in PostgreSQL.
                expr("a || b + c", "(valueOperator a || (valueAdditive b + c))"),
                expr("a || b || c", "(valueOperator (valueOperator a || b) || c)"),
                expr("a & b + c", "(valueOperator a & (valueAdditive b + c))"),
                expr("|/ a + b", "(valuePrefixOperator |/ (valueAdditive a + b))"),
                expr("a << 1 | b", "(valueOperator (valueOperator a << 1) | b)"),
                // Comparison below arithmetic; NOT, AND, OR in that order; NOT binds tighter than AND.
                expr("a + b = c * d", "(predicateComparison (valueAdditive a + b) = (valueMultiplicative c * d))"),
                expr("a OR b AND c", "(or a OR (and b AND c))"),
                expr("NOT a AND b", "(and (not NOT a) AND b)"),
                expr("a AND b OR c AND d", "(or (and a AND b) OR (and c AND d))"),
                expr("NOT a = b", "(not NOT (predicateComparison a = b))"),
                // BETWEEN takes its own AND; the enclosing AND is a conjunction.
                expr(
                    "a BETWEEN 1 AND 10 AND b",
                    "(and (predicateBetween a BETWEEN 1 AND 10) AND b)",
                ),
                expr("a BETWEEN b + 1 AND c * 2", "(predicateBetween a BETWEEN (valueAdditive b + 1) AND (valueMultiplicative c * 2))"),
                // Predicates over operator expressions.
                expr("a || b LIKE 'x%'", "(predicateLike (valueOperator a || b) LIKE 'x%')"),
                expr("a IS NOT NULL", "(predicateNull a IS NOT NULL)"),
                expr("a NOT IN (1, 2)", "(predicateInList a NOT IN ( 1 , 2 ))"),
                expr(
                    "(a, b) IN ((1, 2), (3, 4))",
                    "(predicateRowInList (rowValue ( a , b )) IN ( (rowValue ( 1 , 2 )) , (rowValue ( 3 , 4 )) ))",
                ),
                expr("a = ANY (SELECT b FROM t)", "(predicateQuantified a = ANY ( (querySpecification SELECT b (fromClause FROM t)) ))"),
                // AT TIME ZONE binds above arithmetic.
                expr(
                    "ts AT TIME ZONE 'UTC' + i",
                    "(valueAdditive (valueAtTimeZone ts AT TIME ZONE 'UTC') + i)",
                ),
                // Paths: field and element access bind tightest, left to right.
                expr("t.a[0].b", "(pathField (pathElement (pathField t . a) [ 0 ]) . b)"),
                expr("t.a[i + 1]", "(pathElement (pathField t . a) [ (valueAdditive i + 1) ])"),
                // Literal prefixes versus names: `DATE '...'` is a literal, `date` alone is a column, `date(x)` a function.
                expr("DATE '2024-01-01'", "(literalDate DATE '2024-01-01')"),
                expr("date", "date"),
                expr("date(x)", "(atomFunctionCall date ( x ))"),
                expr(
                    "TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+00'",
                    "(literalTimestampTz TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+00')",
                ),
                expr("INTERVAL '1' DAY TO SECOND(3)", "(literalInterval INTERVAL '1' (intervalQualifier DAY TO SECOND ( 3 )))"),
                // Numeric literals: an exponent makes one token; a bare `e3` would be an alias.
                expr("1e3", "1e3"),
                expr("1 e3", "(selectItemExpr 1 e3)"),
                expr("1.5e-3", "1.5e-3"),
                expr(".5", ".5"),
                expr("5.", "5."),
                // `||/` is the cube root unless a comment follows the slash.
                expr("||/ 8", "(valuePrefixOperator ||/ 8)"),
                expr("a ||/* comment */ b", "(valueOperator a || b)"),
                // Non-reserved keywords are identifiers; LEFT and RIGHT are function names.
                expr("year", "year"),
                expr("LEFT(s, 2)", "(atomFunctionCall LEFT ( s , 2 ))"),
                expr("COUNT(*)", "(atomCountStar COUNT ( * ))"),
                expr("COUNT(DISTINCT a)", "(atomFunctionCall COUNT ( DISTINCT a ))"),
                // SUBSTRING and TRIM have their own argument forms.
                expr("SUBSTRING(s FROM 2 FOR 3)", "(atomSubstringFrom SUBSTRING ( s FROM 2 FOR 3 ))"),
                expr("SUBSTRING(s, 2, 3)", "(atomFunctionCall SUBSTRING ( s , 2 , 3 ))"),
                expr("TRIM(BOTH 'x' FROM s)", "(atomTrim TRIM ( BOTH 'x' FROM s ))"),
                expr("TRIM(s)", "(atomTrim TRIM ( s ))"),
                // Set operations: INTERSECT binds tighter than UNION and EXCEPT; the same level is left-associative.
                case(
                    "SELECT a FROM t UNION SELECT b FROM u INTERSECT SELECT c FROM v",
                    "(root (queryTermSetOp " + spec("a", "t") + " UNION " +
                        "(queryTermSetOp " + spec("b", "u") + " INTERSECT " + spec("c", "v") + ")) <EOF>)",
                ),
                case(
                    "SELECT a FROM t UNION ALL SELECT b FROM u EXCEPT SELECT c FROM v",
                    "(root (queryTermSetOp (queryTermSetOp " + spec("a", "t") + " UNION ALL " + spec("b", "u") + ") " +
                        "EXCEPT " + spec("c", "v") + ") <EOF>)",
                ),
                // ORDER BY and LIMIT after a set operation apply to its result.
                case(
                    "SELECT a FROM t UNION SELECT a FROM u ORDER BY 1 LIMIT 5",
                    "(root (query (queryTermSetOp " + spec("a", "t") + " UNION " + spec("a", "u") + ") " +
                        "(orderByClause ORDER BY 1) (limitClause LIMIT 5)) <EOF>)",
                ),
                // Joins are left-associative; a parenthesized join groups.
                case(
                    "SELECT 1 FROM a JOIN b ON a.x = b.x LEFT JOIN c ON b.y = c.y",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableJoin (tableJoin a JOIN b " +
                        "(joinOn ON " + eq("a . x", "b . x") + ")) " +
                        "LEFT JOIN c (joinOn ON " + eq("b . y", "c . y") + ")))) <EOF>)",
                ),
                case(
                    "SELECT 1 FROM a JOIN (b JOIN c ON b.y = c.y) ON a.x = b.x",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableJoin a JOIN (tableParenthesized ( " +
                        "(tableJoin b JOIN c (joinOn ON " + eq("b . y", "c . y") + ")) )) " +
                        "(joinOn ON " + eq("a . x", "b . x") + ")))) <EOF>)",
                ),
                // A parenthesized query in FROM is a subquery, not a parenthesized table reference.
                case(
                    "SELECT 1 FROM (SELECT 1) s",
                    "(root (querySpecification SELECT 1 (fromClause FROM (tableSubquery ( (querySpecification SELECT 1) ) s))) <EOF>)",
                ),
                // A SUPER navigation in FROM is a table reference; the alias applies to it.
                case(
                    "SELECT i FROM t, t.items AS i AT idx",
                    "(root (querySpecification SELECT i (fromClause FROM t , " +
                        "(tableName (pathField t . items) (tableAlias AS i) AT idx))) <EOF>)",
                ),
                // Trailing semicolons and a second statement are recognized.
                case("SELECT 1;;", "(root (querySpecification SELECT 1) ; ; <EOF>)"),
                case("SELECT 1; SELECT 2", "(root (querySpecification SELECT 1) ; (querySpecification SELECT 2) <EOF>)"),
            )
    }
}
