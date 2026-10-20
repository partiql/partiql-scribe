--#[date-trunc-00]
SELECT DATE_ADD(MONTH, DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']), TIMESTAMP '0001-01-01 00:00:00') AS "v" FROM "default"."T" AS "t";

--#[date-trunc-01]
SELECT DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) / CAST(7 AS BIGINT)) * CAST(7 AS BIGINT), TIMESTAMP '0001-01-01 00:00:00') AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-trunc-02]
SELECT DATE_ADD(MONTH, (DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', "t"['col_timestamp']) / CAST(3 AS BIGINT)) * CAST(3 AS BIGINT), TIMESTAMP '0001-01-01 00:00:00') AS "q", DATE_ADD(YEAR, (DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', "t"['col_timestamp']) / CAST(1000 AS BIGINT)) * CAST(1000 AS BIGINT), TIMESTAMP '0001-01-01 00:00:00') AS "m" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-trunc-03]
SELECT DATE_ADD(YEAR, (DATE_DIFF(YEAR, TIMESTAMP '0000-01-01 00:00:00', "t"['timestamp_1']) / CAST(10 AS BIGINT)) * CAST(10 AS BIGINT), TIMESTAMP '0000-01-01 00:00:00') AS "d", DATE_ADD(YEAR, (DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) / CAST(100 AS BIGINT)) * CAST(100 AS BIGINT), TIMESTAMP '0001-01-01 00:00:00') AS "c" FROM "default"."T" AS "t";

--#[date-trunc-04]
SELECT DATE_ADD(SECOND, DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']), TIMESTAMP '0001-01-01 00:00:00') + (INTERVAL '0.001' SECOND (9, 3) * ((CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT))) AS "v" FROM "default"."T" AS "t";

--#[date-trunc-05]
SELECT DATE_ADD(DAY, DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-06-01 09:59:59'), TIMESTAMP '0001-01-01 00:00:00') AS "v" FROM "default"."T" AS "t";

--#[date-trunc-06]
[UNSUPPORTED{message="DATE_TRUNC with a TIMESTAMPTZ argument"}];

--#[date-trunc-07]
[UNSUPPORTED{message="DATE_TRUNC with a datepart that is not a character string literal"}];

--#[date-trunc-08]
[UNSUPPORTED{message="character string '6/1/2008 09:59:59' as a TIMESTAMP"}];

--#[last-day-00]
SELECT DATE_ADD(DAY, -1, DATE_ADD(MONTH, DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) + CAST(1 AS BIGINT), DATE '0001-01-01')) AS "v" FROM "default"."T" AS "t";

--#[last-day-01]
SELECT DATE_ADD(DAY, -1, DATE_ADD(MONTH, DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) + CAST(1 AS BIGINT), DATE '0001-01-01')) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[add-months-00]
SELECT CASE WHEN EXTRACT(DAY FROM DATE_ADD(DAY, 1, CAST("t"['col_date'] AS TIMESTAMP))) = 1 THEN DATE_ADD(DAY, -1, DATE_ADD(MONTH, 1, DATE_ADD(DAY, 1, CAST("t"['col_date'] AS TIMESTAMP)))) ELSE DATE_ADD(MONTH, 1, CAST("t"['col_date'] AS TIMESTAMP)) END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[add-months-01]
SELECT CASE WHEN EXTRACT(DAY FROM DATE_ADD(DAY, 1, "t"['timestamp_1'])) = 1 THEN DATE_ADD(DAY, -1, DATE_ADD(MONTH, -2, DATE_ADD(DAY, 1, "t"['timestamp_1']))) ELSE DATE_ADD(MONTH, -2, "t"['timestamp_1']) END AS "v" FROM "default"."T" AS "t";

--#[add-months-02]
[UNSUPPORTED{message="ADD_MONTHS with a TIMESTAMPTZ argument"}];

--#[add-months-03]
[UNSUPPORTED{message="ADD_MONTHS with a non-integer number of months"}];

--#[next-day-00]
SELECT DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) + ((CAST(7 AS BIGINT) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) % CAST(7 AS BIGINT))) % CAST(7 AS BIGINT))) + CAST(1 AS BIGINT), DATE '0001-01-01') AS "v" FROM "default"."T" AS "t";

--#[next-day-01]
SELECT DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) + ((CAST(12 AS BIGINT) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) % CAST(7 AS BIGINT))) % CAST(7 AS BIGINT))) + CAST(1 AS BIGINT), DATE '0001-01-01') AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[next-day-02]
[UNSUPPORTED{message="NEXT_DAY with a day name that is not a literal"}];

--#[next-day-03]
[UNSUPPORTED{message="NEXT_DAY day name 'Tomorrow'"}];

--#[date-cmp-00]
SELECT CASE WHEN "t"['col_date'] < DATE '2008-01-04' THEN -1 WHEN "t"['col_date'] > DATE '2008-01-04' THEN 1 WHEN "t"['col_date'] = DATE '2008-01-04' THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-cmp-01]
SELECT CASE WHEN "t"['col_date'] < DATE '2008-01-04' THEN -1 WHEN "t"['col_date'] > DATE '2008-01-04' THEN 1 WHEN "t"['col_date'] = DATE '2008-01-04' THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-cmp-02]
SELECT CASE WHEN CAST("t"['col_date'] AS TIMESTAMP WITH TIME ZONE) < "t"['col_timestampz'] THEN -1 WHEN CAST("t"['col_date'] AS TIMESTAMP WITH TIME ZONE) > "t"['col_timestampz'] THEN 1 WHEN "t"['col_date'] = "t"['col_timestampz'] THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-cmp-03]
[UNSUPPORTED{message="DATE_CMP with a TIMESTAMP argument"}];

--#[timestamp-cmp-00]
SELECT CASE WHEN "t"['timestamp_1'] < "t"['timestamp_2'] THEN -1 WHEN "t"['timestamp_1'] > "t"['timestamp_2'] THEN 1 WHEN "t"['timestamp_1'] = "t"['timestamp_2'] THEN 0 ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[timestamp-cmp-01]
SELECT CASE WHEN CAST("t"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) < "t"['col_timestampz'] THEN -1 WHEN CAST("t"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) > "t"['col_timestampz'] THEN 1 WHEN "t"['col_timestamp'] = "t"['col_timestampz'] THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[timestamptz-cmp-00]
SELECT CASE WHEN "t"['col_timestampz'] < CAST(DATE '2008-01-04' AS TIMESTAMP WITH TIME ZONE) THEN -1 WHEN "t"['col_timestampz'] > CAST(DATE '2008-01-04' AS TIMESTAMP WITH TIME ZONE) THEN 1 WHEN "t"['col_timestampz'] = DATE '2008-01-04' THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[convert-timezone-00]
SELECT "t"['timestamp_1'] AS "v" FROM "default"."T" AS "t";

--#[convert-timezone-01]
SELECT CAST("t"['col_date'] AS TIMESTAMP) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[convert-timezone-02]
SELECT TIMESTAMP '2008-08-21 07:23:54' AS "v" FROM "default"."T" AS "t";

--#[convert-timezone-03]
[UNSUPPORTED{message="CONVERT_TIMEZONE with time zone 'America/New_York'"}];

--#[convert-timezone-04]
[UNSUPPORTED{message="CONVERT_TIMEZONE with a time zone that is not a literal"}];

--#[at-time-zone-00]
SELECT CAST("t"['timestamp_1'] AS TIMESTAMP (6) WITH TIME ZONE) AS "v" FROM "default"."T" AS "t";

--#[at-time-zone-01]
SELECT CAST("t"['col_timestampz'] AS TIMESTAMP) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[at-time-zone-02]
[UNSUPPORTED{message="AT TIME ZONE 'PST'"}];

--#[at-time-zone-03]
[UNSUPPORTED{message="AT TIME ZONE with a DATE argument"}];

--#[timezone-00]
SELECT CAST("t"['timestamp_1'] AS TIMESTAMP (6) WITH TIME ZONE) AS "v" FROM "default"."T" AS "t";

--#[timezone-01]
SELECT CAST(TIMESTAMP '2008-06-17 09:44:54' AS TIMESTAMP (6) WITH TIME ZONE) AS "v" FROM "default"."T" AS "t";

--#[timezone-02]
[UNSUPPORTED{message="TIMEZONE('PST', ...)"}];

--#[months-between-00]
[UNSUPPORTED{message="function `MONTHS_BETWEEN`"}];

--#[to-timestamp-00]
[UNSUPPORTED{message="function `TO_TIMESTAMP`"}];

--#[to-char-00]
[UNSUPPORTED{message="function `TO_CHAR`"}];

--#[to-date-00]
[UNSUPPORTED{message="function `TO_DATE`"}];

--#[interval-cmp-00]
[UNSUPPORTED{message="function `INTERVAL_CMP`"}];
