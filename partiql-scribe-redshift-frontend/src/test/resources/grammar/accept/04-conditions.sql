-- Conditions: comparison, logical, truth tests, LIKE, BETWEEN, NULL tests, and IN.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_conditions.html

--#[comparison]
SELECT a FROM t WHERE a = 1 AND b <> 2 AND c != 3 AND d < 4 AND e > 5 AND f <= 6 AND g >= 7;

--#[comparison-strings-dates]
SELECT a FROM t WHERE s = 'x' AND d >= DATE '2024-01-01' AND ts < TIMESTAMP '2024-01-01 00:00:00';

--#[comparison-expressions]
SELECT a FROM t WHERE a + 1 = b * 2 AND (a > b) = (c > d);

--#[comparison-subquery]
SELECT a FROM t WHERE a = (SELECT MAX(a) FROM t);

--#[logical]
SELECT a FROM t WHERE a AND b OR NOT c;

--#[logical-precedence]
SELECT a FROM t WHERE a OR b AND c OR NOT d AND e;

--#[logical-parenthesized]
SELECT a FROM t WHERE (a OR b) AND NOT (c OR d);

--#[logical-not-comparison]
SELECT a FROM t WHERE NOT a = 1 AND NOT (b IN (1, 2));

--#[is-truth]
SELECT a FROM t WHERE a IS TRUE OR b IS FALSE OR c IS UNKNOWN;

--#[is-not-truth]
SELECT a FROM t WHERE a IS NOT TRUE AND b IS NOT FALSE AND c IS NOT UNKNOWN;

--#[like]
SELECT a FROM t WHERE a LIKE 'abc%' AND b NOT LIKE '_x_';

--#[ilike]
SELECT a FROM t WHERE a ILIKE 'ABC%' AND b NOT ILIKE 'x%';

--#[like-escape]
SELECT a FROM t WHERE a LIKE '100\%' ESCAPE '\\' AND b LIKE 'a!_b' ESCAPE '!';

--#[like-expression-pattern]
SELECT a FROM t WHERE a LIKE b || '%' AND c LIKE UPPER(d);

--#[between]
SELECT a FROM t WHERE a BETWEEN 1 AND 10 AND b NOT BETWEEN 'a' AND 'm';

--#[between-expressions]
SELECT a FROM t WHERE a BETWEEN b - 1 AND b + 1 AND d BETWEEN DATE '2024-01-01' AND CURRENT_DATE;

--#[is-null]
SELECT a FROM t WHERE a IS NULL AND b IS NOT NULL;

--#[isnull-notnull]
SELECT a FROM t WHERE a ISNULL AND b NOTNULL;

--#[is-null-expression]
SELECT a FROM t WHERE a + b IS NULL AND NULLIF(a, 0) IS NOT NULL;

--#[in-list]
SELECT a FROM t WHERE a IN (1, 2, 3) AND b NOT IN ('x', 'y');

--#[in-list-single]
SELECT a FROM t WHERE a IN (1);

--#[in-list-expressions]
SELECT a FROM t WHERE a IN (b, c + 1, (SELECT MAX(d) FROM u));

--#[in-subquery]
SELECT a FROM t WHERE a IN (SELECT b FROM u) AND c NOT IN (SELECT d FROM v WHERE e = 1);

--#[in-subquery-correlated]
SELECT a FROM t WHERE a IN (SELECT b FROM u WHERE u.t_id = t.id);

--#[condition-in-select-list]
SELECT a = 1, b IS NULL, c IN (1, 2), d LIKE 'x%', NOT e FROM t;

--#[condition-in-case]
SELECT CASE WHEN a BETWEEN 1 AND 5 AND b IS NOT NULL THEN 1 ELSE 0 END FROM t;
