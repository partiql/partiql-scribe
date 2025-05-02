--#[exclude-00]
-- Exclude a top-level field
SELECT "t"."flds" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-01]
SELECT "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-02]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"c"') AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-03]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"b"', '"c"."field_y"') AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-04]
-- Exclude two nested fields
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"c"."field_y"') AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-05]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"b"', '"c"."field_x"') AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"b"', '"c"' SET '"c"', OBJECT()) AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

-- START OF EXCLUDE with COLLECTION WILDCARD
-- COLLECTION WILDCARD is not supported for Redshift target
-- --#[exclude-07]
-- SELECT * EXCLUDE t.a[*].field_x FROM EXCLUDE_T_COLL_WILDCARD AS t;

-- --#[exclude-08]
-- SELECT * EXCLUDE t.a[*].field_y FROM EXCLUDE_T_COLL_WILDCARD AS t;

--#[exclude-09]
-- EXCLUDE with JOIN and WHERE clause
SELECT "t1"."flds", OBJECT_TRANSFORM("t2"."flds" KEEP '"a"', '"c"."field_y"') AS "flds", "t2"."foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo";

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT OBJECT_TRANSFORM("t1"."flds" KEEP '"b"', '"c"') AS "flds", "t1"."foo", OBJECT_TRANSFORM("t2"."flds" KEEP '"a"', '"c"') AS "flds", "t2"."foo", OBJECT_TRANSFORM("t3"."flds" KEEP '"a"', '"b"') AS "flds", "t3"."foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT OBJECT_TRANSFORM("t1"."flds" KEEP '"b"', '"c"') AS "flds", OBJECT_TRANSFORM("t2"."flds" KEEP '"a"', '"c"') AS "flds", OBJECT_TRANSFORM("t3"."flds" KEEP '"a"', '"b"') AS "flds" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

-- Tests for EXCLUDE on top-level columns only --
-- Baseline query without `EXCLUDE`
--#[exclude-36]
SELECT "t"."a", "t"."b", "t"."c", "t"."d", "t"."e", "t"."f", "t"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE single top-level column (no `t.a`)
--#[exclude-37]
SELECT "t"."b", "t"."c", "t"."d", "t"."e", "t"."f", "t"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE multiple top-level columns (no `t.a` through `t.e`)
--#[exclude-38]
SELECT "t"."f", "t"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE top-level columns with WHERE
--#[exclude-39]
SELECT "t"."c", "t"."d", "t"."e", "t"."f", "t"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a" AND ("t"."c" = 'remove');

-- EXCLUDE top-level columns with explicit SELECT list
--#[exclude-40]
SELECT "t"."c", "t"."d", "t"."e", "t"."f", "t"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a" AND ("t"."c" = 'remove');

-- EXCLUDE top-level with subquery
--#[exclude-41]
SELECT "subq"."a", "subq"."b", "subq"."c", "subq"."d", "subq"."e", "subq"."f", "subq"."g" FROM (SELECT "t"."a", "t"."b", "t"."c", "t"."d", "t"."e", "t"."f", "t"."g", 'foo' AS "remove_me" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a") AS "subq";

-- EXCLUDE top-level columns with JOIN
--#[exclude-42]
SELECT "t1"."b", "t1"."d", "t1"."f", "t2"."a", "t2"."c", "t2"."e", "t2"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true;

-- EXCLUDE top-level columns with JOIN and WHERE
--#[exclude-43]
SELECT "t1"."b", "t1"."d", "t1"."f", "t2"."a", "t2"."c", "t2"."e", "t2"."g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT element
--#[exclude-44]
SELECT "t1"."b", "t1"."d", "t1"."f" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT elements
--#[exclude-45]
SELECT "t1"."b", "t1"."d", "t1"."f", "t2"."a" AS "special" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with multiple JOINs
--#[exclude-46]
SELECT
    "t1"."a",
    "t2"."b",
    "t3"."c",
    "t4"."d",
    "t5"."e",
    "t6"."f",
    "t7"."g"
FROM
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON "t2"."a" LEFT OUTER JOIN
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t3" ON "t3"."a" INNER JOIN
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t4" ON "t4"."a" RIGHT OUTER JOIN
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t5" ON "t5"."a" FULL OUTER JOIN
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t6" ON "t6"."a" INNER JOIN
"default"."T_EXCLUDE_TOP_LEVEL" AS "t7" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE all nested fields of a struct
--#[exclude-47]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"b"', '"c"' SET '"a"', OBJECT(), '"b"', OBJECT(), '"c"', OBJECT()) AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

-- EXCLUDE all fields of a top-level struct column
--#[exclude-48]
SELECT OBJECT() AS "flds", "t"."foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-49]
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"a"', '"c"."field_y"') AS "flds", "t"."foo" FROM "default"."EXCLUDE_T_NULLABLE" AS "t";
