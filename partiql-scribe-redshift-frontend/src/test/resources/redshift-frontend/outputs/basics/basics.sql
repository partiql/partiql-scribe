--#[select-00]
SELECT "t"['a'] AS "a", "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[select-01]
SELECT "t"['a'] AS "x", "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[select-02]
SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['c'] AS "c", "t"['d'] AS "d", "t"['x'] AS "x", "t"['array'] AS "array", "t"['z'] AS "z", "t"['v'] AS "v", "t"['timestamp_1'] AS "timestamp_1", "t"['timestamp_2'] AS "timestamp_2" FROM "default"."T" AS "t";

--#[select-03]
SELECT DISTINCT "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[select-04]
SELECT "t"['a'] AS "a", "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[select-05]
SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['c'] AS "c", "t"['d'] AS "d", "t"['x'] AS "x", "t"['array'] AS "array", "t"['z'] AS "z", "t"['v'] AS "v", "t"['timestamp_1'] AS "timestamp_1", "t"['timestamp_2'] AS "timestamp_2" FROM "default"."T" AS "t";

--#[where-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE ("t"['b'] > 1) AND ("t"['c'] <> 'x');

--#[where-01]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE ("t"['b'] <> 1) OR NOT ("t"['a']);

--#[where-02]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] BETWEEN 1 AND 10;

--#[where-03]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] NOT IN (1, 2, 3);

--#[where-04]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a%' AND "t"['z'] IS NOT NULL;

--#[where-05]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] = 'it''s';

--#[order-00]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] ASC NULLS LAST;

--#[order-01]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] DESC NULLS FIRST;

--#[order-02]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] DESC NULLS LAST LIMIT 10 OFFSET 5;

--#[top-00]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" LIMIT 5;

--#[group-00]
SELECT "t"['c'] AS "c", count(1) AS "n", sum("t"['b']) AS "total" FROM "default"."T" AS "t" GROUP BY "t"['c'] HAVING count(1) > CAST(1 AS BIGINT);

--#[group-01]
SELECT count(DISTINCT "t"['c']) AS "n" FROM "default"."T" AS "t";

--#[join-00]
SELECT "t1"['a'] AS "a", "t2"['b'] AS "b" FROM "default"."T" AS "t1" INNER JOIN "default"."T" AS "t2" ON "t1"['b'] = "t2"['b'];

--#[join-01]
SELECT "t1"['a'] AS "a", "t2"['b'] AS "b" FROM "default"."T" AS "t1" LEFT JOIN "default"."T" AS "t2" ON "t1"['b'] = "t2"['b'];

--#[subquery-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

--#[subquery-01]
SELECT "s"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['a']) AS "s";

--#[case-00]
SELECT CASE WHEN "t"['b'] > 0 THEN 'pos' WHEN "t"['b'] < 0 THEN 'neg' ELSE 'zero' END AS "sign" FROM "default"."T" AS "t";

--#[case-01]
SELECT CASE WHEN "t"['b'] = 1 THEN 'one' ELSE NULL END AS "n" FROM "default"."T" AS "t";

--#[comment-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t";
