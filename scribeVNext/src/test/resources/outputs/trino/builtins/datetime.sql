--#[datetime-08]
SELECT current_date AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT date_add('second', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT date_add('minute', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT date_add('hour', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT date_add('day', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT date_add('month', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT date_add('year', 5, current_date) AS "_1" FROM "default"."T" AS "T";

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
SELECT date_add('second', 1, TIMESTAMP '2017-01-02 03:04:05.006') AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT date_diff('second', TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";
