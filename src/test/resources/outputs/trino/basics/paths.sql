-- ----------------------------------------
--  Trino Array Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-16]
-- array navigation with literal (1-indexed)
SELECT "t"."array"[1] AS "v" FROM "default"."T" AS "t";

-- Additional path exprs
--#[paths-sfw-17]
SELECT "t"."flds"."a" AS "v" FROM "default"."EXCLUDE_T" AS "t";

--#[paths-sfw-18]
SELECT "t"."flds"."a" AS "v" FROM "default"."EXCLUDE_T" AS "t";

--#[paths-sfw-19]
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T" AS "t";

-- qualified path
--#[paths-sfw-20]
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T" AS "t";

-- nullable fields
--#[paths-sfw-21]
SELECT "t"."flds".a.field_x AS "v" FROM "default"."EXCLUDE_T_NULLABLE" AS "t";

-- nullable fields + qualified path
--#[paths-sfw-22]
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T_NULLABLE" AS "t";

-- nullable fields + mix of qualified and unqualified path components
--#[paths-sfw-23]
SELECT "t"."flds"."a".field_x AS "v" FROM "default"."EXCLUDE_T_NULLABLE" AS "t";
