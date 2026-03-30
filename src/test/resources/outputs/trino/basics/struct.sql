-- ----------------------------------------
--  Select struct columns
-- ----------------------------------------

--#[struct-00]
-- Select a simple struct column
SELECT "T"."col_struct_simple" AS "col_struct_simple" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-01]
-- Select a nested struct column
SELECT "T"."col_struct_nested" AS "col_struct_nested" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-02]
-- Select struct alongside scalar columns
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple" AS "col_struct_simple", "T"."col_struct_nested" AS "col_struct_nested" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Path navigation — simple struct
-- ----------------------------------------

--#[struct-03]
-- Access multiple fields of a simple struct
SELECT "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_simple"."level1_col_string" AS "level1_col_string" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-04]
-- Access all fields of a simple struct
SELECT "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_simple"."level1_col_double" AS "level1_col_double", "T"."col_struct_simple"."level1_col_string" AS "level1_col_string", "T"."col_struct_simple"."level1_col_timestamp" AS "level1_col_timestamp" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Path navigation — nested struct
-- ----------------------------------------

--#[struct-05]
-- Access multiple fields within the nested struct
SELECT "T"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-06]
-- Mix top-level and nested field access
SELECT "T"."col_struct_nested"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_double" AS "level2_col_double" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Struct wildcard (dot-star)
-- ----------------------------------------

--#[struct-07]
-- Wildcard on simple struct
SELECT "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_simple"."level1_col_double" AS "level1_col_double", "T"."col_struct_simple"."level1_col_string" AS "level1_col_string", "T"."col_struct_simple"."level1_col_timestamp" AS "level1_col_timestamp" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-08]
-- Wildcard on nested struct
SELECT "T"."col_struct_nested"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_double" AS "level1_col_double", "T"."col_struct_nested"."level1_col_string" AS "level1_col_string", "T"."col_struct_nested"."level1_col_timestamp" AS "level1_col_timestamp", "T"."col_struct_nested"."level1_col_struct" AS "level1_col_struct" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-09]
-- Wildcard on inner nested struct
SELECT "T"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_double" AS "level2_col_double", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string", "T"."col_struct_nested"."level1_col_struct"."level2_col_timestamp" AS "level2_col_timestamp" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-10]
-- Wildcard with other columns
SELECT "T"."col_int32" AS "col_int32", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_simple"."level1_col_double" AS "level1_col_double", "T"."col_struct_simple"."level1_col_string" AS "level1_col_string", "T"."col_struct_simple"."level1_col_timestamp" AS "level1_col_timestamp" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Struct construction in SELECT
-- ----------------------------------------

--#[struct-11]
-- Construct a struct literal in SELECT
SELECT CAST(ROW("T"."col_int32", "T"."col_string") AS ROW("a" INTEGER, "b" VARCHAR)) AS "constructed" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-12]
-- Construct a struct from struct fields
SELECT CAST(ROW("T"."col_struct_simple"."level1_col_int", "T"."col_struct_simple"."level1_col_string") AS ROW("x" INTEGER, "y" VARCHAR)) AS "constructed" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-13]
-- Construct a nested struct literal
SELECT CAST(ROW(CAST(ROW("T"."col_struct_nested"."level1_col_struct"."level2_col_int") AS ROW("inner_val" INTEGER))) AS ROW("outer" ROW("inner_val" INTEGER))) AS "constructed" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Filtering on struct fields
-- ----------------------------------------

--#[struct-14]
-- WHERE on a simple struct field
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_simple"."level1_col_int" > 10;

--#[struct-15]
-- WHERE on a nested struct field
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_nested"."level1_col_struct"."level2_col_string" = 'hello';

--#[struct-16]
-- WHERE with multiple struct field conditions
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T" WHERE ("T"."col_struct_simple"."level1_col_int" > 0) AND ("T"."col_struct_nested"."level1_col_double" < 100.0);

--#[struct-17]
-- IS NULL check on struct field
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_simple"."level1_col_string" IS NULL;

--#[struct-18]
-- IS NOT NULL check on nested struct field
SELECT "T"."col_int32" AS "col_int32", "T"."col_string" AS "col_string", "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "level2_col_string" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_nested"."level1_col_struct"."level2_col_timestamp" IS NOT NULL;

-- ----------------------------------------
--  Aliasing struct fields
-- ----------------------------------------

--#[struct-19]
-- Alias multiple struct paths at different nesting levels
SELECT "T"."col_struct_simple"."level1_col_int" AS "s_int", "T"."col_struct_nested"."level1_col_struct"."level2_col_string" AS "n_str" FROM "default"."T_ALL_TYPES" AS "T";

-- ----------------------------------------
--  Struct fields in ORDER BY / GROUP BY
-- ----------------------------------------

--#[struct-20]
-- ORDER BY a struct field
SELECT "T"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T"."col_string" AS "col_string" FROM "default"."T_ALL_TYPES" AS "T" ORDER BY "T"."col_struct_simple"."level1_col_int" ASC NULLS LAST;

--#[struct-21]
-- GROUP BY a struct field
SELECT "T"."col_struct_simple"."level1_col_string" AS "level1_col_string", count(1) AS "cnt" FROM "default"."T_ALL_TYPES" AS "T" GROUP BY "T"."col_struct_simple"."level1_col_string";

-- ----------------------------------------
--  Struct with JOIN
-- ----------------------------------------

--#[struct-22]
-- Join on struct fields
SELECT "t1"."col_struct_simple"."level1_col_int" AS "level1_col_int", "T2"."col_struct_nested"."level1_col_string" AS "level1_col_string" FROM "default"."T_ALL_TYPES" AS "t1" INNER JOIN "default"."T_ALL_TYPES" AS "T2" ON "t1"."col_struct_simple"."level1_col_int" = "T2"."col_struct_nested"."level1_col_int";

--#[struct-23]
-- Left join with nested struct field in condition
SELECT "t1"."col_struct_simple" AS "col_struct_simple", "T2"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int" FROM "default"."T_ALL_TYPES" AS "t1" LEFT JOIN "default"."T_ALL_TYPES" AS "T2" ON "t1"."col_struct_simple"."level1_col_string" = "T2"."col_struct_nested"."level1_col_struct"."level2_col_string";

-- ----------------------------------------
--  Struct in subquery
-- ----------------------------------------

--#[struct-24]
-- Subquery selecting struct fields
SELECT "T"."col_struct_simple"."level1_col_int" AS "level1_col_int" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_simple"."level1_col_int" > (SELECT "T2"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int" FROM "default"."T_ALL_TYPES" AS "T2" WHERE "T2"."col_struct_nested"."level1_col_struct"."level2_col_int" = 1);

--#[struct-25]
-- Struct field in IN subquery
SELECT "T"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_struct_simple"."level1_col_int" IN (SELECT "T2"."col_struct_nested"."level1_col_struct"."level2_col_int" AS "level2_col_int" FROM "default"."T_ALL_TYPES" AS "T2");

-- ----------------------------------------
--  Struct construction with real values
-- ----------------------------------------

--#[struct-26]
-- Construct struct with mixed literal types
SELECT CAST(ROW(42, 3.14, 'hello', false) AS ROW("count" INTEGER, "rate" DECIMAL(3, 2), "message" VARCHAR, "enabled" BOOLEAN)) AS "config" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-27]
-- Construct nested struct with literal values
SELECT CAST(ROW(CAST(ROW(100, 'test@example.com') AS ROW("id" INTEGER, "email" VARCHAR)), '2023-01-01T00:00:00Z') AS ROW("user" ROW("id" INTEGER, "email" VARCHAR), "timestamp" VARCHAR)) AS "record" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[struct-28]
-- -- Construct struct with null values, Unknown type NULL cannot be converted trino.
-- SELECT CAST(ROW(123, NULL, 'test') AS ROW("value" INTEGER, "optional_field" NULL, "description" VARCHAR)) AS "data" FROM "default"."T_ALL_TYPES" AS "T";

--#[struct-29]
-- Construct struct with arithmetic expressions
SELECT CAST(ROW(10 + 5, 3 * 7, 100.0 / CAST(4 AS DECIMAL(10,0))) AS ROW("sum" INTEGER, "product" INTEGER, "ratio" DECIMAL(15, 12))) AS "calculations" FROM "default"."T_ALL_TYPES" AS "T";
