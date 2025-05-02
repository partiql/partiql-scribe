--#[datetime-00]
CURRENT_DATE;

--#[datetime-01]
DATE_ADD(SECOND, 5, TIME '12:34:56');

--#[datetime-02]
DATE_ADD(MINUTE, 5, TIME '12:34:56');

--#[datetime-03]
DATE_ADD(HOUR, 5, TIME '12:34:56');

--#[datetime-04]
DATE_ADD(DAY, 5, CURRENT_DATE);

--#[datetime-05]
DATE_ADD(MONTH, 5, CURRENT_DATE);

--#[datetime-06]
DATE_ADD(YEAR, 5, CURRENT_DATE);

--#[datetime-07]
DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE);

--#[datetime-08]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT DATE_ADD(SECOND, 5, "T"['timestamp_1']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT DATE_ADD(MINUTE, 5, "T"['timestamp_1']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT DATE_ADD(HOUR, 5, "T"['timestamp_1']) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT DATE_ADD(DAY, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT DATE_ADD(MONTH, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT DATE_ADD(YEAR, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

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
SELECT DATE_ADD(SECOND, 1, TIMESTAMP '2017-01-02 03:04:05.006') AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT DATE_DIFF(SECOND, TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";
