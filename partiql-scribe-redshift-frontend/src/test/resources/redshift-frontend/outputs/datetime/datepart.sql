--#[extract-second-00]
SELECT CAST(CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) / CAST(1000000000 AS BIGINT) AS INT) AS "s" FROM "default"."T" AS "t";

--#[extract-second-01]
SELECT CAST(CAST(EXTRACT(SECOND FROM "t"['col_time']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) / CAST(1000000000 AS BIGINT) AS INT) AS "s" FROM "default"."T_ALL_TYPES" AS "t";

--#[extract-millisecond-00]
SELECT CAST((CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) % CAST(1000000000 AS BIGINT)) / CAST(1000000 AS BIGINT) AS INT) AS "ms" FROM "default"."T" AS "t";

--#[extract-quarter-00]
SELECT ((EXTRACT(MONTH FROM "t"['timestamp_1']) - 1) / 3) + 1 AS "q" FROM "default"."T" AS "t";

--#[extract-dow-00]
SELECT CAST((DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) + CAST(1 AS BIGINT)) % CAST(7 AS BIGINT) AS INT) AS "d" FROM "default"."T_ALL_TYPES" AS "t";

--#[extract-doy-00]
SELECT CAST(DATE_DIFF(DAY, DATE_ADD(YEAR, DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']), TIMESTAMP '0001-01-01 00:00:00'), "t"['timestamp_1']) + CAST(1 AS BIGINT) AS INT) AS "d" FROM "default"."T" AS "t";

--#[extract-week-00]
SELECT CAST((DATE_DIFF(DAY, DATE_ADD(YEAR, DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', CAST(DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01') AS TIMESTAMP (6))), DATE '0001-01-01'), DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01')) / CAST(7 AS BIGINT)) + CAST(1 AS BIGINT) AS INT) AS "w" FROM "default"."T" AS "t";

--#[extract-decade-00]
SELECT (EXTRACT(YEAR FROM "t"['timestamp_1']) / 10) + 1 AS "d", ((EXTRACT(YEAR FROM "t"['timestamp_1']) - 1) / 100) + 1 AS "c", ((EXTRACT(YEAR FROM "t"['timestamp_1']) - 1) / 1000) + 1 AS "m" FROM "default"."T" AS "t";

--#[extract-century-00]
SELECT ((EXTRACT(YEAR FROM "t"['col_date']) - 1) / 100) + 1 AS "c" FROM "default"."T_ALL_TYPES" AS "t";

--#[extract-timestamptz-00]
SELECT CAST(EXTRACT(HOUR FROM CAST("t"['col_timestampz'] AS TIMESTAMP)) AS DOUBLE PRECISION) AS "h" FROM "default"."T_ALL_TYPES" AS "t";

--#[extract-timestamptz-01]
SELECT CAST(EXTRACT(YEAR FROM CAST(TIMESTAMP WITH TIME ZONE '2025-12-31 18:30:45+00:00' AS TIMESTAMP)) AS DOUBLE PRECISION) AS "y" FROM "default"."T" AS "t";

--#[extract-timestamptz-02]
[UNSUPPORTED{message="EXTRACT(SECOND ...) of a TIMESTAMPTZ value"}];

--#[extract-timetz-00]
SELECT EXTRACT(HOUR FROM CAST("t"['col_timez'] AS TIME (6))) AS "h" FROM "default"."T_ALL_TYPES" AS "t";

--#[extract-interval-00]
SELECT EXTRACT(DAY FROM "t"['timestamp_2'] - "t"['timestamp_1']) AS "d" FROM "default"."T" AS "t";

--#[extract-interval-01]
SELECT EXTRACT(MONTH FROM "t"['col_y2mon']) AS "m", EXTRACT(HOUR FROM "t"['col_d2s']) AS "h", EXTRACT(SECOND FROM "t"['col_d2s']) AS "s" FROM "default"."T_INTERVALS" AS "t";

--#[extract-interval-02]
[UNSUPPORTED{message="EXTRACT datepart `year` of an INTERVAL DAY TO SECOND value"}];

--#[extract-string-00]
[UNSUPPORTED{message="EXTRACT from a character string"}];

--#[extract-microsecond-00]
[UNSUPPORTED{message="EXTRACT datepart `microsecond`"}];

--#[extract-time-year-00]
[UNSUPPORTED{message="EXTRACT datepart `year` of a TIME value"}];

--#[extract-super-00]
[UNSUPPORTED{message="EXTRACT with an argument of unknown type"}];

--#[date-part-00]
SELECT CAST(EXTRACT(YEAR FROM "t"['col_date']) AS DOUBLE PRECISION) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-part-01]
SELECT CAST(CAST((DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) + CAST(1 AS BIGINT)) % CAST(7 AS BIGINT) AS INT) AS DOUBLE PRECISION) AS "v" FROM "default"."T" AS "t";

--#[date-part-02]
SELECT CAST(EXTRACT(MINUTE FROM CAST("t"['col_timestampz'] AS TIMESTAMP)) AS DOUBLE PRECISION) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-part-03]
SELECT CAST(CAST((DATE_DIFF(DAY, DATE_ADD(YEAR, DATE_DIFF(YEAR, TIMESTAMP '0001-01-01 00:00:00', CAST(DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-06-17 00:00:00') - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-06-17 00:00:00') % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01') AS TIMESTAMP (6))), DATE '0001-01-01'), DATE_ADD(DAY, (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-06-17 00:00:00') - (DATE_DIFF(DAY, TIMESTAMP '0001-01-01 00:00:00', TIMESTAMP '2008-06-17 00:00:00') % CAST(7 AS BIGINT))) + CAST(3 AS BIGINT), DATE '0001-01-01')) / CAST(7 AS BIGINT)) + CAST(1 AS BIGINT) AS INT) AS DOUBLE PRECISION) AS "v" FROM "default"."T" AS "t";

--#[date-part-epoch-00]
SELECT CAST(DATE_DIFF(SECOND, TIMESTAMP '1970-01-01 00:00:00', CAST("t"['col_date'] AS TIMESTAMP (6))) AS DOUBLE PRECISION) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-part-epoch-01]
[UNSUPPORTED{message="DATE_PART(epoch, ...) of a TIMESTAMP value"}];

--#[date-part-second-00]
[UNSUPPORTED{message="DATE_PART datepart `sec`"}];

--#[date-part-millisecond-00]
[UNSUPPORTED{message="DATE_PART datepart `ms`"}];

--#[date-part-time-00]
[UNSUPPORTED{message="DATE_PART with a TIME argument"}];

--#[date-part-year-00]
SELECT EXTRACT(YEAR FROM "t"['col_date']) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[date-part-year-01]
SELECT EXTRACT(YEAR FROM CAST("t"['col_timestampz'] AS TIMESTAMP)) AS "v" FROM "default"."T_ALL_TYPES" AS "t";
