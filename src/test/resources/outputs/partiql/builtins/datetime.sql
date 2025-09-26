--#[datetime-00]
CURRENT_DATE;

--#[datetime-01]
INTERVAL '5' SECOND (9, 0) + TIME '12:34:56';

--#[datetime-02]
INTERVAL '5' MINUTE (9) + TIME '12:34:56';

--#[datetime-03]
INTERVAL '5' HOUR (9) + TIME '12:34:56';

--#[datetime-04]
INTERVAL '5' DAY (9) + CURRENT_DATE;

--#[datetime-05]
INTERVAL '5' MONTH (9) + CURRENT_DATE;

--#[datetime-06]
INTERVAL '5' YEAR (9) + CURRENT_DATE;

--#[datetime-07]
DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE);

--#[datetime-08]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT INTERVAL '5' SECOND (9, 0) + "T"['timestamp_1'] AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT INTERVAL '5' MINUTE (9) + "T"['timestamp_1'] AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT INTERVAL '5' HOUR (9) + "T"['timestamp_1'] AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT INTERVAL '5' DAY (9) + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT INTERVAL '5' MONTH (9) + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT INTERVAL '5' YEAR (9) + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT DATE_DIFF(YEAR, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-16]
SELECT DATE_DIFF(MONTH, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-17]
SELECT DATE_DIFF(DAY, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-18]
SELECT DATE_DIFF(HOUR, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-19]
SELECT DATE_DIFF(MINUTE, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-20]
SELECT DATE_DIFF(SECOND, "T"['timestamp_1'], "T"['timestamp_2']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-21]
SELECT INTERVAL '1' SECOND (9, 0) + TIMESTAMP '2017-01-02 03:04:05.006' AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT DATE_DIFF(SECOND, TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";
