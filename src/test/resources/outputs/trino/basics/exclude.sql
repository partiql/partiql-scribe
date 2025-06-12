--#[exclude-00]
SELECT "t"."flds" AS "flds" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-01]
SELECT "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-02]
SELECT CAST(ROW("t"."flds"."a", "t"."flds"."c") AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-03]
SELECT CAST(ROW("t"."flds"."a", "t"."flds"."b", CAST(ROW("t"."flds"."c"."field_y") AS ROW("field_y" VARCHAR))) AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "b" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_y" VARCHAR))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-04]
SELECT CAST(ROW("t"."flds"."a", CAST(ROW("t"."flds"."c"."field_y") AS ROW("field_y" VARCHAR))) AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_y" VARCHAR))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-05]
SELECT CAST(ROW("t"."flds"."a", "t"."flds"."b", CAST(ROW("t"."flds"."c"."field_x") AS ROW("field_x" INTEGER))) AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "b" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-06]
-- Exclude all the fields of `t.flds.c`; unsure if Trino supports ROWs with no fields. Asked a question in discussion to see if feasible https://github.com/trinodb/trino/discussions/20558
-- Can just `EXCLUDE t.flds.c` rather than excluding all the fields individually
-- SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_y") AS ROW("field_y" VARCHAR))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

--#[exclude-08]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_x") AS ROW("field_x" INTEGER))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

--#[exclude-09]
SELECT "t1"."flds" AS "flds", CAST(ROW("t2"."flds"."a", CAST(ROW("t2"."flds"."c"."field_y") AS ROW("field_y" VARCHAR))) AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_y" VARCHAR))) AS "flds", "t2"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo";

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT CAST(ROW("t1"."flds"."b", "t1"."flds"."c") AS ROW("b" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", "t1"."foo" AS "foo", CAST(ROW("t2"."flds"."a", "t2"."flds"."c") AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", "t2"."foo" AS "foo", CAST(ROW("t3"."flds"."a", "t3"."flds"."b") AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "b" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", "t3"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT CAST(ROW("t1"."flds"."b", "t1"."flds"."c") AS ROW("b" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", CAST(ROW("t2"."flds"."a", "t2"."flds"."c") AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", CAST(ROW("t3"."flds"."a", "t3"."flds"."b") AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "b" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

-- EXCLUDE with different types
-- bool
--#[exclude-12]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" BOOLEAN)) AS "foo" FROM "default"."datatypes"."T_BOOL" AS "t"

-- int16
--#[exclude-13]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" SMALLINT)) AS "foo" FROM "default"."datatypes"."T_INT16" AS "t";

-- int32
--#[exclude-14]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" INTEGER)) AS "foo" FROM "default"."datatypes"."T_INT32" AS "t";

-- int64
--#[exclude-15]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" BIGINT)) AS "foo" FROM "default"."datatypes"."T_INT64" AS "t";

-- int (unconstrained)
-- Trino does not support unconstrained int; error or give result of BIGINT
-- --#[exclude-16]
-- SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT AS t;

-- decimal
-- Trino does not support unconstrained decimal; error or give result of DECIMAL(38, 38)
-- --#[exclude-17]
-- SELECT * EXCLUDE t.foo.bar FROM datatypes.T_DECIMAL AS t;

-- float32
--#[exclude-18]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" REAL)) AS "foo" FROM "default"."datatypes"."T_FLOAT32" AS "t";

-- float64
--#[exclude-19]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" DOUBLE)) AS "foo" FROM "default"."datatypes"."T_FLOAT64" AS "t";

-- string
--#[exclude-20]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" VARCHAR)) AS "foo" FROM "default"."datatypes"."T_STRING" AS "t";

-- date
--#[exclude-21]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" DATE)) AS "foo" FROM "default"."datatypes"."T_DATE" AS "t";

-- time
--#[exclude-22]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" TIME)) AS "foo" FROM "default"."datatypes"."T_TIME" AS "t";

-- timestamp
--#[exclude-23]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" TIMESTAMP)) AS "foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- struct
--#[exclude-25]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" ROW("keep1" INTEGER, "keep2" VARCHAR))) AS "foo" FROM "default"."datatypes"."T_STRUCT" AS "t";

-- list
--#[exclude-26]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" ARRAY<INTEGER>)) AS "foo" FROM "default"."datatypes"."T_LIST" AS "t";

-- decimal(5, 2)
--#[exclude-27]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" DECIMAL(5, 2))) AS "foo" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- varchar(16)
--#[exclude-28]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" VARCHAR(16))) AS "foo" FROM "default"."datatypes"."T_STRING_16" AS "t";

-- char(16)
--#[exclude-29]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" CHAR(16))) AS "foo" FROM "default"."datatypes"."T_CHAR_16" AS "t";

-- time(6)
--#[exclude-30]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" TIME(6))) AS "foo" FROM "default"."datatypes"."T_TIME_6" AS "t";

-- timestamp(6)
--#[exclude-31]
SELECT CAST(ROW("t"."foo"."keep") AS ROW("keep" TIMESTAMP(6))) AS "foo" FROM "default"."datatypes"."T_TIMESTAMP_6" AS "t";

-- Tests for EXCLUDE on top-level columns only --
-- Baseline query without `EXCLUDE`
--#[exclude-36]
SELECT "t"."a" AS "a", "t"."b" AS "b", "t"."c" AS "c", "t"."d" AS "d", "t"."e" AS "e", "t"."f" AS "f", "t"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE single top-level column (no `t.a`)
--#[exclude-37]
SELECT "t"."b" AS "b", "t"."c" AS "c", "t"."d" AS "d", "t"."e" AS "e", "t"."f" AS "f", "t"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE multiple top-level columns (no `t.a` through `t.e`)
--#[exclude-38]
SELECT "t"."f" AS "f", "t"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t";

-- EXCLUDE top-level columns with WHERE
--#[exclude-39]
SELECT "t"."c" AS "c", "t"."d" AS "d", "t"."e" AS "e", "t"."f" AS "f", "t"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a" AND ("t"."c" = 'remove');

-- EXCLUDE top-level columns with explicit SELECT list
--#[exclude-40]
SELECT "t"."c" AS "c", "t"."d" AS "d", "t"."e" AS "e", "t"."f" AS "f", "t"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a" AND ("t"."c" = 'remove');

-- EXCLUDE top-level with subquery
--#[exclude-41]
SELECT "subq"."a" AS "a", "subq"."b" AS "b", "subq"."c" AS "c", "subq"."d" AS "d", "subq"."e" AS "e", "subq"."f" AS "f", "subq"."g" AS "g" FROM (SELECT "t"."a" AS "a", "t"."b" AS "b", "t"."c" AS "c", "t"."d" AS "d", "t"."e" AS "e", "t"."f" AS "f", "t"."g" AS "g", 'foo' AS "remove_me" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t" WHERE "t"."a") AS "subq";

-- EXCLUDE top-level columns with JOIN
--#[exclude-42]
SELECT "t1"."b" AS "b", "t1"."d" AS "d", "t1"."f" AS "f", "t2"."a" AS "a", "t2"."c" AS "c", "t2"."e" AS "e", "t2"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true;

-- EXCLUDE top-level columns with JOIN and WHERE
--#[exclude-43]
SELECT "t1"."b" AS "b", "t1"."d" AS "d", "t1"."f" AS "f", "t2"."a" AS "a", "t2"."c" AS "c", "t2"."e" AS "e", "t2"."g" AS "g" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT element
--#[exclude-44]
SELECT "t1"."b" AS "b", "t1"."d" AS "d", "t1"."f" AS "f" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT elements
--#[exclude-45]
SELECT "t1"."b" AS "b", "t1"."d" AS "d", "t1"."f" AS "f", "t2"."a" AS "special" FROM "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON true WHERE "t1"."a" AND "t2"."a";

-- EXCLUDE top-level columns with multiple JOINs
--#[exclude-46]
SELECT
    "t1"."a" AS "a",
    "t2"."b" AS "b",
    "t3"."c" AS "c",
    "t4"."d" AS "d",
    "t5"."e" AS "e",
    "t6"."f" AS "f",
    "t7"."g" AS "g"
FROM
    "default"."T_EXCLUDE_TOP_LEVEL" AS "t1" INNER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON "t2"."a" LEFT JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t3" ON "t3"."a" INNER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t4" ON "t4"."a" RIGHT JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t5" ON "t5"."a" FULL JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t6" ON "t6"."a" INNER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t7" ON true
WHERE "t1"."a" AND "t2"."a";

--#[exclude-49]
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT CAST(ROW("t"."flds"."a", CAST(ROW("t"."flds"."c"."field_y") AS ROW("field_y" VARCHAR))) AS ROW("a" ROW("field_x" INTEGER, "field_y" VARCHAR), "c" ROW("field_y" VARCHAR))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-50]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_y", ___coll_wildcard___."field_z", ___coll_wildcard___."nested_list") AS ROW("field_y" VARCHAR, "field_z" VARCHAR, "nested_list" ARRAY<INTEGER>))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

--#[exclude-51]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_x", ___coll_wildcard___."field_z", ___coll_wildcard___."nested_list") AS ROW("field_x" INTEGER, "field_z" VARCHAR, "nested_list" ARRAY<INTEGER>))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

--#[exclude-52]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_x", ___coll_wildcard___."field_y", ___coll_wildcard___."nested_list") AS ROW("field_x" INTEGER, "field_y" VARCHAR, "nested_list" ARRAY<INTEGER>))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

--#[exclude-53]
SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_x", ___coll_wildcard___."field_y", ___coll_wildcard___."field_z") AS ROW("field_x" INTEGER, "field_y" VARCHAR, "field_z" VARCHAR))) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

--#[exclude-54]
SELECT CAST(ROW(CAST(ROW("t"."flds"."select"."field_y") AS ROW("field_y" VARCHAR)), "t"."flds"."order") AS ROW("select" ROW("field_y" VARCHAR), "order" ROW("field_x" INTEGER, "field_y" VARCHAR))) AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_RESERVED_KEYWORDS" AS "t";
