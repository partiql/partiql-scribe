-- Basic case: struct literal path access in first leg of UNION ALL
--#[trino-sandbox-00]
(SELECT "t1"."b" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."b" AS "a" FROM "default"."SIMPLE_T" AS "t2");

-- Selecting the LAST field (not the first) to verify correct field matching
--#[trino-sandbox-01]
(SELECT "t1"."c" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."c" AS "a" FROM "default"."SIMPLE_T" AS "t2");

-- Struct literal with only literal values (no column references)
--#[trino-sandbox-02]
(SELECT 'hello' AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."c" AS "a" FROM "default"."SIMPLE_T" AS "t2");

-- Multiple struct literal path accesses in the same SELECT
--#[trino-sandbox-03]
(SELECT "t1"."b" AS "a", "t1"."c" AS "b" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."b" AS "a", "t2"."c" AS "b" FROM "default"."SIMPLE_T" AS "t2");

-- Struct literal path access WITHOUT UNION ALL (plain SELECT)
--#[trino-sandbox-04]
SELECT "t1"."b" AS "a" FROM "default"."SIMPLE_T" AS "t1";

-- Nested: struct literal resolves to a struct column, then path into that
--#[trino-sandbox-05]
(SELECT "t1"."d"."e" AS "a" FROM "default"."T" AS "t1") UNION ALL (SELECT "t2"."c" AS "a" FROM "default"."SIMPLE_T" AS "t2");
