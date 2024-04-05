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
SELECT DATEDIFF(YEAR, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-16]
SELECT DATEDIFF(MONTH, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-17]
SELECT DATEDIFF(DAY, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-18]
SELECT DATEDIFF(HOUR, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-19]
SELECT DATEDIFF(MINUTE, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-20]
SELECT DATEDIFF(SECOND, "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";
