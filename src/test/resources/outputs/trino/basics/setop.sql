-- SQL set ops

-- SQL UNION
--#[setop-00]
(SELECT "t1"."a" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION DISTINCT (SELECT "t2"."a" AS "a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-01]
(SELECT "t1"."a" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a" AS "a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-02]
((SELECT "t1"."a" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a" AS "a" FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3"."a" AS "a" FROM "default"."SIMPLE_T" AS "t3");

--#[setop-03]
(SELECT "t1"."a" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"."a" AS "a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"."a" AS "a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-04]
(SELECT "t1"."c" AS "c", "t1"."b" AS "b", "t1"."a" AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"."c" AS "c", "t2"."b" AS "b", "t2"."a" AS "a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"."c" AS "c", "t3"."b" AS "b", "t3"."a" AS "a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-05]
((SELECT "t1"."a" AS "a", "t1"."b" AS "b", "t1"."c" AS "c" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a" AS "a", "t2"."b" AS "b", "t2"."c" AS "c" FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3"."a" AS "a", "t3"."b" AS "b", "t3"."c" AS "c" FROM "default"."SIMPLE_T" AS "t3");

--#[setop-06]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") UNION DISTINCT (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-07]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "a", "t1"."col_timestamp" AS "b", "t1"."col_float64" AS "c" FROM "default"."T_ALL_TYPES" AS "t1") UNION DISTINCT (SELECT "t2"."col_int64" AS "a", CAST("t2"."col_date" AS TIMESTAMP) AS "b", CAST("t2"."col_decimal" AS DOUBLE) AS "c" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-08]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") UNION ALL (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-09]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") EXCEPT DISTINCT (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-10]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") EXCEPT ALL (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-11]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") INTERSECT DISTINCT (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-12]
(SELECT CAST("t1"."col_int32" AS BIGINT) AS "_", "t1"."col_timestamp" AS "_", "t1"."col_float64" AS "_" FROM "default"."T_ALL_TYPES" AS "t1") INTERSECT ALL (SELECT "t2"."col_int64" AS "_", CAST("t2"."col_date" AS TIMESTAMP) AS "_", CAST("t2"."col_decimal" AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2");

--#[setop-13]
(SELECT CAST(CASE WHEN "t1"."col_int32" = 0 THEN 0 ELSE 1 / CAST("t1"."col_int32" AS DECIMAL(10,0)) END AS DECIMAL(21,11)) AS "_1", CAST(DATE '2026-04-15' AS TIMESTAMP) AS "_", ("t1"."col_float64" * CAST(3 AS DOUBLE)) - CAST(10 AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t1") INTERSECT ALL (SELECT CAST("char_length"("t2"."col_string") AS DECIMAL(21,11)) AS "_1", "t2"."col_timestamp" AS "_", CAST((SELECT 123.45 AS "_1" FROM "default"."T_ALL_TYPES" AS "T_ALL_TYPES") AS DOUBLE) AS "_" FROM "default"."T_ALL_TYPES" AS "t2")