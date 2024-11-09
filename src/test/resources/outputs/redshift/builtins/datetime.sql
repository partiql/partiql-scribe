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

--#[datetime-24]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) > DATEADD(DAY, -180, TIMESTAMP '2023-10-19 12:34:56');
