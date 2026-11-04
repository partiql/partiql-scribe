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

-- Redshift escapes with a backslash when there is no ESCAPE clause; PartiQL is told so.
--#[like-default-escape]
SELECT a FROM SIMPLE_T WHERE c LIKE '%start\\_%';

--#[ilike-default-escape]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'A\\%B%';

-- The escape character is lowered with the pattern.
--#[ilike-escape-letter]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'aX%b' ESCAPE 'X';

--#[ilike-escape-expression]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'a%' ESCAPE c;

--#[null-operands]
SELECT NULL LIKE 'a%', NULL IS TRUE, b IN (1, NULL) FROM SIMPLE_T;

-- The planner does not type BETWEEN on NULL.
--#[null-between]
SELECT a FROM SIMPLE_T WHERE NULL BETWEEN 1 AND 2;

-- The pattern is lowered here, so a letter that is the escape character cannot become one once lowered.
--#[ilike-escape-letter-in-pattern]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'x%X%' ESCAPE 'X';

--#[ilike-escaped-escape]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'XX%' ESCAPE 'X';

--#[ilike-no-escape-character]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'a\\%' ESCAPE '';

--#[ilike-pattern-ends-with-escape]
SELECT a FROM SIMPLE_T WHERE c ILIKE 'a%X' ESCAPE 'X';

--#[ilike-letter-escape-with-expression-pattern]
SELECT a FROM SIMPLE_T WHERE c ILIKE c ESCAPE 'X';

--#[ilike-expression-pattern]
SELECT a FROM SIMPLE_T WHERE c ILIKE c || '%' ESCAPE '!';

--#[like-escape-two-characters]
SELECT a FROM SIMPLE_T WHERE c LIKE 'a' ESCAPE 'ab';

-- A literal pattern is written as PartiQL takes it: an escape only before %, _, and the escape character.
--#[like-escaped-letter]
SELECT c FROM SIMPLE_T WHERE c LIKE 'a\\b';

--#[like-escaped-letter-with-escape-clause]
SELECT c FROM SIMPLE_T WHERE c LIKE 'aXb' ESCAPE 'X';

--#[like-no-escape-character]
SELECT c FROM SIMPLE_T WHERE c LIKE 'a%' ESCAPE '';

--#[like-no-escape-character-with-backslash]
SELECT c FROM SIMPLE_T WHERE c LIKE 'a\\%' ESCAPE '';

--#[like-expression-pattern-no-escape-character]
SELECT c FROM SIMPLE_T WHERE c LIKE c ESCAPE '';

--#[like-expression-escape]
SELECT c FROM SIMPLE_T WHERE c LIKE 'a%' ESCAPE c;
