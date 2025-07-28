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

--#[cast-06]
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

--#[cast-29]
SELECT CAST(1 AS DOUBLE) AS "res" FROM "default"."T" AS "T";

-- <boolean type>
--#[cast-30]
SELECT CAST(true AS BOOLEAN) AS "res" FROM "default"."T" AS "T";

--#[cast-31]
SELECT CAST(true AS BOOLEAN) AS "res" FROM "default"."T" AS "T";

-- <datetime type>
--#[cast-32]
SELECT CAST("T"."timestamp_1" AS DATE) AS "res" FROM "default"."T" AS "T";

--#[cast-33]
SELECT CAST("T"."timestamp_1" AS TIME) AS "res" FROM "default"."T" AS "T";

--#[cast-34]
SELECT CAST("T"."timestamp_1" AS TIME (6)) AS "res" FROM "default"."T" AS "T";

--#[cast-35]
SELECT CAST("T"."timestamp_1" AS TIME WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

--#[cast-36]
SELECT CAST("T"."timestamp_1" AS TIME (6) WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

--#[cast-37]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP) AS "res" FROM "default"."T" AS "T";

--#[cast-38]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP (6)) AS "res" FROM "default"."T" AS "T";

--#[cast-39]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

--#[cast-40]
SELECT CAST("T"."timestamp_1" AS TIMESTAMP (6) WITH TIME ZONE) AS "res" FROM "default"."T" AS "T";

-- INTERVAL YEAR-MONTH
-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-41]
-- SELECT CAST("T"."col_y2mon" AS INTERVAL YEAR) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-42]
-- SELECT CAST("T"."col_y2mon" AS INTERVAL YEAR (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-43]
-- SELECT CAST("T"."col_y2mon" AS INTERVAL MONTH) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-44]
-- SELECT CAST("T"."col_y2mon" AS INTERVAL MONTH (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

--#[cast-45]
SELECT CAST("T"."col_y2mon" AS INTERVAL YEAR TO MONTH) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino does not support precision on datetime fields
--#[cast-46]
SELECT CAST("T"."col_y2mon" AS INTERVAL YEAR TO MONTH) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- INTERVAL DAY-TIME
-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-47]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-48]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-49]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-50]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-51]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-52]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-53]
-- SELECT CAST("T"."col_d2s" AS INTERVAL SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-54]
-- SELECT CAST("T"."col_d2s" AS INTERVAL SECOND (2)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-55]
-- SELECT CAST("T"."col_d2s" AS INTERVAL SECOND (2, 3)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-56]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO HOUR) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-57]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY (2) TO HOUR) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-58]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO MINUTE) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-59]
-- SELECT CAST("T"."col_d2s" AS INTERVAL DAY (2) TO MINUTE) AS "res" FROM "default"."T_INTERVALS" AS "T";

--#[cast-60]
SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino does not support precision on datetime fields
--#[cast-61]
SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino does not support precision on datetime fields
--#[cast-62]
SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino does not support precision on datetime fields
--#[cast-63]
SELECT CAST("T"."col_d2s" AS INTERVAL DAY TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-64]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR TO MINUTE) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-65]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR (2) TO MINUTE) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-66]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-67]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR (2) TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-68]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR TO SECOND (3)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-69]
-- SELECT CAST("T"."col_d2s" AS INTERVAL HOUR (2) TO SECOND (3)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-70]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-71]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE (2) TO SECOND) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-72]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE TO SECOND (3)) AS "res" FROM "default"."T_INTERVALS" AS "T";

-- Trino only supports casting to YEAR-MONTH and DAY-SECOND intervals
-- --#[cast-73]
-- SELECT CAST("T"."col_d2s" AS INTERVAL MINUTE (2) TO SECOND (3)) AS "res" FROM "default"."T_INTERVALS" AS "T";
