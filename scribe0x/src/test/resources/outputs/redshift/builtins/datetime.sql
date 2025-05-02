-- NOTE: Redshift does not support top-level expression syntax.

--#[datetime-08]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT DATEADD(SECOND, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT DATEADD(MINUTE, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT DATEADD(HOUR, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT DATEADD(DAY, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT DATEADD(MONTH, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT DATEADD(YEAR, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT DATEDIFF(YEAR, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-16]
SELECT DATEDIFF(MONTH, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-17]
SELECT DATEDIFF(DAY, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-18]
SELECT DATEDIFF(HOUR, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-19]
SELECT DATEDIFF(MINUTE, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-20]
SELECT DATEDIFF(SECOND, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-21]
SELECT DATEADD(SECOND, 1, TIMESTAMP '2017-01-02 03:04:05.006') AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT DATEDIFF(SECOND, TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";

-- Explicit casts for comparison/equality with a datetime path
--#[datetime-24]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) > TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-25]
SELECT "t"."foo" FROM "default"."datatypes"."T_DATE" AS "t" WHERE DATE '2023-10-19' < CAST("t"."foo"."keep" AS DATE);

--#[datetime-26]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIME" AS "t" WHERE CAST("t"."foo"."keep" AS TIME) <= TIME '12:34:56';

--#[datetime-27]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-28]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) <> TIMESTAMP '2023-10-19 12:34:56';

-- Explicit casts for EXTRACT with a datetime path
-- TODO EXTRACT not yet supported in AST->PLAN conversion in v0.14.9
-- --#[datetime-29]
-- SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_TIMESTAMP AS t;
--
-- --#[datetime-30]
-- SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_DATE AS t;
--
-- --#[datetime-31]
-- SELECT EXTRACT(HOUR FROM t.foo.keep) FROM datatypes.T_TIME AS t;

-- Explicit casts for BETWEEN with a datetime path
--#[datetime-32]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-33]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE TIMESTAMP '2023-10-19 12:34:56' BETWEEN CAST("t"."foo"."keep" AS TIMESTAMP) AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-34]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE TIMESTAMP '2023-12-19 12:34:56' BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND CAST("t"."foo"."keep" AS TIMESTAMP);

-- Explicit casts for NULLIF with a datetime path
--#[datetime-35]
SELECT NULLIF(CAST("t"."foo"."keep" AS TIMESTAMP), TIMESTAMP '2023-12-19 12:34:56') AS "_1" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-36]
SELECT NULLIF(TIMESTAMP '2023-12-19 12:34:56', CAST("t"."foo"."keep" AS TIMESTAMP)) AS "_1" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- Explicit casts for CASE-WHEN with a datetime path
--#[datetime-37]
SELECT CASE WHEN CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-12-19 12:34:56' THEN CAST("t"."foo"."keep" AS TIMESTAMP) ELSE sysdate END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-38]
SELECT CASE WHEN CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-12-19 12:34:56' THEN TIMESTAMP '2023-12-19 12:34:56' ELSE CAST("t"."foo"."keep" AS TIMESTAMP) END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-39]
SELECT CASE WHEN TIMESTAMP '2023-12-19 12:34:56' = CAST("t"."foo"."keep" AS TIMESTAMP) THEN TIMESTAMP '2023-12-19 12:34:56' ELSE CAST("t"."foo"."keep" AS TIMESTAMP) END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- Explicit cast for top-level timestamp in comparison
--#[datetime-40]
SELECT "T"."timestamp_2", CAST("T"."timestamp_1" AS TIMESTAMP) < CAST("T"."timestamp_2" AS TIMESTAMP) AS "result" FROM "default"."T" AS "T" WHERE CAST("T"."timestamp_1" AS TIMESTAMP) > TIMESTAMP '2023-12-19 12:34:56';
