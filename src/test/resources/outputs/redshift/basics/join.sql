-- Comma join (implicit cross join)
--#[join-00]
SELECT "T1"."a", "T2"."b", "T1"."c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Explicit cross join
--#[join-01]
SELECT "T1"."b", "T2"."c", "T1"."z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Basic join with true condition
--#[join-02]
SELECT "T1"."c", "T2"."a", "T1"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true;

-- Comma join with three tables
--#[join-03]
SELECT "T1"."a", "T2"."b", "T3"."c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Cross join with three tables
--#[join-04]
SELECT "T1"."b", "T2"."c", "T3"."z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Join with three tables using true condition
--#[join-05]
SELECT "T1"."a", "T2"."v", "T3"."b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON true INNER JOIN "default"."T" AS "T3" ON true;

-- Left cross join
--#[join-06]
SELECT "T1"."c", "T2"."z", "T1"."a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON true;

-- Left join with true condition
--#[join-07]
SELECT "T1"."b", "T2"."c", "T1"."v" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON true;

-- Full join with true condition
-- Redshift is failing on this query and likely a bug in redshift
-- ERROR: could not devise a query plan for the given query [ErrorId: 1-698c0a0c-49191b3e14f0d41c6bd39898]
--#[join-08]
SELECT "T1"."a", "T2"."b", "T1"."z" FROM "default"."T" AS "T1" FULL JOIN "default"."T" AS "T2" ON true;

-- Inner join with equality condition
--#[join-09]
SELECT "T1"."a", "T2"."c", "T1"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Inner join with specific columns
--#[join-10]
SELECT "T1"."a", "T2"."b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Right join
--#[join-11]
SELECT "T1"."c", "T2"."a", "T2"."z" FROM "default"."T" AS "T1" RIGHT JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Right outer join (explicit)
--#[join-12]
SELECT "T1"."b", "T2"."v", "T1"."a" FROM "default"."T" AS "T1" RIGHT JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Full outer join (explicit)
--#[join-13]
SELECT "T1"."c", "T2"."b", "T1"."z" FROM "default"."T" AS "T1" FULL JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Join with multiple AND conditions
--#[join-14]
SELECT "T1"."a", "T2"."v", "T1"."b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"."b" = "T2"."b") AND ("T1"."c" = "T2"."c");

-- Join with OR condition
--#[join-15]
SELECT "T1"."c", "T2"."z", "T1"."a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"."b" = "T2"."b") OR ("T1"."c" = "T2"."c");

-- Three table inner joins
--#[join-16]
SELECT "T1"."a", "T2"."c", "T3"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" INNER JOIN "default"."T" AS "T3" ON "T2"."b" = "T3"."b";

-- Three table left joins
--#[join-17]
SELECT "T1"."b", "T2"."z", "T3"."a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" LEFT JOIN "default"."T" AS "T3" ON "T2"."b" = "T3"."b";

-- Mixed join types (inner and left)
--#[join-18]
SELECT "T1"."c", "T2"."a", "T3"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" LEFT JOIN "default"."T" AS "T3" ON "T2"."b" = "T3"."b";

-- Self join
--#[join-19]
SELECT "T1"."a", "T2"."c", "T1"."z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Join with WHERE clause
--#[join-20]
SELECT "T1"."b", "T2"."v", "T1"."a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" WHERE "T1"."c" = 'active';

-- Left join with NULL check (anti-join pattern)
--#[join-21]
SELECT "T1"."c", "T1"."z", "T1"."a" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" WHERE "T2"."b" IS NULL;

-- Join with aggregation and GROUP BY
--#[join-22]
SELECT "T1"."c", count(1) AS "_1" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" GROUP BY "T1"."c";

-- Left join with aggregation
--#[join-23]
SELECT "T1"."b", sum("T2"."b") AS "_1" FROM "default"."T" AS "T1" LEFT JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" GROUP BY "T1"."b";

-- Join with ORDER BY
--#[join-24]
SELECT "T1"."a", "T2"."b", "T1"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" ORDER BY "T1"."c" ASC NULLS LAST;

-- Join with multiple ORDER BY columns
--#[join-25]
SELECT "T1"."c", "T2"."z", "T1"."a" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" ORDER BY "T1"."c" ASC NULLS LAST, "T2"."timestamp_1" DESC NULLS FIRST;

-- Join with subquery
--#[join-26]
SELECT "T1"."a", "T2"."c", "T1"."v" FROM "default"."T" AS "T1" INNER JOIN (SELECT "T"."b", "T"."c" FROM "default"."T" AS "T" WHERE "T"."a" = true) AS "T2" ON "T1"."b" = "T2"."b";

-- Join with complex condition (comparison)
--#[join-27]
SELECT "T1"."b", "T2"."c", "T1"."z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON ("T1"."b" = "T2"."b") AND (CAST("T1"."timestamp_1" AS TIMESTAMP) > CAST("T2"."timestamp_1" AS TIMESTAMP));

-- Join with range condition
--#[join-28]
SELECT "T1"."a", "T2"."v", "T1"."c" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON (CAST("T1"."timestamp_1" AS TIMESTAMP) <= CAST("T2"."timestamp_1" AS TIMESTAMP)) AND (CAST("T1"."timestamp_2" AS TIMESTAMP) >= CAST("T2"."timestamp_1" AS TIMESTAMP));

-- Join using USING clause (single column)
-- Left join using USING clause (multiple columns)
-- Natural join
-- Four table joins
--#[join-30]
SELECT "T1"."a", "T2"."c", "T3"."v", "T4"."z" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" INNER JOIN "default"."T" AS "T3" ON "T2"."b" = "T3"."b" INNER JOIN "default"."T" AS "T4" ON "T3"."b" = "T4"."b";

-- Join with DISTINCT
--#[join-31]
SELECT DISTINCT "T1"."b" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b";

-- Join with LIMIT
--#[join-32]
SELECT "T1"."c", "T2"."a", "T1"."v" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" LIMIT 10;

-- Join with HAVING clause
--#[join-33]
SELECT "T2"."c", count(1) AS "_1" FROM "default"."T" AS "T1" INNER JOIN "default"."T" AS "T2" ON "T1"."b" = "T2"."b" GROUP BY "T2"."c" HAVING count(1) > CAST(5 AS BIGINT);
