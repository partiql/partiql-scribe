--#[datetime-08]
SELECT current_date AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT INTERVAL '5' SECOND + "T"."timestamp_1" AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT INTERVAL '5' MINUTE + "T"."timestamp_1" AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT INTERVAL '5' HOUR + "T"."timestamp_1" AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT INTERVAL '5' DAY + current_date AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT INTERVAL '5' MONTH + current_date AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT INTERVAL '5' YEAR + current_date AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT date_diff('year', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-16]
SELECT date_diff('month', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-17]
SELECT date_diff('day', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-18]
SELECT date_diff('hour', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-19]
SELECT date_diff('minute', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-20]
SELECT date_diff('second', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-21]
SELECT INTERVAL '1' SECOND + TIMESTAMP '2017-01-02 03:04:05.006' AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT date_diff('second', TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";
