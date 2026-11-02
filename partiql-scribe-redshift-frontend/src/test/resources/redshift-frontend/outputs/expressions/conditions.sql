--#[comparison]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE (((((("simple_t"['b'] = 1) AND ("simple_t"['b'] <> 2)) AND ("simple_t"['b'] <> 3)) AND ("simple_t"['b'] < 4)) AND ("simple_t"['b'] > 5)) AND ("simple_t"['b'] <= 6)) AND ("simple_t"['b'] >= 7);

--#[comparison-strings]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['c'] = 'x') AND ("simple_t"['c'] > 'a');

--#[comparison-expressions]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['b'] + 1) = ("simple_t"['b'] * 2);

--#[logical]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['a'] AND ("simple_t"['b'] > 0)) OR NOT ("simple_t"['a']);

--#[logical-precedence]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['a'] OR (("simple_t"['b'] > 0) AND ("simple_t"['c'] = 'x'))) OR (NOT ("simple_t"['a']) AND ("simple_t"['b'] < 0));

--#[logical-parenthesized]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['a'] OR ("simple_t"['b'] > 0)) AND (NOT(("simple_t"['c'] = 'x') OR ("simple_t"['b'] < 0)));

--#[logical-not-comparison]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] <> 1;

--#[is-truth]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['a'] IS TRUE OR "simple_t"['a'] IS FALSE) OR "simple_t"['a'] IS UNKNOWN;

--#[is-not-truth]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE (NOT ("simple_t"['a'] IS TRUE) AND NOT ("simple_t"['a'] IS FALSE)) AND NOT ("simple_t"['a'] IS UNKNOWN);

--#[like]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['c'] LIKE 'abc%' AND "simple_t"['c'] NOT LIKE '_x_';

--#[ilike]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "lower"("simple_t"['c']) LIKE "lower"('ABC%') AND "lower"("simple_t"['c']) NOT LIKE "lower"('x%');

--#[like-escape]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['c'] LIKE '100!%' ESCAPE '!';

--#[like-expression-pattern]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['c'] LIKE "simple_t"['c'] || '%';

--#[between]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] BETWEEN 1 AND 10 AND "simple_t"['c'] NOT BETWEEN 'a' AND 'm';

--#[between-expressions]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] BETWEEN "simple_t"['b'] - 1 AND "simple_t"['b'] + 1;

--#[is-null]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IS NULL AND "simple_t"['c'] IS NOT NULL;

--#[isnull-notnull]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IS NULL AND "simple_t"['c'] IS NOT NULL;

--#[in-list]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IN (1, 2, 3) AND "simple_t"['c'] NOT IN ('x', 'y');

--#[in-list-single]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IN (1);

--#[in-list-expressions]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IN ("simple_t"['b'] + 1, "simple_t"['b'] * 2);

--#[in-list-subquery-element]
[UNSUPPORTED_OPERATION{message="subquery as an element of an IN list"}];

--#[in-subquery]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] IN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AND "simple_t"['c'] NOT IN (SELECT "t"['c'] AS "c" FROM "default"."T" AS "t" WHERE "t"['a']);

--#[condition-in-select-list]
SELECT "simple_t"['b'] = 1 AS "_1", "simple_t"['c'] IS NULL AS "_2", "simple_t"['b'] IN (1, 2) AS "_3", "simple_t"['c'] LIKE 'x%' AS "_4", NOT ("simple_t"['a']) AS "_5" FROM "default"."SIMPLE_T" AS "simple_t";

--#[condition-in-case]
SELECT CASE WHEN "simple_t"['b'] BETWEEN 1 AND 5 AND "simple_t"['c'] IS NOT NULL THEN 1 ELSE 0 END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";
