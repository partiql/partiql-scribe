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
SELECT CAST('abc' AS VARCHAR(65535)) AS "res" FROM "default"."T" AS "T";

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

-- Redshift does not have a TINYINT
-- --#[cast-26]
-- SELECT CAST(1 AS TINYINT) AS "res" FROM "default"."T" AS "T";

-- <numeric type> - <approximate numeric type>
--#[cast-27]
SELECT CAST(1 AS REAL) AS "res" FROM "default"."T" AS "T";

--#[cast-28]
SELECT CAST(1 AS REAL) AS "res" FROM "default"."T" AS "T";

--#[cast-29]
SELECT CAST(1 AS DOUBLE PRECISION) AS "res" FROM "default"."T" AS "T";

-- <boolean type>
--#[cast-30]
SELECT CAST(true AS BOOL) AS "res" FROM "default"."T" AS "T";

--#[cast-31]
SELECT CAST(true AS BOOL) AS "res" FROM "default"."T" AS "T";

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
