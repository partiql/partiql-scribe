--#[exclude-00]
SELECT "t"."flds" AS "flds" FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-01]
SELECT "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-02] TODO(duckdb)
SELECT {'a': "t"."flds"."a", 'c': "t"."flds"."c"} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-03] TODO(duckdb)
SELECT {'a': "t"."flds"."a", 'b': "t"."flds"."b", 'c': {'field_y': "t"."flds"."c"."field_y"}} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-04] TODO(duckdb)
SELECT {'a': "t"."flds"."a", 'c': {'field_y': "t"."flds"."c"."field_y"}} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-05] TODO(duckdb)
SELECT {'a': "t"."flds"."a", 'b': "t"."flds"."b", 'c': {'field_x': "t"."flds"."c"."field_x"}} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-06]
-- Excludes all the fields of `t.flds.c`, which would produce an empty struct. DuckDB does not support empty
-- structs (`{}`, `ROW()`, and `struct_pack()` all error), so Scribe rejects this with UNSUPPORTED_PLAN_TO_AST_CONVERSION.
-- Exclude the containing field instead, e.g. `EXCLUDE t.flds.c`.
-- SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
-- --#[exclude-07] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_y': ___coll_wildcard___."field_y"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

-- --#[exclude-08] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_x': ___coll_wildcard___."field_x"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

-- --#[exclude-09] TODO(duckdb)
SELECT "t1"."flds" AS "flds", {'a': "t2"."flds"."a", 'c': {'field_y': "t2"."flds"."c"."field_y"}} AS "flds", "t2"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo";

-- --#[exclude-10] TODO(duckdb)
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT {'b': "t1"."flds"."b", 'c': "t1"."flds"."c"} AS "flds", "t1"."foo" AS "foo", {'a': "t2"."flds"."a", 'c': "t2"."flds"."c"} AS "flds", "t2"."foo" AS "foo", {'a': "t3"."flds"."a", 'b': "t3"."flds"."b"} AS "flds", "t3"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

-- --#[exclude-11] TODO(duckdb)
-- EXCLUDE with select projection list and multiple JOINs
SELECT {'b': "t1"."flds"."b", 'c': "t1"."flds"."c"} AS "flds", {'a': "t2"."flds"."a", 'c': "t2"."flds"."c"} AS "flds", {'a': "t3"."flds"."a", 'b': "t3"."flds"."b"} AS "flds" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE ("t1"."foo" = "t2"."foo") AND ("t2"."foo" = "t3"."foo");

-- EXCLUDE with different types
-- bool
-- --#[exclude-12] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_BOOL" AS "t";

-- int16
-- --#[exclude-13] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_INT16" AS "t";

-- int32
-- --#[exclude-14] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_INT32" AS "t";

-- int64
-- --#[exclude-15] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_INT64" AS "t";

-- int (unconstrained)
-- DuckDB does not support unconstrained int; error or give result of BIGINT
-- --#[exclude-16]
-- SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT AS t;

-- decimal
-- DuckDB does not support unconstrained decimal; error or give result of DECIMAL(38, 38)
-- --#[exclude-17]
-- SELECT * EXCLUDE t.foo.bar FROM datatypes.T_DECIMAL AS t;

-- float32
-- --#[exclude-18] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_FLOAT32" AS "t";

-- float64
-- --#[exclude-19] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_FLOAT64" AS "t";

-- string
-- --#[exclude-20] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_STRING" AS "t";

-- date
-- --#[exclude-21] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_DATE" AS "t";

-- time
-- --#[exclude-22] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_TIME" AS "t";

-- timestamp
-- --#[exclude-23] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- struct
-- --#[exclude-25] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_STRUCT" AS "t";

-- list
-- --#[exclude-26] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_LIST" AS "t";

-- decimal(5, 2)
-- --#[exclude-27] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- varchar(16)
-- --#[exclude-28] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_STRING_16" AS "t";

-- char(16)
-- --#[exclude-29] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_CHAR_16" AS "t";

-- time(6)
-- --#[exclude-30] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_TIME_6" AS "t";

-- timestamp(6)
-- --#[exclude-31] TODO(duckdb)
SELECT {'keep': "t"."foo"."keep"} AS "foo" FROM "default"."datatypes"."T_TIMESTAMP_6" AS "t";

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

-- --#[exclude-49] TODO(duckdb)
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT {'a': "t"."flds"."a", 'c': {'field_y': "t"."flds"."c"."field_y"}} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T" AS "t";

-- --#[exclude-50] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_y': ___coll_wildcard___."field_y", 'field_z': ___coll_wildcard___."field_z", 'nested_list': ___coll_wildcard___."nested_list"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

-- --#[exclude-51] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_x': ___coll_wildcard___."field_x", 'field_z': ___coll_wildcard___."field_z", 'nested_list': ___coll_wildcard___."nested_list"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

-- --#[exclude-52] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_x': ___coll_wildcard___."field_x", 'field_y': ___coll_wildcard___."field_y", 'nested_list': ___coll_wildcard___."nested_list"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

-- --#[exclude-53] TODO(duckdb)
SELECT list_transform("t"."a", ___coll_wildcard___ -> {'field_x': ___coll_wildcard___."field_x", 'field_y': ___coll_wildcard___."field_y", 'field_z': ___coll_wildcard___."field_z"}) AS "a", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

-- --#[exclude-54] TODO(duckdb)
SELECT {'select': {'field_y': "t"."flds"."select"."field_y"}, 'order': "t"."flds"."order"} AS "flds", "t"."foo" AS "foo" FROM "default"."EXCLUDE_T_RESERVED_KEYWORDS" AS "t";
