--#[where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] > 0;

--#[where-complex]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE (("simple_t"['b'] > 0) AND "simple_t"['c'] IS NOT NULL) OR ("simple_t"['a'] AND "simple_t"['c'] LIKE 'x%');

--#[group-by]
SELECT "simple_t"['c'] AS "c", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['c'];

--#[group-by-several]
SELECT "simple_t"['a'] AS "a", "simple_t"['c'] AS "c", sum("simple_t"['b']) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['a'], "simple_t"['c'];

--#[group-by-expression]
SELECT "simple_t"['b'] + 1 AS "_1", count(1) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['b'] + 1;

--#[group-by-ordinal]
SELECT "simple_t"['c'] AS "c", "simple_t"['a'] AS "a", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['c'], "simple_t"['a'];

--#[group-by-ordinal-of-expression]
SELECT "simple_t"['b'] * 2 AS "twice", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['b'] * 2;

--#[group-by-ordinal-out-of-range]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: ordinal 3 is out of range of the select list"}];

--#[group-by-ordinal-of-star]
[UNSUPPORTED_OPERATION{message="`*` with other select items"}];

--#[having]
SELECT "simple_t"['c'] AS "c", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['c'] HAVING count(1) > CAST(1 AS BIGINT);

--#[having-complex]
SELECT "simple_t"['c'] AS "c", sum("simple_t"['b']) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['c'] HAVING (sum("simple_t"['b']) > CAST(100 AS BIGINT)) AND max("simple_t"['b']) IS NOT NULL;

--#[union]
(SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

--#[union-all]
(SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION ALL (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

--#[union-chain]
((SELECT CAST("simple_t"['b'] AS DECIMAL(38,0)) AS "_" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT CAST("t"['b'] AS DECIMAL(38,0)) AS "_" FROM "default"."T" AS "t")) UNION ALL (SELECT "t_decimals"['da'] AS "_" FROM "default"."T_DECIMALS" AS "t_decimals");

--#[union-parenthesized]
(SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

--#[union-order-limit]
(SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") ORDER BY "_1"['b'] ASC NULLS LAST LIMIT 10;

--#[union-in-subquery]
SELECT "s"['b'] AS "b" FROM ((SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION ALL (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t")) AS "s";

--#[order-by]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST;

--#[order-by-direction]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST, "simple_t"['b'] DESC NULLS FIRST;

--#[order-by-nulls]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS FIRST, "simple_t"['b'] DESC NULLS LAST;

--#[order-by-expression]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] + 1 ASC NULLS LAST, CASE WHEN "simple_t"['a'] THEN 0 ELSE 1 END ASC NULLS LAST;

--#[order-by-ordinal]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] DESC NULLS FIRST, "simple_t"['a'] ASC NULLS LAST;

--#[order-by-alias]
SELECT "simple_t"['b'] + 1 AS "x" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] + 1 ASC NULLS LAST;

--#[order-by-alias-shadows-column]
SELECT "simple_t"['b'] + 1 AS "c" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] + 1 ASC NULLS LAST;

--#[limit]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" LIMIT 10;

--#[limit-all]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t";

--#[limit-offset]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST LIMIT 10 OFFSET 20;

--#[offset-limit]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST LIMIT 10 OFFSET 20;

--#[offset-only]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST OFFSET 5;

--#[distinct-limit]
SELECT "$distinct"['c'] AS "c" FROM (SELECT DISTINCT "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['c'] ASC NULLS LAST LIMIT 5;

--#[distinct-offset]
SELECT "$distinct"['a'] AS "a", "$distinct"['b'] AS "b" FROM (SELECT DISTINCT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" OFFSET 2;

--#[distinct-top]
SELECT "$distinct"['c'] AS "c" FROM (SELECT DISTINCT "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" LIMIT 3;

--#[distinct-limit-order-by-alias]
SELECT "$distinct"['x'] AS "x" FROM (SELECT DISTINCT "simple_t"['b'] + 1 AS "x" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['x'] DESC NULLS FIRST LIMIT 5;

--#[distinct-limit-order-by-ordinal]
SELECT "$distinct"['a'] AS "a", "$distinct"['b'] AS "b" FROM (SELECT DISTINCT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['b'] ASC NULLS LAST, "$distinct"['a'] ASC NULLS LAST LIMIT 5;

--#[distinct-limit-order-by-star]
SELECT "$distinct"['a'] AS "a", "$distinct"['b'] AS "b", "$distinct"['c'] AS "c" FROM (SELECT DISTINCT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['c'] ASC NULLS LAST LIMIT 5;

--#[distinct-limit-order-by-expression]
[UNSUPPORTED_OPERATION{message="ORDER BY key that is not a select item, with DISTINCT and LIMIT or OFFSET"}];

--#[parenthesized-query-order-by]
[UNSUPPORTED_OPERATION{message="ORDER BY, LIMIT, or OFFSET on a parenthesized query that has its own"}];

-- --#[lateral-alias-where] TODO(redshift)
SELECT "simple_t"['b'] + 1 AS "next" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['b'] + 1) > 10;

-- --#[lateral-alias-select] TODO(redshift)
SELECT "simple_t"['b'] + 1 AS "next", ("simple_t"['b'] + 1) * 2 AS "after" FROM "default"."SIMPLE_T" AS "simple_t";

-- --#[lateral-alias-group-by] TODO(redshift)
SELECT "simple_t"['b'] * 2 AS "twice", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['b'] * 2;

--#[lateral-alias-column-wins]
SELECT "simple_t"['b'] + 1 AS "c" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['c'] = 'x';

--#[all-clauses]
WITH "c" AS (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") SELECT "$distinct"['c'] AS "c", "$distinct"['n'] AS "n" FROM (SELECT DISTINCT "c"['c'] AS "c", count(1) AS "n" FROM "c" AS "c" INNER JOIN "default"."T" AS "u" ON "c"['b'] = "u"['b'] WHERE "c"['b'] > 0 GROUP BY "c"['c'] HAVING count(1) > CAST(1 AS BIGINT)) AS "$distinct" ORDER BY "$distinct"['n'] DESC NULLS FIRST LIMIT 10 OFFSET 5;
--#[subquery-scalar]
SELECT "simple_t"['a'] AS "a", (SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t") AS "mb" FROM "default"."SIMPLE_T" AS "simple_t";

--#[subquery-scalar-in-where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] > (SELECT avg("t"['b']) AS "_1" FROM "default"."T" AS "t");

--#[subquery-in-having]
SELECT "simple_t"['c'] AS "c", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['c'] HAVING count(1) > (SELECT count(1) AS "_1" FROM "default"."T" AS "t");

--#[subquery-correlated-select]
SELECT "t"['a'] AS "a", (SELECT count(1) AS "_1" FROM "default"."T" AS "u" WHERE "u"['b'] = "t"['b']) AS "n" FROM "default"."SIMPLE_T" AS "t";

--#[subquery-correlated-where]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" WHERE "t"['b'] > (SELECT avg("u"['b']) AS "_1" FROM "default"."T" AS "u" WHERE "u"['c'] = "t"['c']);

--#[subquery-correlated-in]
SELECT "t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t" WHERE "t"['b'] IN (SELECT "u"['b'] AS "b" FROM "default"."T" AS "u" WHERE "u"['c'] = "t"['c']);

--#[order-by-alias-in-expression]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[order-by-alias-in-expression-column-wins]
SELECT "simple_t"['b'] + 1 AS "c" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['c'] || 'x' ASC NULLS LAST;

--#[distinct-limit-order-by-qualified-column]
SELECT "$distinct"['b'] AS "b", "$distinct"['yb'] AS "yb" FROM (SELECT DISTINCT "x"['b'] AS "b", "y"['b'] AS "yb" FROM "default"."T" AS "x" INNER JOIN "default"."T" AS "y" ON "x"['a'] = "y"['a']) AS "$distinct" ORDER BY "$distinct"['yb'] ASC NULLS LAST LIMIT 5;

--#[distinct-limit-order-by-item-expression]
SELECT "$distinct"['x'] AS "x" FROM (SELECT DISTINCT "simple_t"['b'] + 1 AS "x" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['x'] ASC NULLS LAST LIMIT 5;

--#[distinct-limit-order-by-ambiguous-name]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: ORDER BY `b` is ambiguous"}];

--#[order-by-ambiguous-alias]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: ORDER BY `x` is ambiguous"}];

--#[order-by-ordinal-of-star]
[UNSUPPORTED_OPERATION{message="Unsupported Redshift construct: ordinal 1 refers to `*`"}];

--#[order-by-ordinal-not-an-int]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: ordinal 99999999999 is out of range of the select list"}];

--#[limit-too-large]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: LIMIT value 99999999999999999999 is out of range"}];
