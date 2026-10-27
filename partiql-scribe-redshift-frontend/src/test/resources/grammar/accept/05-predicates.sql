-- Conditions: comparisons, NULL tests, BETWEEN, IN, LIKE and the pattern operators, EXISTS, quantified comparisons.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_conditions.html

--#[comparison-operators]
SELECT a = b, a <> b, a != b, a < b, a <= b, a > b, a >= b FROM t;

--#[comparison-expressions]
SELECT * FROM t WHERE a + 1 = b * 2 AND LOWER(c) = 'x' AND d = (SELECT MAX(d) FROM u);

--#[logical]
SELECT * FROM t WHERE a AND b OR NOT c AND (d OR e);

--#[logical-nested-not]
SELECT * FROM t WHERE NOT NOT a AND NOT (b OR c);

--#[is-null]
SELECT * FROM t WHERE a IS NULL AND b IS NOT NULL;

--#[is-truth]
SELECT * FROM t WHERE a IS TRUE AND b IS NOT FALSE AND c IS UNKNOWN AND d IS NOT UNKNOWN;

--#[is-distinct-from]
SELECT * FROM t WHERE a IS DISTINCT FROM b AND c IS NOT DISTINCT FROM d;

--#[between]
SELECT * FROM t WHERE a BETWEEN 1 AND 10;

--#[not-between]
SELECT * FROM t WHERE a NOT BETWEEN b AND c;

--#[between-expressions]
SELECT * FROM t WHERE a BETWEEN b - 1 AND b + 1 AND d BETWEEN '2024-01-01' AND '2024-12-31';

--#[between-symmetric]
SELECT * FROM t WHERE a BETWEEN SYMMETRIC 10 AND 1 AND b NOT BETWEEN ASYMMETRIC 1 AND 10;

--#[in-list]
SELECT * FROM t WHERE a IN (1, 2, 3);

--#[in-list-single]
SELECT * FROM t WHERE a IN ('x');

--#[not-in-list]
SELECT * FROM t WHERE a NOT IN ('x', 'y', NULL);

--#[in-list-expressions]
SELECT * FROM t WHERE a IN (b, c + 1, LOWER(d), (SELECT MAX(e) FROM u));

--#[in-subquery]
SELECT * FROM t WHERE a IN (SELECT a FROM u WHERE x > 1);

--#[not-in-subquery]
SELECT * FROM t WHERE a NOT IN (SELECT a FROM u);

--#[in-subquery-with]
SELECT * FROM t WHERE a IN (WITH c AS (SELECT a FROM u) SELECT a FROM c);

--#[row-in-list]
SELECT * FROM t WHERE (a, b) IN ((1, 2), (3, 4));

--#[row-in-subquery]
SELECT * FROM t WHERE (a, b) NOT IN (SELECT a, b FROM u);

--#[like]
SELECT * FROM t WHERE a LIKE 'abc%' AND b NOT LIKE '%x_y%';

--#[like-escape]
SELECT * FROM t WHERE a LIKE '100!%' ESCAPE '!' AND b LIKE 'a#_b' ESCAPE '#';

--#[like-expression-pattern]
SELECT * FROM t WHERE a LIKE b || '%' AND c LIKE CONCAT('%', d);

--#[ilike]
SELECT * FROM t WHERE a ILIKE 'abc%' AND b NOT ILIKE '%X%';

--#[like-operators]
SELECT * FROM t WHERE a ~~ 'abc%' AND b !~~ '%x' AND c ~~* 'ABC%' AND d !~~* '%X';

--#[similar-to]
SELECT * FROM t WHERE a SIMILAR TO '(ab|cd)%' AND b NOT SIMILAR TO 'x_' ESCAPE '!';

--#[posix-operators]
SELECT * FROM t WHERE a ~ '^abc' AND b !~ 'x$' AND c ~* '^ABC' AND d !~* 'X$';

--#[exists]
SELECT * FROM t WHERE EXISTS (SELECT 1 FROM u WHERE u.id = t.id);

--#[not-exists]
SELECT * FROM t WHERE NOT EXISTS (SELECT * FROM u WHERE u.id = t.id);

--#[quantified-any]
SELECT * FROM t WHERE a = ANY (SELECT a FROM u);

--#[quantified-some]
SELECT * FROM t WHERE a = SOME (SELECT a FROM u);

--#[quantified-all]
SELECT * FROM t WHERE a <> ALL (SELECT a FROM u) AND b > ALL (SELECT b FROM u);

--#[predicate-in-select-list]
SELECT a > 1, b IS NULL, c IN (1, 2), d LIKE 'x%', e BETWEEN 1 AND 2 FROM t;

--#[predicate-in-case]
SELECT CASE WHEN a IS NULL OR b NOT IN (1, 2) THEN 1 ELSE 0 END FROM t;

--#[predicate-parenthesized]
SELECT * FROM t WHERE ((a = 1) AND (b = 2)) OR ((c = 3));
