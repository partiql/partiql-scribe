-- NOTE: Redshift does not support top-level expression syntax.

--#[datetime-08]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT INTERVAL '5' SECOND + CAST("T"."timestamp_1" AS TIMESTAMP) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT INTERVAL '5' MINUTE + CAST("T"."timestamp_1" AS TIMESTAMP) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT INTERVAL '5' HOUR + CAST("T"."timestamp_1" AS TIMESTAMP) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT INTERVAL '5' DAY + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT INTERVAL '5' MONTH + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT INTERVAL '5' YEAR + CURRENT_DATE AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT DATEDIFF(YEAR, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-16]
SELECT DATEDIFF(MONTH, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-17]
SELECT DATEDIFF(DAY, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-18]
SELECT DATEDIFF(HOUR, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-19]
SELECT DATEDIFF(MINUTE, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-20]
SELECT DATEDIFF(SECOND, CAST("T"."timestamp_1" AS TIMESTAMP), CAST("T"."timestamp_2" AS TIMESTAMP)) AS "_1" FROM "default"."T" AS "T"

--#[datetime-21]
SELECT INTERVAL '1' SECOND + TIMESTAMP '2017-01-02 03:04:05.006' AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT DATEDIFF(SECOND, TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";

-- Explicit casts for comparison/equality with a datetime path
--#[datetime-24]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) > TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-25]
SELECT "t"."foo" FROM "default"."datatypes"."T_DATE" AS "t" WHERE DATE '2023-10-19' < CAST("t"."foo"."keep" AS DATE);

--#[datetime-26]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIME" AS "t" WHERE CAST("t"."foo"."keep" AS TIME) <= TIME '12:34:56';

--#[datetime-27]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-28]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) <> TIMESTAMP '2023-10-19 12:34:56';

-- Explicit casts for EXTRACT with a datetime path
-- TODO EXTRACT not yet supported in AST->PLAN conversion in v0.14.9
-- --#[datetime-29]
-- SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_TIMESTAMP AS t;
--
-- --#[datetime-30]
-- SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_DATE AS t;
--
-- --#[datetime-31]
-- SELECT EXTRACT(HOUR FROM t.foo.keep) FROM datatypes.T_TIME AS t;

-- Explicit casts for BETWEEN with a datetime path
--#[datetime-32]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE CAST("t"."foo"."keep" AS TIMESTAMP) BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-33]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE TIMESTAMP '2023-10-19 12:34:56' BETWEEN CAST("t"."foo"."keep" AS TIMESTAMP) AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-34]
SELECT "t"."foo" FROM "default"."datatypes"."T_TIMESTAMP" AS "t" WHERE TIMESTAMP '2023-12-19 12:34:56' BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND CAST("t"."foo"."keep" AS TIMESTAMP);

-- Explicit casts for NULLIF with a datetime path
--#[datetime-35]
SELECT NULLIF(CAST("t"."foo"."keep" AS TIMESTAMP), TIMESTAMP '2023-12-19 12:34:56') AS "_1" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-36]
SELECT NULLIF(TIMESTAMP '2023-12-19 12:34:56', CAST("t"."foo"."keep" AS TIMESTAMP)) AS "_1" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- Explicit casts for CASE-WHEN with a datetime path
--#[datetime-37]
SELECT CASE WHEN CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-12-19 12:34:56' THEN CAST("t"."foo"."keep" AS TIMESTAMP) ELSE sysdate END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-38]
SELECT CASE WHEN CAST("t"."foo"."keep" AS TIMESTAMP) = TIMESTAMP '2023-12-19 12:34:56' THEN TIMESTAMP '2023-12-19 12:34:56' ELSE CAST("t"."foo"."keep" AS TIMESTAMP) END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

--#[datetime-39]
SELECT CASE WHEN TIMESTAMP '2023-12-19 12:34:56' = CAST("t"."foo"."keep" AS TIMESTAMP) THEN TIMESTAMP '2023-12-19 12:34:56' ELSE CAST("t"."foo"."keep" AS TIMESTAMP) END AS "result" FROM "default"."datatypes"."T_TIMESTAMP" AS "t";

-- Explicit cast for top-level timestamp in comparison
--#[datetime-40]
SELECT "T"."timestamp_2", CAST("T"."timestamp_1" AS TIMESTAMP) < CAST("T"."timestamp_2" AS TIMESTAMP) AS "result" FROM "default"."T" AS "T" WHERE CAST("T"."timestamp_1" AS TIMESTAMP) > TIMESTAMP '2023-12-19 12:34:56';

--#[datetime-41]
SELECT DATEDIFF(SECOND, CAST("T"."col_time" AS TIME), CAST("T"."col_time" AS TIME)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-42]
SELECT DATEDIFF(SECOND, CAST("T"."col_time" AS TIMETZ), CAST("T"."col_timez" AS TIMETZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-43]
SELECT DATEDIFF(SECOND, CAST("T"."col_timez" AS TIMETZ), CAST("T"."col_time" AS TIMETZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-44]
SELECT DATEDIFF(SECOND, CAST("T"."col_timez" AS TIMETZ), CAST("T"."col_timez" AS TIMETZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-45]
SELECT DATEDIFF(DAY, CAST("T"."col_date" AS DATE), CAST("T"."col_date" AS DATE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-46]
SELECT DATEDIFF(DAY, CAST("T"."col_date" AS TIMESTAMP), CAST("T"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Redshift function pg_catalog.date_diff("unknown", timestamp with time zone, timestamp with time zone) does not exist
-- --#[datetime-47]
-- SELECT DATEDIFF(DAY, CAST("T"."col_date" AS TIMESTAMPTZ), CAST("T"."col_timestampz" AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-48]
SELECT DATEDIFF(DAY, CAST("T"."col_timestamp" AS TIMESTAMP), CAST("T"."col_date" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-49]
SELECT DATEDIFF(DAY, CAST("T"."col_timestamp" AS TIMESTAMP), CAST("T"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Redshift function pg_catalog.date_diff("unknown", timestamp with time zone, timestamp with time zone) does not exist
-- --#[datetime-50]
-- SELECT DATEDIFF(DAY, CAST("T"."col_timestamp" AS TIMESTAMPTZ), CAST("T"."col_timestampz" AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-51]
-- SELECT DATEDIFF(DAY, CAST("T"."col_timestampz" AS TIMESTAMPTZ), CAST("T"."col_date" AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
--
-- --#[datetime-52]
-- SELECT DATEDIFF(DAY, CAST("T"."col_timestampz" AS TIMESTAMPTZ), CAST("T"."col_timestamp" AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
--
-- --#[datetime-53]
-- SELECT DATEDIFF(DAY, CAST("T"."col_timestampz" AS TIMESTAMPTZ), CAST("T"."col_timestampz" AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- NOTE: Redshift does not support top-level expression syntax.

--#[datetime-54]
SELECT DATEDIFF(SECOND, TIME '12:34:56', TIME '13:45:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-55]
SELECT DATEDIFF(SECOND, CAST(TIME '12:34:56' AS TIMETZ), TIMETZ '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-56]
SELECT DATEDIFF(SECOND, TIMETZ '12:34:56+08:00', CAST(TIME '13:45:00' AS TIMETZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-57]
SELECT DATEDIFF(SECOND, TIMETZ '12:34:56+08:00', TIMETZ '13:45:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-58]
SELECT DATEDIFF(DAY, DATE '2023-01-15', DATE '2023-12-25') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-59]
SELECT DATEDIFF(DAY, CAST(DATE '2023-01-15' AS TIMESTAMP), TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Redshift function pg_catalog.date_diff("unknown", timestamp with time zone, timestamp with time zone) does not exist
-- #[datetime-60]
-- SELECT DATEDIFF(DAY, CAST(DATE '2023-01-15' AS TIMESTAMPTZ), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-61]
SELECT DATEDIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', CAST(DATE '2023-12-25' AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-62]
SELECT DATEDIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Redshift function pg_catalog.date_diff("unknown", timestamp with time zone, timestamp with time zone) does not exist
-- #[datetime-63]
-- SELECT DATEDIFF(DAY, CAST(TIMESTAMP '2023-01-15 08:00:00' AS TIMESTAMPTZ), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- #[datetime-64]
-- SELECT DATEDIFF(DAY, TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(DATE '2023-12-25' AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- #[datetime-65]
-- SELECT DATEDIFF(DAY, TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(TIMESTAMP '2023-12-25 10:30:00' AS TIMESTAMPTZ)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- #[datetime-66]
-- SELECT DATEDIFF(DAY, TIMESTAMPTZ '2023-01-15 08:00:00+08:00', TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
