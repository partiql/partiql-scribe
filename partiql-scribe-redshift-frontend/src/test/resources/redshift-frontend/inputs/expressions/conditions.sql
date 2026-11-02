-- Conditions: comparison, logical, truth tests, LIKE, BETWEEN, NULL tests, and IN.

--#[comparison]
SELECT a FROM SIMPLE_T WHERE b = 1 AND b <> 2 AND b != 3 AND b < 4 AND b > 5 AND b <= 6 AND b >= 7;

--#[comparison-strings]
SELECT a FROM SIMPLE_T WHERE c = 'x' AND c > 'a';

--#[comparison-expressions]
SELECT a FROM SIMPLE_T WHERE b + 1 = b * 2;

--#[logical]
SELECT a FROM SIMPLE_T WHERE a AND b > 0 OR NOT a;

--#[logical-precedence]
SELECT a FROM SIMPLE_T WHERE a OR b > 0 AND c = 'x' OR NOT a AND b < 0;

--#[logical-parenthesized]
SELECT a FROM SIMPLE_T WHERE (a OR b > 0) AND NOT (c = 'x' OR b < 0);

--#[logical-not-comparison]
SELECT a FROM SIMPLE_T WHERE NOT b = 1;

--#[is-truth]
SELECT a FROM SIMPLE_T WHERE a IS TRUE OR a IS FALSE OR a IS UNKNOWN;

--#[is-not-truth]
SELECT a FROM SIMPLE_T WHERE a IS NOT TRUE AND a IS NOT FALSE AND a IS NOT UNKNOWN;

--#[like]
SELECT a FROM SIMPLE_T WHERE c LIKE 'abc%' AND c NOT LIKE '_x_';

--#[ilike]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'ABC%' AND c NOT ILIKE 'x%';

--#[like-escape]
SELECT a FROM SIMPLE_T WHERE c LIKE '100!%' ESCAPE '!';

--#[like-expression-pattern]
SELECT a FROM SIMPLE_T WHERE c LIKE c || '%';

--#[between]
SELECT a FROM SIMPLE_T WHERE b BETWEEN 1 AND 10 AND c NOT BETWEEN 'a' AND 'm';

--#[between-expressions]
SELECT a FROM SIMPLE_T WHERE b BETWEEN b - 1 AND b + 1;

--#[is-null]
SELECT a FROM SIMPLE_T WHERE b IS NULL AND c IS NOT NULL;

--#[isnull-notnull]
SELECT a FROM SIMPLE_T WHERE b ISNULL AND c NOTNULL;

--#[in-list]
SELECT a FROM SIMPLE_T WHERE b IN (1, 2, 3) AND c NOT IN ('x', 'y');

--#[in-list-single]
SELECT a FROM SIMPLE_T WHERE b IN (1);

--#[in-list-expressions]
SELECT a FROM SIMPLE_T WHERE b IN (b + 1, b * 2);

--#[in-list-subquery-element]
SELECT a FROM SIMPLE_T WHERE b IN (1, (SELECT MAX(b) FROM T));

--#[in-subquery]
SELECT a FROM SIMPLE_T WHERE b IN (SELECT b FROM T) AND c NOT IN (SELECT c FROM T WHERE a);

--#[condition-in-select-list]
SELECT b = 1, c IS NULL, b IN (1, 2), c LIKE 'x%', NOT a FROM SIMPLE_T;

--#[condition-in-case]
SELECT CASE WHEN b BETWEEN 1 AND 5 AND c IS NOT NULL THEN 1 ELSE 0 END FROM SIMPLE_T;
