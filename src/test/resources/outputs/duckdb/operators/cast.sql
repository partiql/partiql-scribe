-- tests translations of type names across dialects.

-- <character string type>
--#[cast-00]
SELECT CAST('abc' AS VARCHAR) AS "res" FROM "default"."T" AS "T";

--#[cast-01]
SELECT CAST('abc' AS VARCHAR(5)) AS "res" FROM "default"."T" AS "T";

--#[cast-02]
SELECT CAST('abc' AS CHAR) AS "res" FROM "default"."T" AS "T";

--#[cast-03]
SELECT CAST('abc' AS CHAR(5)) AS "res" FROM "default"."T" AS "T";

--#[cast-04]
SELECT CAST('abc' AS VARCHAR) AS "res" FROM "default"."T" AS "T";

--#[cast-05]
SELECT CAST('abc' AS VARCHAR(5)) AS "res" FROM "default"."T" AS "T";

-- --#[cast-06] TODO(duckdb)
SELECT CAST('abc' AS VARCHAR) AS "res" FROM "default"."T" AS "T";

-- <numeric type> - <exact numeric type>
--#[cast-07]
SELECT CAST(1 AS DECIMAL) AS "res" FROM "default"."T" AS "T";

--#[cast-08]
SELECT CAST(1 AS DECIMAL(5)) AS "res" FROM "default"."T" AS "T";

--#[cast-09]
SELECT CAST(1 AS DECIMAL(5,2)) AS "res" FROM "default"."T" AS "T";

--#[cast-10]
SELECT CAST(1 AS DECIMAL) AS "res" FROM "default"."T" AS "T";

--#[cast-11]
SELECT CAST(1 AS DECIMAL(5)) AS "res" FROM "default"."T" AS "T";

--#[cast-12]
SELECT CAST(1 AS DECIMAL(5,2)) AS "res" FROM "default"."T" AS "T";

--#[cast-13]
SELECT CAST(1 AS DECIMAL) AS "res" FROM "default"."T" AS "T";

--#[cast-14]
SELECT CAST(1 AS DECIMAL(5)) AS "res" FROM "default"."T" AS "T";

--#[cast-15]
SELECT CAST(1 AS DECIMAL(5,2)) AS "res" FROM "default"."T" AS "T";

--#[cast-16]
SELECT CAST(1 AS BIGINT) AS "res" FROM "default"."T" AS "T";

--#[cast-17]
SELECT CAST(1 AS BIGINT) AS "res" FROM "default"."T" AS "T";

--#[cast-18]
SELECT CAST(1 AS BIGINT) AS "res" FROM "default"."T" AS "T";

--#[cast-19]
SELECT CAST(1 AS INT) AS "res" FROM "default"."T" AS "T";

--#[cast-20]
SELECT CAST(1 AS INT) AS "res" FROM "default"."T" AS "T";

--#[cast-21]
SELECT CAST(1 AS INT) AS "res" FROM "default"."T" AS "T";

--#[cast-22]
SELECT CAST(1 AS INT) AS "res" FROM "default"."T" AS "T";

--#[cast-23]
SELECT CAST(1 AS SMALLINT) AS "res" FROM "default"."T" AS "T";

--#[cast-24]
SELECT CAST(1 AS SMALLINT) AS "res" FROM "default"."T" AS "T";

--#[cast-25]
SELECT CAST(1 AS SMALLINT) AS "res" FROM "default"."T" AS "T";

--#[cast-26]
SELECT CAST(1 AS TINYINT) AS "res" FROM "default"."T" AS "T";

-- <numeric type> - <approximate numeric type>
--#[cast-27]
SELECT CAST(1 AS REAL) AS "res" FROM "default"."T" AS "T";

--#[cast-28]
SELECT CAST(1 AS REAL) AS "res" FROM "default"."T" AS "T";

-- --#[cast-29] TODO(duckdb)
SELECT CAST(1 AS DOUBLE) AS "res" FROM "default"."T" AS "T";

-- <boolean type>
-- --#[cast-30] TODO(duckdb)
SELECT CAST(true AS BOOLEAN) AS "res" FROM "default"."T" AS "T";

-- --#[cast-31] TODO(duckdb)
SELECT CAST(true AS BOOLEAN) AS "res" FROM "default"."T" AS "T";

-- <datetime type>
--#[cast-32]
SELECT CAST("T"."timestamp_1" AS DATE) AS "res" FROM "default"."T" AS "T";

--#[cast-33]
SELECT CAST("T"."timestamp_1" AS TIME) AS "res" FROM "default"."T" AS "T";

-- --#[cast-34] TODO(duckdb)
SELECT CAST("T"."timestamp_1" AS TIME) AS "res" FROM "default"."T" AS "T";

--#[cast-35]
SELECT CAST("T"."timestamp_1" AS TIME WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

-- --#[cast-36] TODO(duckdb)
SELECT CAST("T"."timestamp_1" AS TIME WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

--#[cast-37]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP) AS "res" FROM "default"."T" AS "T";

-- --#[cast-38] TODO(duckdb)
SELECT CAST("T"."timestamp_1" AS TIMESTAMP) AS "res" FROM "default"."T" AS "T";

--#[cast-39]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

-- --#[cast-40] TODO(duckdb)
SELECT CAST("T"."timestamp_1" AS TIMESTAMP WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

-- INTERVAL YEAR-MONTH
-- --#[cast-41] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-42] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-43] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-44] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-45] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- DuckDB does not support precision on datetime fields
-- --#[cast-46] TODO(duckdb)
SELECT CAST("T"."col_y2mon" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- INTERVAL DAY-TIME
-- --#[cast-47] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-48] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-49] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-50] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-51] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-52] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-53] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-54] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-55] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-56] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-57] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-58] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-59] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-60] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- DuckDB does not support precision on datetime fields
-- --#[cast-61] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- DuckDB does not support precision on datetime fields
-- --#[cast-62] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- DuckDB does not support precision on datetime fields
-- --#[cast-63] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-64] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-65] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-66] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-67] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-68] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-69] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-70] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-71] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-72] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- --#[cast-73] TODO(duckdb)
SELECT CAST("T"."col_d2s" AS INTERVAL) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Test precision during cast, transcribed result should preserve previous precision if possible

-- --#[cast-74] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-75] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-76] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-77] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-78] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-79] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-80] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[cast-81] TODO(duckdb)
SELECT CAST(TIMESTAMP '2023-01-15 12:30:45' AS TIMESTAMP WITH TIME ZONE) > TIMESTAMPTZ '2023-01-15 10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
