--#[select-00]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c" FROM "default"."T" AS "T";

--#[select-01]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T";

--#[select-02]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c" FROM "default"."T" AS "T";

--#[select-03]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION}];

--#[select-04]
SELECT "t1"."a" AS "a", "t1"."b" AS "b", "t1"."c" AS "c", "t1"."d" AS "d", "t1"."x" AS "x", "t1"."array" AS "array", "t1"."z" AS "z", "t1"."v" AS "v", "t1"."timestamp_1" AS "timestamp_1", "t1"."timestamp_2" AS "timestamp_2", "t2"."a" AS "a", "t2"."b" AS "b", "t2"."c" AS "c", "t2"."d" AS "d", "t2"."x" AS "x", "t2"."array" AS "array", "t2"."z" AS "z", "t2"."v" AS "v", "t2"."timestamp_1" AS "timestamp_1", "t2"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "t1" INNER JOIN "default"."T" AS "t2" ON true;

-- #[select-05]
-- ERR! Trino doesn't have path expressions
-- SELECT t.d.* FROM "default"."T";

-- #[select-06]
-- ERR! Trino doesn't have path expressions
-- SELECT t, t.d.* FROM "default"."T";

-- #[select-07]
-- ERR! Trino doesn't have path expressions
-- SELECT t.d.*, t.d.* FROM "default"."T";

-- #[select-08]
-- ERR! Trino doesn't have path expressions
-- SELECT "T"."d".* FROM "default"."T" AS "T";

--#[select-09]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T";

--#[select-10]
SELECT "T"."c" || current_user AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT current_user AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT "t"."a" AS "a" FROM "default"."T" AS "t";

--#[select-div-00]
SELECT "T"."b" / 2 AS "q", (-"T"."b") / 2 AS "nq", CAST("T"."b" AS BIGINT) / CAST(2 AS BIGINT) AS "bq" FROM "default"."T" AS "T";

--#[select-div-01]
SELECT CAST("T"."b" AS DECIMAL(10,0)) / 2.0 AS "q" FROM "default"."T" AS "T";

--#[select-13]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION}];
