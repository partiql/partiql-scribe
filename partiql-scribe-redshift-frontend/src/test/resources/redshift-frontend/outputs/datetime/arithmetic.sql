--#[date-plus-integer-00]
SELECT DATE_ADD(DAY, 1, "t"['col_date']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-integer-01]
SELECT DATE_ADD(DAY, -"t"['col_int32'], "t"['col_date']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-integer-02]
SELECT DATE_ADD(DAY, 1, "t"['col_date']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[timestamp-minus-number-00]
[UNSUPPORTED{message="TIMESTAMP - number"}];

--#[date-plus-decimal-00]
[UNSUPPORTED{message="DATE + number"}];

--#[date-minus-date-00]
SELECT CAST(DATE_DIFF(DAY, DATE '2020-01-01', "t"['col_date']) AS INT) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-minus-date-01]
SELECT CAST(DATE_DIFF(DAY, "t"['col_date'], DATE_ADD(DAY, 1, "t"['col_date'])) AS INT) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[timestamp-minus-timestamp-00]
SELECT "t"['timestamp_2'] - "t"['timestamp_1'] AS "v" FROM "default"."T" AS "t";

--#[timestamp-minus-timestamp-01]
SELECT "t"['col_timestampz'] - CAST("t"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[time-minus-time-00]
SELECT "t"['col_time'] - "t"['col_time'] AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[timetz-minus-timetz-00]
[UNSUPPORTED{message="TIMETZ - TIMETZ"}];

--#[timestamp-plus-interval-00]
SELECT "t"['timestamp_1'] + INTERVAL '1' DAY (9) AS "v" FROM "default"."T" AS "t";

--#[timestamp-plus-interval-01]
SELECT "t"['col_timestamp'] + ("t"['col_d2s'] * 3) AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[timestamp-plus-interval-02]
SELECT "t"['col_y2mon'] + "t"['col_timestampz'] AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[date-plus-interval-00]
SELECT "t"['col_date'] - INTERVAL '1' MONTH (9) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-interval-01]
SELECT CAST("t"['col_date'] AS TIMESTAMP) + "t"['col_h'] AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[date-plus-interval-02]
SELECT CAST("t"['col_date'] AS TIMESTAMP) + INTERVAL '3:35' HOUR (9) TO MINUTE AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-interval-03]
SELECT CAST("t"['col_date'] AS TIMESTAMP) - INTERVAL '4' MONTH (9) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[time-plus-interval-00]
SELECT "t"['col_time'] + "t"['col_d2s'] AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[time-plus-interval-01]
[UNSUPPORTED{message="TIME + INTERVAL YEAR TO MONTH"}];

--#[timetz-plus-interval-00]
SELECT CAST((CAST("t"['col_timez'] AS TIME (6)) - (CAST(CAST("t"['col_timez'] AS TIME (6)) AS TIME (6) WITH TIME ZONE) - "t"['col_timez'])) + "t"['col_h'] AS TIME (6) WITH TIME ZONE) AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[date-plus-time-00]
SELECT CAST("t"['col_date'] AS TIMESTAMP) + ("t"['col_time'] - TIME '00:00:00') AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-time-01]
SELECT CAST("t"['col_date'] AS TIMESTAMP) + ("t"['col_time'] - TIME '00:00:00') AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-plus-timetz-00]
SELECT CAST(CAST("t"['col_date'] AS TIMESTAMP) + (CAST("t"['col_timez'] AS TIME (6)) - TIME '00:00:00') AS TIMESTAMP (6) WITH TIME ZONE) - (CAST(CAST("t"['col_timez'] AS TIME (6)) AS TIME (6) WITH TIME ZONE) - "t"['col_timez']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[interval-plus-interval-00]
SELECT "t"['col_d'] + "t"['col_h'] AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[interval-plus-interval-01]
[UNSUPPORTED{message="`+` of a YEAR TO MONTH interval and a DAY TO SECOND interval"}];

--#[date-plus-string-00]
[UNSUPPORTED{message="DATE + character string"}];
