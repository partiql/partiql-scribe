-- Test Data
-- Create schema and table for testing subqueries in Redshift
-- CREATE SCHEMA IF NOT EXISTS "public";

-- Create test table T
-- CREATE TABLE "public"."T" (
--                               "a" BOOLEAN,
--                               "b" INTEGER,
--                               "c" INTEGER,
--                               "v" VARCHAR(50)
-- );

-- Insert sample data
-- INSERT INTO "public"."T" ("a", "b", "c", "v") VALUES
--                                                  (true, 1, 10, 'value1'),
--                                                  (false, 2, 20, 'value2'),
--                                                  (true, 3, 30, 'value3'),
--                                                  (false, 0, 40, 'value4'),
--                                                  (true, -1, 50, 'value5');

-- Scalar subquery coercion
-- #[subquery-00]
-- SELECT 1 = (SELECT "T"."b" FROM "public"."T" AS "T");

-- Row value subquery coercion
-- #[subquery-01]
-- (false, 1) = (SELECT "T"."a", "T"."b" FROM "public"."T" AS "T");

-- Scalar subquery coercion with aggregation
-- #[subquery-02]
-- SELECT 100 = (SELECT count(1) AS "_1" FROM "public"."T" AS "T");

-- Comparison operators with subqueries
--#[subquery-03]
SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" = (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-04]
SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-05]
SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" < (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-06]
SELECT "T"."v" FROM "public"."T" AS "T" WHERE "T"."b" >= (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-07]
SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" <= (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

--#[subquery-08]
SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" <> (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 1);

-- IN collection subquery
--#[subquery-09]
SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" IN (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);

-- NOT IN subquery
--#[subquery-10]
SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" NOT IN (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);

-- EXISTS subquery
-- #[subquery-11]
-- SELECT "T"."b" FROM "public"."T" AS "T" WHERE EXISTS (SELECT "t2"."v" FROM "public"."T" AS "t2" WHERE "t2"."b" > 0);

-- NOT EXISTS subquery
-- #[subquery-12]
-- SELECT "T"."v" FROM "public"."T" AS "T" WHERE NOT EXISTS (SELECT "t2"."a" FROM "public"."T" AS "t2" WHERE "t2"."b" > 0 AND "t2"."b" > 0);

-- UNIQUE subquery
-- #[subquery-13]
-- SELECT "T"."a" FROM "public"."T" AS "T" WHERE UNIQUE (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);

-- Nested subqueries
--#[subquery-14]
SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" IN (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" = 0));

-- Subquery in SELECT clause
--#[subquery-15]
SELECT "T"."b", (SELECT "t2"."v" FROM "public"."T" AS "t2" WHERE "t2"."b" = 0) AS "match_v" FROM "public"."T" AS "T";

-- Multiple subqueries in WHERE
-- #[subquery-16]
-- SELECT "T"."v" FROM "public"."T" AS "T" WHERE "T"."b" > (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0) AND EXISTS (SELECT "t2"."a" FROM "public"."T" AS "t2" WHERE "t2"."b" > 0);

-- Correlated subquery
-- #[subquery-17]
-- SELECT "t1"."a" FROM "public"."T" AS "t1" WHERE "t1"."b" > (SELECT "t2"."b" FROM "public"."T" AS "t2" WHERE "t1"."b" = "t2"."b");

-- ANY/SOME with subquery
-- #[subquery-18]
-- SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > ANY (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);

-- #[subquery-19]
-- SELECT "T"."v" FROM "public"."T" AS "T" WHERE "T"."b" = SOME (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);

-- ALL with subquery
-- #[subquery-20]
-- SELECT "T"."a" FROM "public"."T" AS "T" WHERE "T"."b" > ALL (SELECT "T"."b" FROM "public"."T" AS "T" WHERE "T"."b" > 0);