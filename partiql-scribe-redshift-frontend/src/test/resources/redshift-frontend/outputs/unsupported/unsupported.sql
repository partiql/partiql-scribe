--#[ilike]
[UNSUPPORTED{message="ILIKE"}];

--#[unknown-function]
[UNSUPPORTED{message="function `MD5`"}];

--#[datepart-week]
SELECT DATE_ADD(DAY, 7 * 1, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[super-type]
[UNSUPPORTED{message="data type `super`"}];

--#[extract-week]
SELECT CAST((DATE_DIFF(DAY, DATE_ADD(YEAR, DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', CAST(DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01') AS TIMESTAMP (6))), DATE '0001-01-01'), DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01')) / CAST(7 AS BIGINT)) + CAST(1 AS BIGINT) AS INT) AS "w" FROM "default"."T" AS "t";

--#[extract-timezone-hour]
[UNSUPPORTED{message="EXTRACT datepart `timezone_hour`"}];

--#[extract-epoch]
[UNSUPPORTED{message="EXTRACT datepart `epoch`"}];

--#[timetz-zone-name]
[UNSUPPORTED{message="TIMETZ literal format '12:30:45 PST'"}];

--#[timestamp-with-offset]
SELECT TIMESTAMP '2026-10-19 12:30:45' AS "v" FROM "default"."T" AS "t";

--#[time-precision-7]
[UNSUPPORTED{message="TIME literal format '12:30:45.1234567'"}];
