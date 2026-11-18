-- ----------------------------------------
--  DuckDB Array Navigation (+SFW)
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
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T" AS "t";
-- -- previous test used unqualified field names based
-- SELECT "t"."flds".a.field_x AS "v" FROM "default"."EXCLUDE_T" AS "t";

-- nullable fields + qualified path
--#[paths-sfw-22]
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T" AS "t";

-- nullable fields + mix of qualified and unqualified path components
--#[paths-sfw-23]
SELECT "t"."flds"."a"."field_x" AS "v" FROM "default"."EXCLUDE_T" AS "t";
-- -- previous test used unqualified field names based
-- SELECT "t"."flds"."a".field_x AS "v" FROM "default"."EXCLUDE_T" AS "t";

--#[paths-sfw-24]
[ScribeException{code=INVALID_PLAN}];

--#[paths-sfw-25]
[ScribeException{code=INVALID_PLAN}];

--#[paths-sfw-26]
[ScribeException{code=INVALID_PLAN}];

--#[paths-sfw-27]
[ScribeException{code=INVALID_PLAN}];

--#[paths-sfw-28]
SELECT {'a': 1, 'b': "t"."b"}."a" AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-29]
SELECT {'a': 1, 'b': "t"."b"}."b" AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-30]
SELECT "t"."x".y AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-31]
SELECT "t"."x"."y" AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-32]
SELECT "t"."a"[1]."nested_list"[1] AS "v" FROM "default"."EXCLUDE_T_NESTED_LIST" AS "t";

-- Literal array index in ORDER BY
--#[paths-sfw-33]
SELECT "t"."a" AS "a" FROM "default"."T" AS "t" ORDER BY "t"."array"[1] ASC NULLS LAST;

-- Non-literal array index in ORDER BY. Not supported: DuckDB supports only literal array indexes
--#[paths-sfw-34]
[ScribeException{code=INVALID_PLAN, message="DuckDB array indexing only supports integer literals, e.g. x[0]."}];
