--#[cond-coalesce-00]
SELECT COALESCE("t_all_types"['col_decimal'], CAST(0 AS DECIMAL(38,0))) + CAST(1 AS DECIMAL(10,0)) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-01]
SELECT CAST(sum(COALESCE("t_all_types"['col_decimal'], CAST(0 AS DECIMAL(38,0)))) AS DECIMAL(38,0)) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-02]
SELECT COALESCE("t_all_types"['col_int64'], CAST(0 AS BIGINT)) AS "v1", COALESCE(CAST("t_all_types"['col_int16'] AS INT), "t_all_types"['col_int32']) AS "v2", COALESCE(CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION), "t_all_types"['col_float64']) AS "v3" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-03]
SELECT COALESCE(CAST("t_all_types"['col_date'] AS TIMESTAMP (6)), "t_all_types"['col_timestamp']) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-04]
SELECT COALESCE("t"['c'], "t"['z'], 'none') AS "v1", COALESCE("t"['b'], 1, 2) AS "v2" FROM "default"."T" AS "t";

--#[cond-coalesce-05]
[UNSUPPORTED{message="`COALESCE` of INTEGER and DECIMAL(38, 30) values: the common type cannot hold all values"}];

--#[cond-coalesce-06]
SELECT COALESCE("t_all_types"['col_int32'], 0) AS "v1", COALESCE("t_all_types"['col_date'], DATE '2020-01-01') AS "v2", COALESCE("t_all_types"['col_bool'], true) AS "v3" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-07]
[UNSUPPORTED{message="`COALESCE` with the character string 'abc' and a value of type INTEGER; Redshift converts the string implicitly"}];

--#[cond-coalesce-08]
[UNSUPPORTED{message="`COALESCE` of INTEGER and VARCHAR values"}];

--#[cond-coalesce-09]
SELECT COALESCE(TRIM(TRAILING FROM "t_all_types"['col_char']), 'x') AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-coalesce-10]
SELECT COALESCE("t"['x'], 0) AS "v" FROM "default"."T" AS "t";

--#[cond-nullif-00]
SELECT NULLIF("t"['b'], 0) AS "v1", NULLIF("t"['c'], '') AS "v2" FROM "default"."T" AS "t";

--#[cond-nullif-01]
SELECT CASE WHEN TRIM(TRAILING FROM "t_all_types"['col_char']) = '' THEN NULL ELSE "t_all_types"['col_char'] END AS "v1", CASE WHEN TRIM(TRAILING FROM "t_all_types"['col_char']) = 'ab' THEN NULL ELSE "t_all_types"['col_char'] END AS "v2" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-nullif-02]
SELECT NULLIF("t_all_types"['col_int32'], 0) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-nullif-03]
[UNSUPPORTED{message="`NULLIF` with the character string '-0' and a value of type INTEGER; Redshift converts the string implicitly"}];

--#[cond-nullif-04]
[UNSUPPORTED{message="`NULLIF` comparison of INTEGER and VARCHAR values"}];

--#[cond-decode-00]
SELECT CASE WHEN "t"['b'] = 1 THEN 'one' WHEN "t"['b'] = 2 THEN 'two' ELSE 'other' END AS "v" FROM "default"."T" AS "t";

--#[cond-decode-01]
SELECT CASE WHEN "t"['b'] IS NULL THEN 0 ELSE 1 END AS "v" FROM "default"."T" AS "t";

--#[cond-decode-02]
SELECT CASE WHEN ("t"['timestamp_1'] = "t"['timestamp_2']) OR ("t"['timestamp_1'] IS NULL AND "t"['timestamp_2'] IS NULL) THEN 'same' ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[cond-decode-03]
SELECT CASE WHEN "t"['b'] = 1 THEN CAST(2 AS DECIMAL(11,1)) ELSE 2.5 END AS "v1", CASE WHEN "t"['b'] = 1 THEN 'one' ELSE NULL END AS "v2" FROM "default"."T" AS "t";

--#[cond-decode-04]
SELECT CASE WHEN TRIM(TRAILING FROM "t_all_types"['col_char']) = 'ab' THEN 1 ELSE 0 END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-decode-05]
[UNSUPPORTED{message="`DECODE` with fewer than 3 arguments"}];

--#[cond-greatest-00]
SELECT CASE WHEN "t"['b'] >= 0 THEN "t"['b'] ELSE 0 END AS "v1", CASE WHEN "t"['b'] <= 10 THEN "t"['b'] ELSE 10 END AS "v2", "t"['b'] AS "v3" FROM "default"."T" AS "t";

--#[cond-greatest-01]
SELECT CASE WHEN "t"['timestamp_1'] >= COALESCE("t"['timestamp_2'], "t"['timestamp_1']) THEN "t"['timestamp_1'] ELSE "t"['timestamp_2'] END AS "v" FROM "default"."T" AS "t";

--#[cond-greatest-02]
SELECT CASE WHEN ("t"['timestamp_1'] >= COALESCE("t"['timestamp_2'], "t"['timestamp_1'])) AND ("t"['timestamp_1'] >= TIMESTAMP '2020-01-01 00:00:00') THEN "t"['timestamp_1'] WHEN "t"['timestamp_2'] >= TIMESTAMP '2020-01-01 00:00:00' THEN "t"['timestamp_2'] ELSE TIMESTAMP '2020-01-01 00:00:00' END AS "v" FROM "default"."T" AS "t";

--#[cond-greatest-03]
SELECT CASE WHEN CAST("t"['b'] AS DECIMAL(11,1)) >= 2.5 THEN CAST("t"['b'] AS DECIMAL(11,1)) ELSE 2.5 END AS "v" FROM "default"."T" AS "t";

--#[cond-greatest-04]
SELECT CASE WHEN CAST(TRIM(TRAILING FROM "t_all_types"['col_char']) AS STRING) <= 'b' THEN TRIM(TRAILING FROM "t_all_types"['col_char']) ELSE 'b' END AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-greatest-05]
[UNSUPPORTED{message="`GREATEST` with an argument of type SUPER"}];

--#[cond-greatest-06]
[UNSUPPORTED{message="`GREATEST` of INTEGER and VARCHAR values"}];

--#[cond-nvl2-00]
SELECT CASE WHEN "t"['c'] IS NOT NULL THEN 'present' ELSE 'absent' END AS "v1", CASE WHEN "t"['b'] IS NOT NULL THEN "t"['b'] ELSE 0 END AS "v2", CASE WHEN "t"['c'] IS NOT NULL THEN CAST("t"['b'] AS DECIMAL(11,1)) ELSE 2.5 END AS "v3" FROM "default"."T" AS "t";

--#[cond-nvl2-01]
SELECT CASE WHEN "t_all_types"['col_int32'] IS NOT NULL THEN 5 ELSE 7 END AS "v1", CASE WHEN "t_all_types"['col_int32'] IS NOT NULL THEN CAST("t_all_types"['col_date'] AS TIMESTAMP (6)) ELSE "t_all_types"['col_timestamp'] END AS "v2" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[cond-nvl2-02]
[UNSUPPORTED{message="`NVL2` with result values of different types (VARCHAR and INTEGER)"}];

--#[cond-nvl2-03]
[UNSUPPORTED{message="`NVL2` with result values of different types (VARCHAR(16) and VARCHAR)"}];

--#[cond-subquery-00]
[UNSUPPORTED{message="subquery as an argument of `NVL` (the Scribe PartiQL target prints it without parentheses)"}];

--#[cond-subquery-01]
SELECT CASE WHEN (SELECT max("t"['b']) AS "max" FROM "default"."T" AS "t") >= 0 THEN (SELECT max("t"['b']) AS "max" FROM "default"."T" AS "t") ELSE 0 END AS "v" FROM "default"."T" AS "t";

--#[cond-nested-00]
SELECT COALESCE(sum("t"['b']), CAST(0 AS BIGINT)) AS "v" FROM "default"."T" AS "t";

--#[cond-nested-01]
SELECT CASE WHEN count(1) = 0 THEN 'none' ELSE 'some' END AS "v" FROM "default"."T" AS "t";

--#[cond-nested-02]
SELECT "t"['c'] AS "c" FROM "default"."T" AS "t" GROUP BY "t"['c'] HAVING COALESCE(sum("t"['b']), CAST(0 AS BIGINT)) > CAST(10 AS BIGINT);
