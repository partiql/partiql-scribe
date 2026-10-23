-- --#[datediff-day-00] TODO(redshift)
SELECT DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-day-01] TODO(redshift)
SELECT DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['col_timestamp']) - DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-day-02] TODO(redshift)
SELECT DATE_DIFF(DAY, "t"['col_date'], "t"['col_date']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-year-00] TODO(redshift)
SELECT DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', "t"['col_timestamp']) - DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-month-00] TODO(redshift)
SELECT DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(MONTH, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-hour-00] TODO(redshift)
SELECT DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-minute-00] TODO(redshift)
SELECT DATE_DIFF(MINUTE, TIMESTAMP '0001-01-01 00:00:00', "t"['col_timestamp']) - DATE_DIFF(MINUTE, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-second-00] TODO(redshift)
SELECT DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-millisecond-00] TODO(redshift)
SELECT ((DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) * CAST(1000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['timestamp_2']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT))) - ((DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) * CAST(1000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT))) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-microsecond-00] TODO(redshift)
SELECT ((DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) * CAST(1000000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['timestamp_2']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000 AS BIGINT))) - ((DATE_DIFF(SECOND, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) * CAST(1000000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000 AS BIGINT))) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-time-00] TODO(redshift)
SELECT DATE_DIFF(HOUR, TIME '00:00:00', TIME '15:24:45') - DATE_DIFF(HOUR, TIME '00:00:00', "t"['col_time']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-time-01] TODO(redshift)
SELECT ((DATE_DIFF(SECOND, TIME '00:00:00', "t"['col_time']) * CAST(1000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['col_time']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT))) - ((DATE_DIFF(SECOND, TIME '00:00:00', "t"['col_time']) * CAST(1000 AS BIGINT)) + ((CAST(EXTRACT(SECOND FROM "t"['col_time']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-timetz-00] TODO(redshift)
SELECT DATE_DIFF(HOUR, TIME '00:00:00', CAST("t"['col_timez'] AS TIME (6)) - (CAST(CAST("t"['col_timez'] AS TIME (6)) AS TIME (6) WITH TIME ZONE) - "t"['col_timez'])) - DATE_DIFF(HOUR, TIME '00:00:00', CAST(TIME WITH TIME ZONE '20:00:00-08:00' AS TIME (6)) - (CAST(CAST(TIME WITH TIME ZONE '20:00:00-08:00' AS TIME (6)) AS TIME (6) WITH TIME ZONE) - TIME WITH TIME ZONE '20:00:00-08:00')) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-quoted-00] TODO(redshift)
SELECT DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-string-00] TODO(redshift)
SELECT DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2023-01-03 05:04:03') - DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2023-01-01 00:00:00') AS "v" FROM "default"."T" AS "t";

-- --#[datediff-string-01] TODO(redshift)
SELECT DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-01-04 00:00:00') - DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[datediff-string-02] TODO(redshift)
[UNSUPPORTED{message="character string '2009-01-01 10:00:00' with a time of day next to a DATE argument"}];

-- --#[datediff-string-03] TODO(redshift)
[UNSUPPORTED{message="character string 'Jun 1,2008 09:59:59 EST' with a time zone as a TIMESTAMP"}];

-- --#[datediff-string-04] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF with a character string argument that is not a literal"}];

-- --#[datediff-nested-00] TODO(redshift)
SELECT DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', DATE_ADD(DAY, 1, "t"['timestamp_1'])) AS "v" FROM "default"."T" AS "t";

-- --#[datediff-quarter-00] TODO(redshift)
[UNSUPPORTED{message="datepart `qtr` in DATEDIFF"}];

-- --#[datediff-week-00] TODO(redshift)
[UNSUPPORTED{message="datepart `week` in DATEDIFF"}];

-- --#[datediff-decade-00] TODO(redshift)
[UNSUPPORTED{message="datepart `decade` in DATEDIFF"}];

-- --#[datediff-dow-00] TODO(redshift)
[UNSUPPORTED{message="datepart `dow` in DATEDIFF"}];

-- --#[datediff-timestamptz-00] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF with a TIMESTAMPTZ argument"}];

-- --#[datediff-time-day-00] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF datepart `day` of a TIME value"}];

-- --#[datediff-mixed-00] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF of TIME and TIMESTAMP values"}];

-- --#[datediff-super-00] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF with an argument of unknown type"}];
