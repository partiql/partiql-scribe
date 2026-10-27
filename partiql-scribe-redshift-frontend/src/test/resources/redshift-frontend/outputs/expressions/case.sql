--#[case-types-00]
SELECT CASE WHEN "t"['a'] THEN CAST("t"['b'] AS DECIMAL(11,1)) ELSE 2.5 END AS "v" FROM "default"."T" AS "t";

--#[case-types-01]
SELECT CASE WHEN "t_all_types"['col_bool'] THEN "t_all_types"['col_int64'] ELSE CAST("t_all_types"['col_int16'] AS BIGINT) END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[case-types-02]
SELECT CASE WHEN "t_all_types"['col_bool'] THEN CAST("t_all_types"['col_date'] AS TIMESTAMP) ELSE "t_all_types"['col_timestamp'] END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[case-types-03]
SELECT CASE WHEN "t_all_types"['col_bool'] THEN "t_all_types"['col_float64'] ELSE CAST(2.5 AS DOUBLE PRECISION) END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[case-types-04]
[UNSUPPORTED{message="CASE with results of types DECIMAL(38, 0), DECIMAL(2, 1)"}];

--#[case-types-05]
[UNSUPPORTED{message="CASE with REAL and DECIMAL results"}];

--#[case-types-06]
[UNSUPPORTED{message="CASE with results of types INTEGER, VARCHAR"}];

--#[case-types-07]
SELECT CASE WHEN "t"['a'] THEN "t"['b'] ELSE 0 END AS "v" FROM "default"."T" AS "t";

--#[case-types-08]
SELECT CASE WHEN "t"['a'] THEN "t"['c'] ELSE 'none' END AS "v1", COALESCE(CASE WHEN "t"['a'] THEN "t"['x'] ELSE "t"['b'] END) AS "v2" FROM "default"."T" AS "t";

--#[case-null-00]
SELECT 2 AS "v" FROM "default"."T" AS "t";

--#[case-null-01]
SELECT CAST(1 AS DECIMAL(11,1)) AS "v" FROM "default"."T" AS "t";

--#[case-null-02]
SELECT CASE WHEN "t"['a'] THEN NULL ELSE "t"['b'] END AS "v" FROM "default"."T" AS "t";

--#[case-null-03]
SELECT CAST(NULL AS INT) AS "v" FROM "default"."T" AS "t";

--#[case-simple-00]
SELECT CASE WHEN "t"['b'] = 1 THEN 'one' WHEN "t"['b'] = 2 THEN 'two' ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[case-simple-01]
SELECT CASE WHEN "t_all_types"['col_date'] = DATE '2020-01-01' THEN 1 ELSE 0 END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[case-simple-02]
SELECT CASE WHEN TRIM(TRAILING FROM "t_all_types"['col_char']) = 'abc' THEN 1 ELSE 0 END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[case-condition-00]
[UNSUPPORTED{message="non-boolean condition"}];
