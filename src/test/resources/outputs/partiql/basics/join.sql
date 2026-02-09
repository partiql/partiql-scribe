-- Comma join (implicit cross join)
--#[join-00]
SELECT "T1"['a'] AS "a", "T2"['b'] AS "b", "T1"['c'] AS "c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Explicit cross join
--#[join-01]
SELECT "T1"['b'] AS "b", "T2"['c'] AS "c", "T1"['z'] AS "z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Basic join with true condition
--#[join-02]
SELECT "T1"['c'] AS "c", "T2"['a'] AS "a", "T1"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Comma join with three tables
--#[join-03]
SELECT "T1"['a'] AS "a", "T2"['b'] AS "b", "T3"['c'] AS "c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Cross join with three tables
--#[join-04]
SELECT "T1"['b'] AS "b", "T2"['c'] AS "c", "T3"['z'] AS "z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Join with three tables using true condition
--#[join-05]
SELECT "T1"['a'] AS "a", "T2"['v'] AS "v", "T3"['b'] AS "b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Left cross join
--#[join-06]
SELECT "T1"['c'] AS "c", "T2"['z'] AS "z", "T1"['a'] AS "a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON true;

-- Left join with true condition
--#[join-07]
SELECT "T1"['b'] AS "b", "T2"['c'] AS "c", "T1"['v'] AS "v" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON true;

-- Full join with true condition
--#[join-08]
SELECT "T1"['a'] AS "a", "T2"['b'] AS "b", "T1"['z'] AS "z" FROM "default"."T" AS "T1" FULL JOIN "default"."T" AS "T2" ON true;

-- Inner join with equality condition
--#[join-09]
SELECT "T1"['a'] AS "a", "T2"['c'] AS "c", "T1"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Inner join with specific columns
--#[join-10]
SELECT "T1"['a'] AS "a", "T2"['b'] AS "b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Right join
--#[join-11]
SELECT "T1"['c'] AS "c", "T2"['a'] AS "a", "T2"['z'] AS "z" FROM "default"."T" AS "T1" RIGHT JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Right outer join (explicit)
--#[join-12]
SELECT "T1"['b'] AS "b", "T2"['v'] AS "v", "T1"['a'] AS "a" FROM "default"."T" AS "T1" RIGHT JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Full outer join (explicit)
--#[join-13]
SELECT "T1"['c'] AS "c", "T2"['b'] AS "b", "T1"['z'] AS "z" FROM "default"."T" AS "T1" FULL JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Join with multiple AND conditions
--#[join-14]
SELECT "T1"['a'] AS "a", "T2"['v'] AS "v", "T1"['b'] AS "b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"['b'] = "T2"['b']) AND ("T1"['c'] = "T2"['c']);

-- Join with OR condition
--#[join-15]
SELECT "T1"['c'] AS "c", "T2"['z'] AS "z", "T1"['a'] AS "a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"['b'] = "T2"['b']) OR ("T1"['c'] = "T2"['c']);

-- Three table inner joins
--#[join-16]
SELECT "T1"['a'] AS "a", "T2"['c'] AS "c", "T3"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] INNER JOIN "default"."T" AS "T3" ON "T2"['b'] = "T3"['b'];

-- Three table left joins
--#[join-17]
SELECT "T1"['b'] AS "b", "T2"['z'] AS "z", "T3"['a'] AS "a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] LEFT JOIN "default"."T" AS "T3" ON "T2"['b'] = "T3"['b'];

-- Mixed join types (inner and left)
--#[join-18]
SELECT "T1"['c'] AS "c", "T2"['a'] AS "a", "T3"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] LEFT JOIN "default"."T" AS "T3" ON "T2"['b'] = "T3"['b'];

-- Self join
--#[join-19]
SELECT "T1"['a'] AS "a", "T2"['c'] AS "c", "T1"['z'] AS "z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Join with WHERE clause
--#[join-20]
SELECT "T1"['b'] AS "b", "T2"['v'] AS "v", "T1"['a'] AS "a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] WHERE "T1"['c'] = 'active';

-- Left join with NULL check (anti-join pattern)
--#[join-21]
SELECT "T1"['c'] AS "c", "T1"['z'] AS "z", "T1"['a'] AS "a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] WHERE "T2"['b'] IS NULL;

-- Join with aggregation and GROUP BY
--#[join-22]
SELECT "c" AS "c", count(1) AS "_1" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] GROUP BY "T1"['c'];

-- Left join with aggregation
--#[join-23]
SELECT "b" AS "b", sum("T2"['b']) AS "_1" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] GROUP BY "T1"['b'];

-- Join with ORDER BY
--#[join-24]
SELECT "T1"['a'] AS "a", "T2"['b'] AS "b", "T1"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] ORDER BY "T1"['c'] ASC NULLS LAST;

-- Join with multiple ORDER BY columns
--#[join-25]
SELECT "T1"['c'] AS "c", "T2"['z'] AS "z", "T1"['a'] AS "a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] ORDER BY "T1"['c'] ASC NULLS LAST, "T2"['timestamp_1'] DESC NULLS FIRST;

-- Join with subquery
--#[join-26]
SELECT "T1"['a'] AS "a", "T2"['c'] AS "c", "T1"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN (SELECT "T"['b'] AS "b", "T"['c'] AS "c" FROM "default"."T" AS "T" WHERE "T"['a'] = true) AS "T2" ON "T1"['b'] = "T2"['b'];

-- Join with complex condition (comparison)
--#[join-27]
SELECT "T1"['b'] AS "b", "T2"['c'] AS "c", "T1"['z'] AS "z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"['b'] = "T2"['b']) AND ("T1"['timestamp_1'] > "T2"['timestamp_1']);

-- Join with range condition
--#[join-28]
SELECT "T1"['a'] AS "a", "T2"['v'] AS "v", "T1"['c'] AS "c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"['timestamp_1'] <= "T2"['timestamp_1']) AND ("T1"['timestamp_2'] >= "T2"['timestamp_1']);


-- Four table joins
--#[join-30]
SELECT "T1"['a'] AS "a", "T2"['c'] AS "c", "T3"['v'] AS "v", "T4"['z'] AS "z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] INNER JOIN "default"."T" AS "T3" ON "T2"['b'] = "T3"['b'] INNER JOIN "default"."T" AS "T4" ON "T3"['b'] = "T4"['b'];

-- Join with DISTINCT
--#[join-31]
SELECT DISTINCT "T1"['b'] AS "b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'];

-- Join with LIMIT
--#[join-32]
SELECT "T1"['c'] AS "c", "T2"['a'] AS "a", "T1"['v'] AS "v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] LIMIT 10;

-- Join with HAVING clause
--#[join-33]
SELECT "c" AS "c", count(1) AS "_1" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"['b'] = "T2"['b'] GROUP BY "T1"['c'] HAVING count(1) > CAST(5 AS BIGINT);
