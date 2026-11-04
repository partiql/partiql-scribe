--#[from-alias]
SELECT "x"['a'] AS "a" FROM "default"."SIMPLE_T" AS "x";

--#[from-alias-bare]
SELECT "x"['a'] AS "a" FROM "default"."SIMPLE_T" AS "x";

--#[from-qualified-table]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t";

--#[from-subquery]
SELECT "s"['a'] AS "a" FROM (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t") AS "s";

--#[from-subquery-bare-alias]
SELECT "s"['a'] AS "a" FROM (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] > 0) AS "s";

--#[from-subquery-no-alias]
SELECT "_0"['a'] AS "a" FROM (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t") AS "_0";

--#[from-comma-join]
SELECT "t"['a'] AS "a", "u"['c'] AS "c" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true WHERE "t"['b'] = "u"['b'];

--#[join-inner]
SELECT "t"['a'] AS "a", "u"['c'] AS "c" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-inner-keyword]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-left]
SELECT "t"['a'] AS "a", "u"['c'] AS "c" FROM "default"."SIMPLE_T" AS "t" LEFT JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-left-outer]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" LEFT JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-right]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" RIGHT JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-full-outer]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" FULL JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'];

--#[join-cross]
SELECT "t"['a'] AS "a", "u"['c'] AS "c" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true;

--#[join-chain]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON "t"['b'] = "u"['b'] LEFT JOIN "default"."T_DECIMALS" AS "d" ON "u"['b'] = "d"['da'];

--#[join-condition-complex]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON (("t"['b'] = "u"['b']) AND "u"['c'] IS NOT NULL) OR "t"['a'];

--#[join-subquery]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" INNER JOIN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['a']) AS "s" ON "t"['b'] = "s"['b'];

--#[join-parenthesized]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" INNER JOIN "default"."T_DECIMALS" AS "d" ON "u"['b'] = "d"['da'] ON "t"['b'] = "u"['b'];

--#[join-parenthesized-alias]
[UNSUPPORTED_OPERATION{message="alias of a parenthesized join"}];

--#[with]
WITH "c" AS (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") SELECT "c"['a'] AS "a" FROM "c" AS "c";
--#[with-columns]
[UNSUPPORTED_OPERATION{message="column list of a common table expression"}];

--#[with-several]
WITH "c1" AS (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t"), "c2" AS (SELECT "c1"['b'] AS "b" FROM "c1" AS "c1" WHERE "c1"['a']) SELECT "c2"['b'] AS "b" FROM "c2" AS "c2";
--#[with-shadows-table]
WITH "t" AS (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t") SELECT "t"['a'] AS "a" FROM "t" AS "t";
--#[with-in-subquery]
SELECT "s"['a'] AS "a" FROM (WITH "c" AS (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t") SELECT "c"['a'] AS "a" FROM "c" AS "c") AS "s";
--#[with-join]
WITH "c" AS (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" INNER JOIN "c" AS "c" ON "t"['b'] = "c"['b'];

--#[with-later-cte-is-a-table]
WITH "x" AS (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t"), "simple_t" AS (SELECT "t"['a'] AS "a" FROM "default"."T" AS "t") SELECT "x"['a'] AS "a" FROM "x" AS "x";

--#[with-shadows-table-of-same-name]
WITH "t" AS (SELECT "t"['a'] AS "a" FROM "default"."T" AS "t") SELECT "t"['a'] AS "a" FROM "t" AS "t";
