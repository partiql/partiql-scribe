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

--#[datetime-41]
SELECT date_diff('second', "T"."col_time", "T"."col_time") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-42]
SELECT date_diff('second', CAST("T"."col_time" AS TIME (6)), "T"."col_timez") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-43]
SELECT date_diff('second', "T"."col_timez", CAST("T"."col_time" AS TIME (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-44]
SELECT date_diff('second', "T"."col_timez", "T"."col_timez") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-45]
SELECT date_diff('day', "T"."col_date", "T"."col_date") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-46]
SELECT date_diff('day', CAST("T"."col_date" AS TIMESTAMP (6)), "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-47]
SELECT date_diff('day', CAST("T"."col_date" AS TIMESTAMP), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-48]
SELECT date_diff('day', "T"."col_timestamp", CAST("T"."col_date" AS TIMESTAMP (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-49]
SELECT date_diff('day', "T"."col_timestamp", "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-50]
SELECT date_diff('day', CAST("T"."col_timestamp" AS TIMESTAMP), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-51]
SELECT date_diff('day', "T"."col_timestampz", CAST("T"."col_date" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-52]
SELECT date_diff('day', "T"."col_timestampz", CAST("T"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-53]
SELECT date_diff('day', "T"."col_timestampz", "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-54]
SELECT date_diff('second', TIME '12:34:56', TIME '13:45:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-55]
SELECT date_diff('second', CAST(TIME '12:34:56' AS TIME (6)), TIME '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-56]
SELECT date_diff('second', TIME '12:34:56+08:00', CAST(TIME '13:45:00' AS TIME (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-57]
SELECT date_diff('second', TIME '12:34:56+08:00', TIME '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-58]
SELECT date_diff('day', DATE '2023-01-15', DATE '2023-12-25') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-59]
SELECT date_diff('day', CAST(DATE '2023-01-15' AS TIMESTAMP (6)), TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-60]
SELECT date_diff('day', CAST(DATE '2023-01-15' AS TIMESTAMP), TIMESTAMP '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-61]
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00', CAST(DATE '2023-12-25' AS TIMESTAMP (6))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-62]
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-63]
SELECT date_diff('day', CAST(TIMESTAMP '2023-01-15 08:00:00' AS TIMESTAMP), TIMESTAMP '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-64]
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00+08:00', CAST(DATE '2023-12-25' AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-65]
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00+08:00', CAST(TIMESTAMP '2023-12-25 10:30:00' AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-66]
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00+08:00', TIMESTAMP '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
