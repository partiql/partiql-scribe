-- --#[dateadd-year-00] TODO(redshift)
SELECT DATE_ADD(DAY, EXTRACT(DAY FROM "t"['timestamp_1']) - EXTRACT(DAY FROM DATE_ADD(YEAR, 1, "t"['timestamp_1'])), DATE_ADD(YEAR, 1, "t"['timestamp_1'])) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-year-01] TODO(redshift)
SELECT DATE_ADD(DAY, EXTRACT(DAY FROM CAST("t"['col_date'] AS TIMESTAMP)) - EXTRACT(DAY FROM DATE_ADD(YEAR, "t"['col_int32'], CAST("t"['col_date'] AS TIMESTAMP))), DATE_ADD(YEAR, "t"['col_int32'], CAST("t"['col_date'] AS TIMESTAMP))) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-month-00] TODO(redshift)
SELECT DATE_ADD(MONTH, 18, CAST("t"['col_date'] AS TIMESTAMP)) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-day-00] TODO(redshift)
SELECT DATE_ADD(DAY, 30, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-minute-00] TODO(redshift)
SELECT DATE_ADD(MINUTE, 30, CAST("t"['col_date'] AS TIMESTAMP)) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-week-00] TODO(redshift)
SELECT DATE_ADD(DAY, 7 * 2, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-week-01] TODO(redshift)
SELECT DATE_ADD(DAY, 7 * "t"['col_int32'], CAST("t"['col_date'] AS TIMESTAMP)) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-millisecond-00] TODO(redshift)
SELECT "t"['timestamp_1'] + (INTERVAL '0.001' SECOND (9, 3) * 1500) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-microsecond-00] TODO(redshift)
SELECT CAST("t"['col_date'] AS TIMESTAMP) + (INTERVAL '0.000001' SECOND (9, 6) * "t"['col_int64']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-time-00] TODO(redshift)
SELECT DATE_ADD(HOUR, 12, TIME '13:24:55') AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-time-01] TODO(redshift)
SELECT "t"['col_time'] + (INTERVAL '0.001' SECOND (9, 3) * 5) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-timetz-00] TODO(redshift)
SELECT CAST(DATE_ADD(HOUR, 2, CAST("t"['col_timez'] AS TIME (6)) - (CAST(CAST("t"['col_timez'] AS TIME (6)) AS TIME (6) WITH TIME ZONE) - "t"['col_timez'])) AS TIME (6) WITH TIME ZONE) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[dateadd-timetz-01] TODO(redshift)
SELECT CAST((CAST(TIME WITH TIME ZONE '13:24:55-08:00' AS TIME (6)) - (CAST(CAST(TIME WITH TIME ZONE '13:24:55-08:00' AS TIME (6)) AS TIME (6) WITH TIME ZONE) - TIME WITH TIME ZONE '13:24:55-08:00')) + (INTERVAL '0.000001' SECOND (9, 6) * 5) AS TIME (6) WITH TIME ZONE) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-quoted-00] TODO(redshift)
SELECT DATE_ADD(DAY, 1, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-string-00] TODO(redshift)
SELECT DATE_ADD(MONTH, 18, TIMESTAMP '2008-02-28 00:00:00') AS "v" FROM "default"."T" AS "t";

-- --#[dateadd-string-01] TODO(redshift)
[UNSUPPORTED{message="special datetime value 'today'"}];

-- --#[dateadd-getdate-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['timestamp_1'] > DATE_ADD(DAY, -30, DATE_ADD(SECOND, DATE_DIFF(SECOND, CAST(TIMESTAMP '0001-01-01 00:00:00' AS TIMESTAMP WITH TIME ZONE), "utcnow"()), TIMESTAMP '0001-01-01 00:00:00'));

-- --#[dateadd-quarter-00] TODO(redshift)
[UNSUPPORTED{message="datepart `quarter` in DATEADD"}];

-- --#[dateadd-century-00] TODO(redshift)
[UNSUPPORTED{message="datepart `century` in DATEADD"}];

-- --#[dateadd-dow-00] TODO(redshift)
[UNSUPPORTED{message="datepart `dow` in DATEADD"}];

-- --#[dateadd-timestamptz-00] TODO(redshift)
[UNSUPPORTED{message="DATEADD with a TIMESTAMPTZ argument"}];

-- --#[dateadd-interval-00] TODO(redshift)
[UNSUPPORTED{message="DATEADD with a non-integer interval"}];

-- --#[dateadd-time-day-00] TODO(redshift)
[UNSUPPORTED{message="DATEADD datepart `day` of a TIME value"}];

-- --#[dateadd-synonym-00] TODO(redshift)
[UNSUPPORTED{message="function `DATE_ADD`"}];
