-- Top level expressions are not supported in Redshift

-- Comparison operators with subqueries
--#[subquery-03]
SELECT "T"."a" FROM "default"."T" AS "T" WHERE "T"."b" = (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-04]
SELECT "T"."a" FROM "default"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-05]
SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" < (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-06]
SELECT "T"."v" FROM "default"."T" AS "T" WHERE "T"."b" >= (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-07]
SELECT "T"."a" FROM "default"."T" AS "T" WHERE "T"."b" <= (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-08]
SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" <> (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 1);

-- IN collection subquery
--#[subquery-09]
SELECT "T"."a" FROM "default"."T" AS "T" WHERE "T"."b" IN (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" > 0);

-- NOT IN subquery
--#[subquery-10]
SELECT "T"."a" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" > 0);

-- EXISTS subquery
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-11]
-- SELECT "T"."b" FROM "default"."T" AS "T" WHERE EXISTS (SELECT "t2"."v" FROM "default"."T" AS "t2" WHERE "t2"."b" > 0);

-- NOT EXISTS subquery
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-12]
-- SELECT "T"."v" FROM "default"."T" AS "T" WHERE NOT EXISTS (SELECT "t2"."a" FROM "default"."T" AS "t2" WHERE "t2"."b" > 0 AND "t2"."b" > 0);

-- Nested subqueries
--#[subquery-13]
SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" IN (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" = 0));

-- Subquery in SELECT clause
--#[subquery-14]
SELECT "T"."b", (SELECT "t2"."v" FROM "default"."T" AS "t2" WHERE "t2"."b" = 0) AS "match_v" FROM "default"."T" AS "T";

-- Multiple subqueries in WHERE
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-15]
-- SELECT "T"."v" FROM "default"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "default"."T" AS "T" WHERE "T"."b" > 0) AND EXISTS (SELECT "t2"."a" FROM "default"."T" AS "t2" WHERE "t2"."b" > 0);

-- Correlated subquery
-- Bug for correlated subquery, tracking with https://github.com/partiql/partiql-scribe/issues/119
-- #[subquery-16]
-- SELECT "t1"."a" FROM "default"."T" AS "t1" WHERE "t1"."b" = (SELECT "t2"."b" FROM "default"."T" AS "t2" WHERE "t1"."b" = "t2"."b");
