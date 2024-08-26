--#[exclude-00]
SELECT "t"."flds" AS "flds" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-01]
SELECT "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-02]
SELECT "$__EXCLUDE_ALIAS__"."t"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t"."flds"."a"."field_x" AS field_x, "t"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t"."flds"."c"."field_x" AS field_x, "t"."flds"."c"."field_y" AS field_y) AS c) AS flds, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-03]
SELECT "$__EXCLUDE_ALIAS__"."t"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t"."flds"."a"."field_x" AS field_x, "t"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t"."flds"."b"."field_x" AS field_x, "t"."flds"."b"."field_y" AS field_y) AS b, CAST(ROW("t"."flds"."c"."field_y") AS ROW(field_y VARCHAR)) AS c) AS flds, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-04]
SELECT "$__EXCLUDE_ALIAS__"."t"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t"."flds"."a"."field_x" AS field_x, "t"."flds"."a"."field_y" AS field_y) AS a, CAST(ROW("t"."flds"."c"."field_y") AS ROW(field_y VARCHAR)) AS c) AS flds, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-05]
SELECT "$__EXCLUDE_ALIAS__"."t"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t"."flds"."a"."field_x" AS field_x, "t"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t"."flds"."b"."field_x" AS field_x, "t"."flds"."b"."field_y" AS field_y) AS b, CAST(ROW("t"."flds"."c"."field_x") AS ROW(field_x INTEGER)) AS c) AS flds, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

-- --#[exclude-06]
-- Exclude all the fields of `t.flds.c`; unsure if Trino supports ROWs with no fields. Asked a question in discussion to see if feasible https://github.com/trinodb/trino/discussions/20558
-- Can just `EXCLUDE t.flds.c` rather than excluding all the fields individually
-- SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT "$__EXCLUDE_ALIAS__"."t"."a" AS "a", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_y") AS ROW(field_y VARCHAR))) AS a, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-08]
SELECT "$__EXCLUDE_ALIAS__"."t"."a" AS "a", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT transform("t"."a", ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___."field_x") AS ROW(field_x INTEGER))) AS a, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-09]
SELECT "$__EXCLUDE_ALIAS__"."t1"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t2"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t2"."foo" AS "foo" FROM (SELECT CAST(ROW((SELECT (SELECT "t1"."flds"."a"."field_x" AS field_x, "t1"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t1"."flds"."b"."field_x" AS field_x, "t1"."flds"."b"."field_y" AS field_y) AS b, (SELECT "t1"."flds"."c"."field_x" AS field_x, "t1"."flds"."c"."field_y" AS field_y) AS c)) AS ROW(flds ROW(a ROW(field_x INTEGER, field_y VARCHAR), b ROW(field_x INTEGER, field_y VARCHAR), c ROW(field_x INTEGER, field_y VARCHAR)))) AS "t1", (SELECT (SELECT (SELECT "t2"."flds"."a"."field_x" AS field_x, "t2"."flds"."a"."field_y" AS field_y) AS a, CAST(ROW("t2"."flds"."c"."field_y") AS ROW(field_y VARCHAR)) AS c) AS flds, "t2"."foo" AS foo) AS "t2" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo") AS "$__EXCLUDE_ALIAS__";

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT "$__EXCLUDE_ALIAS__"."t1"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t1"."foo" AS "foo", "$__EXCLUDE_ALIAS__"."t2"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t2"."foo" AS "foo", "$__EXCLUDE_ALIAS__"."t3"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t3"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t1"."flds"."b"."field_x" AS field_x, "t1"."flds"."b"."field_y" AS field_y) AS b, (SELECT "t1"."flds"."c"."field_x" AS field_x, "t1"."flds"."c"."field_y" AS field_y) AS c) AS flds, "t1"."foo" AS foo) AS "t1", (SELECT (SELECT (SELECT "t2"."flds"."a"."field_x" AS field_x, "t2"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t2"."flds"."c"."field_x" AS field_x, "t2"."flds"."c"."field_y" AS field_y) AS c) AS flds, "t2"."foo" AS foo) AS "t2", (SELECT (SELECT (SELECT "t3"."flds"."a"."field_x" AS field_x, "t3"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t3"."flds"."b"."field_x" AS field_x, "t3"."flds"."b"."field_y" AS field_y) AS b) AS flds, "t3"."foo" AS foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo")) AS "$__EXCLUDE_ALIAS__";

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT "$__EXCLUDE_ALIAS__"."t1"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t2"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t3"."flds" AS "flds" FROM (SELECT (SELECT (SELECT (SELECT "t1"."flds"."b"."field_x" AS field_x, "t1"."flds"."b"."field_y" AS field_y) AS b, (SELECT "t1"."flds"."c"."field_x" AS field_x, "t1"."flds"."c"."field_y" AS field_y) AS c) AS flds, "t1"."foo" AS foo) AS "t1", (SELECT (SELECT (SELECT "t2"."flds"."a"."field_x" AS field_x, "t2"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t2"."flds"."c"."field_x" AS field_x, "t2"."flds"."c"."field_y" AS field_y) AS c) AS flds, "t2"."foo" AS foo) AS "t2", (SELECT (SELECT (SELECT "t3"."flds"."a"."field_x" AS field_x, "t3"."flds"."a"."field_y" AS field_y) AS a, (SELECT "t3"."flds"."b"."field_x" AS field_x, "t3"."flds"."b"."field_y" AS field_y) AS b) AS flds, "t3"."foo" AS foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo")) AS "$__EXCLUDE_ALIAS__";

-- EXCLUDE with different types
-- bool
--#[exclude-12]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep BOOLEAN))) AS ROW(foo ROW(keep BOOLEAN))) AS "t" FROM "default"."datatypes"."T_BOOL" AS "t") AS "$__EXCLUDE_ALIAS__";

-- int16
--#[exclude-13]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep SMALLINT))) AS ROW(foo ROW(keep SMALLINT))) AS "t" FROM "default"."datatypes"."T_INT16" AS "t") AS "$__EXCLUDE_ALIAS__";

-- int32
--#[exclude-14]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep INTEGER))) AS ROW(foo ROW(keep INTEGER))) AS "t" FROM "default"."datatypes"."T_INT32" AS "t") AS "$__EXCLUDE_ALIAS__";

-- int64
--#[exclude-15]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep BIGINT))) AS ROW(foo ROW(keep BIGINT))) AS "t" FROM "default"."datatypes"."T_INT64" AS "t") AS "$__EXCLUDE_ALIAS__";

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
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep DOUBLE))) AS ROW(foo ROW(keep DOUBLE))) AS "t" FROM "default"."datatypes"."T_FLOAT32" AS "t") AS "$__EXCLUDE_ALIAS__";

-- float64
--#[exclude-19]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep DOUBLE))) AS ROW(foo ROW(keep DOUBLE))) AS "t" FROM "default"."datatypes"."T_FLOAT64" AS "t") AS "$__EXCLUDE_ALIAS__";

-- string
--#[exclude-20]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep VARCHAR))) AS ROW(foo ROW(keep VARCHAR))) AS "t" FROM "default"."datatypes"."T_STRING" AS "t") AS "$__EXCLUDE_ALIAS__";

-- date
--#[exclude-21]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep DATE))) AS ROW(foo ROW(keep DATE))) AS "t" FROM "default"."datatypes"."T_DATE" AS "t") AS "$__EXCLUDE_ALIAS__";

-- time
--#[exclude-22]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep TIME))) AS ROW(foo ROW(keep TIME))) AS "t" FROM "default"."datatypes"."T_TIME" AS "t") AS "$__EXCLUDE_ALIAS__";

-- timestamp
--#[exclude-23]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep TIMESTAMP))) AS ROW(foo ROW(keep TIMESTAMP))) AS "t" FROM "default"."datatypes"."T_TIMESTAMP" AS "t") AS "$__EXCLUDE_ALIAS__";

-- null
--#[exclude-24]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep NULL))) AS ROW(foo ROW(keep NULL))) AS "t" FROM "default"."datatypes"."T_NULL" AS "t") AS "$__EXCLUDE_ALIAS__";

-- struct
--#[exclude-25]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW((SELECT "t"."foo"."keep"."keep1" AS keep1, "t"."foo"."keep"."keep2" AS keep2)) AS ROW(keep ROW(keep1 INTEGER, keep2 VARCHAR)))) AS ROW(foo ROW(keep ROW(keep1 INTEGER, keep2 VARCHAR)))) AS "t" FROM "default"."datatypes"."T_STRUCT" AS "t") AS "$__EXCLUDE_ALIAS__";

-- list
--#[exclude-26]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW(transform("t"."foo"."keep", ___coll_wildcard___ -> ___coll_wildcard___)) AS ROW(keep ARRAY<INTEGER>))) AS ROW(foo ROW(keep ARRAY<INTEGER>))) AS "t" FROM "default"."datatypes"."T_LIST" AS "t") AS "$__EXCLUDE_ALIAS__";

-- decimal(5, 2)
--#[exclude-27]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep DECIMAL(5, 2)))) AS ROW(foo ROW(keep DECIMAL(5, 2)))) AS "t" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t") AS "$__EXCLUDE_ALIAS__";

-- varchar(16)
--#[exclude-28]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep VARCHAR(16)))) AS ROW(foo ROW(keep VARCHAR(16)))) AS "t" FROM "default"."datatypes"."T_STRING_16" AS "t") AS "$__EXCLUDE_ALIAS__";

-- char(16)
--#[exclude-29]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep CHAR(16)))) AS ROW(foo ROW(keep CHAR(16)))) AS "t" FROM "default"."datatypes"."T_CHAR_16" AS "t") AS "$__EXCLUDE_ALIAS__";

-- time(6)
--#[exclude-30]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep TIME(6)))) AS ROW(foo ROW(keep TIME(6)))) AS "t" FROM "default"."datatypes"."T_TIME_6" AS "t") AS "$__EXCLUDE_ALIAS__";

-- timestamp(6)
--#[exclude-31]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep TIMESTAMP(6)))) AS ROW(foo ROW(keep TIMESTAMP(6)))) AS "t" FROM "default"."datatypes"."T_TIMESTAMP_6" AS "t") AS "$__EXCLUDE_ALIAS__";

-- union(string, null)
--#[exclude-32]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep VARCHAR))) AS ROW(foo ROW(keep VARCHAR))) AS "t" FROM "default"."datatypes"."T_STRING_NULL" AS "t") AS "$__EXCLUDE_ALIAS__";

-- union(int32, null)
--#[exclude-33]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep INTEGER))) AS ROW(foo ROW(keep INTEGER))) AS "t" FROM "default"."datatypes"."T_INT32_NULL" AS "t") AS "$__EXCLUDE_ALIAS__";

-- union(varchar(16), null)
--#[exclude-34]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep VARCHAR(16)))) AS ROW(foo ROW(keep VARCHAR(16)))) AS "t" FROM "default"."datatypes"."T_STRING_16_NULL" AS "t") AS "$__EXCLUDE_ALIAS__";

-- union(char(16), null)
--#[exclude-35]
SELECT "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT CAST(ROW(CAST(ROW("t"."foo"."keep") AS ROW(keep CHAR(16)))) AS ROW(foo ROW(keep CHAR(16)))) AS "t" FROM "default"."datatypes"."T_CHAR_16_NULL" AS "t") AS "$__EXCLUDE_ALIAS__";

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
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t2" ON "t2"."a" LEFT OUTER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t3" ON "t3"."a" INNER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t4" ON "t4"."a" RIGHT OUTER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t5" ON "t5"."a" FULL OUTER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t6" ON "t6"."a" INNER JOIN
        "default"."T_EXCLUDE_TOP_LEVEL" AS "t7" ON true
WHERE "t1"."a" AND "t2"."a";

--#[exclude-49]
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT "$__EXCLUDE_ALIAS__"."t"."flds" AS "flds", "$__EXCLUDE_ALIAS__"."t"."foo" AS "foo" FROM (SELECT (SELECT (SELECT (SELECT "t"."flds"."a"."field_x" AS field_x, "t"."flds"."a"."field_y" AS field_y) AS a, CAST(ROW("t"."flds"."c"."field_y") AS ROW(field_y VARCHAR)) AS c) AS flds, "t"."foo" AS foo) AS "t" FROM "default"."EXCLUDE_T_NULLABLE" AS "t") AS "$__EXCLUDE_ALIAS__";
