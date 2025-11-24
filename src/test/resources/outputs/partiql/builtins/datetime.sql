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

--#[datetime-41]
SELECT DATE_DIFF(SECOND, "T"['col_time'], "T"['col_time']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-42]
SELECT DATE_DIFF(SECOND, CAST("T"['col_time'] AS TIME (6) WITH TIME ZONE), "T"['col_timez']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-43]
SELECT DATE_DIFF(SECOND, "T"['col_timez'], CAST("T"['col_time'] AS TIME (6) WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-44]
SELECT DATE_DIFF(SECOND, "T"['col_timez'], "T"['col_timez']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-45]
SELECT DATE_DIFF(DAY, "T"['col_date'], "T"['col_date']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-46]
SELECT DATE_DIFF(DAY, CAST("T"['col_date'] AS TIMESTAMP (6)), "T"['col_timestamp']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-47]
SELECT DATE_DIFF(DAY, CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE), "T"['col_timestampz']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-48]
SELECT DATE_DIFF(DAY, "T"['col_timestamp'], CAST("T"['col_date'] AS TIMESTAMP (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-49]
SELECT DATE_DIFF(DAY, "T"['col_timestamp'], "T"['col_timestamp']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-50]
SELECT DATE_DIFF(DAY, CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE), "T"['col_timestampz']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-51]
SELECT DATE_DIFF(DAY, "T"['col_timestampz'], CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-52]
SELECT DATE_DIFF(DAY, "T"['col_timestampz'], CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-53]
SELECT DATE_DIFF(DAY, "T"['col_timestampz'], "T"['col_timestampz']) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-54]
SELECT DATE_DIFF(SECOND, TIME '12:34:56', TIME '13:45:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-55]
SELECT DATE_DIFF(SECOND, CAST(TIME '12:34:56' AS TIME (6) WITH TIME ZONE), TIME WITH TIME ZONE '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-56]
SELECT DATE_DIFF(SECOND, TIME WITH TIME ZONE '12:34:56+08:00', CAST(TIME '13:45:00' AS TIME (6) WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-57]
SELECT DATE_DIFF(SECOND, TIME WITH TIME ZONE '12:34:56+08:00', TIME WITH TIME ZONE '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-58]
SELECT DATE_DIFF(DAY, DATE '2023-01-15', DATE '2023-12-25') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-59]
SELECT DATE_DIFF(DAY, CAST(DATE '2023-01-15' AS TIMESTAMP (6)), TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-60]
SELECT DATE_DIFF(DAY, CAST(DATE '2023-01-15' AS TIMESTAMP WITH TIME ZONE), TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-61]
SELECT DATE_DIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', CAST(DATE '2023-12-25' AS TIMESTAMP (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-62]
SELECT DATE_DIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-63]
SELECT DATE_DIFF(DAY, CAST(TIMESTAMP '2023-01-15 08:00:00' AS TIMESTAMP WITH TIME ZONE), TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-64]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', CAST(DATE '2023-12-25' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-65]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', CAST(TIMESTAMP '2023-12-25 10:30:00' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-66]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
