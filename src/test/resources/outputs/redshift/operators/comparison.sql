--#[eq-00]
SELECT "T"."a" = true AS "_1" FROM "default"."T" AS "T";

--#[eq-01]
SELECT "T"."b" = 1 AS "_1" FROM "default"."T" AS "T";

--#[eq-02]
SELECT "T"."c" = 'a' AS "_1" FROM "default"."T" AS "T";

--#[eq-03]
SELECT "T"."c" = "T"."z" AS "_1" FROM "default"."T" AS "T";

--#[eq-04]
SELECT "T"."c" = "T"."d"."e" AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[eq-05]
SELECT "T"."c" = "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[neq-00]
SELECT "T"."a" <> true AS "_1" FROM "default"."T" AS "T";

--#[neq-01]
SELECT "T"."b" <> 1 AS "_1" FROM "default"."T" AS "T";

--#[neq-02]
SELECT "T"."c" <> 'a' AS "_1" FROM "default"."T" AS "T";

--#[neq-03]
SELECT "T"."c" <> "T"."z" AS "_1" FROM "default"."T" AS "T";

--#[neq-04]
SELECT "T"."c" <> "T"."d"."e" AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[neq-05]
SELECT "T"."c" <> "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[lt-00]
SELECT "T"."b" < 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[lt-01]
SELECT "T"."b" < "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[lte-00]
SELECT "T"."b" <= 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[lte-01]
SELECT "T"."b" <= "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[gt-00]
SELECT "T"."b" > 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[gt-01]
SELECT "T"."b" > "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[gte-00]
SELECT "T"."b" >= 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[gte-01]
-- SELECT "T"."b" >= "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[not-00]
SELECT NOT ("T"."a") AS "_1" FROM "default"."T" AS "T";

--#[not-01]
SELECT NOT (NOT ("T"."a")) AS "_1" FROM "default"."T" AS "T";

-- Time comparisons
--#[comparison-datetime-00]
SELECT CAST("T"."col_time" AS TIME) > CAST("T"."col_time" AS TIME) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-01]
SELECT CAST("T"."col_time" AS TIME) < CAST("T"."col_time" AS TIME) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-02]
SELECT CAST("T"."col_time" AS TIME) >= CAST("T"."col_time" AS TIME) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-03]
SELECT CAST("T"."col_time" AS TIME) <= CAST("T"."col_time" AS TIME) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-04]
SELECT CAST("T"."col_time" AS TIMETZ) > CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-05]
SELECT CAST("T"."col_time" AS TIMETZ) < CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-06]
SELECT CAST("T"."col_time" AS TIMETZ) >= CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-07]
SELECT CAST("T"."col_time" AS TIMETZ) <= CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-08]
SELECT CAST("T"."col_timez" AS TIMETZ) > CAST("T"."col_time" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-09]
SELECT CAST("T"."col_timez" AS TIMETZ) < CAST("T"."col_time" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-10]
SELECT CAST("T"."col_timez" AS TIMETZ) >= CAST("T"."col_time" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-11]
SELECT CAST("T"."col_timez" AS TIMETZ) <= CAST("T"."col_time" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-12]
SELECT CAST("T"."col_timez" AS TIMETZ) > CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-13]
SELECT CAST("T"."col_timez" AS TIMETZ) < CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-14]
SELECT CAST("T"."col_timez" AS TIMETZ) >= CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-15]
SELECT CAST("T"."col_timez" AS TIMETZ) <= CAST("T"."col_timez" AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Date comparisons
--#[comparison-datetime-16]
SELECT CAST("T"."col_date" AS DATE) > CAST("T"."col_date" AS DATE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-17]
SELECT CAST("T"."col_date" AS DATE) < CAST("T"."col_date" AS DATE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-18]
SELECT CAST("T"."col_date" AS DATE) >= CAST("T"."col_date" AS DATE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-19]
SELECT CAST("T"."col_date" AS DATE) <= CAST("T"."col_date" AS DATE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-20]
SELECT CAST("T"."col_date" AS TIMESTAMP) > CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-21]
SELECT CAST("T"."col_date" AS TIMESTAMP) < CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-22]
SELECT CAST("T"."col_date" AS TIMESTAMP) >= CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-23]
SELECT CAST("T"."col_date" AS TIMESTAMP) <= CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-24]
SELECT CAST("T"."col_date" AS TIMESTAMPTZ) > CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-25]
SELECT CAST("T"."col_date" AS TIMESTAMPTZ) < CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-26]
SELECT CAST("T"."col_date" AS TIMESTAMPTZ) >= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-27]
SELECT CAST("T"."col_date" AS TIMESTAMPTZ) <= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestamp comparisons
--#[comparison-datetime-28]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) > CAST("T"."col_date" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-29]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) < CAST("T"."col_date" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-30]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) >= CAST("T"."col_date" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-31]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) <= CAST("T"."col_date" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-32]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) > CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-33]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) < CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-34]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) >= CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-35]
SELECT CAST("T"."col_timestamp" AS TIMESTAMP) <= CAST("T"."col_timestamp" AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-36]
SELECT CAST("T"."col_timestamp" AS TIMESTAMPTZ) > CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-37]
SELECT CAST("T"."col_timestamp" AS TIMESTAMPTZ) < CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-38]
SELECT CAST("T"."col_timestamp" AS TIMESTAMPTZ) >= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-39]
SELECT CAST("T"."col_timestamp" AS TIMESTAMPTZ) <= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestampz comparisons
--#[comparison-datetime-40]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) > CAST("T"."col_date" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-41]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) < CAST("T"."col_date" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-42]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) >= CAST("T"."col_date" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-43]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) <= CAST("T"."col_date" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-44]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) > CAST("T"."col_timestamp" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-45]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) < CAST("T"."col_timestamp" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-46]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) >= CAST("T"."col_timestamp" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-47]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) <= CAST("T"."col_timestamp" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-48]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) > CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-49]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) < CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-50]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) >= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-51]
SELECT CAST("T"."col_timestampz" AS TIMESTAMPTZ) <= CAST("T"."col_timestampz" AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Time literal comparisons
--#[comparison-datetime-52]
SELECT TIME '12:30:45' > TIME '10:15:30' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-53]
SELECT TIME '12:30:45' < TIME '14:45:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-54]
SELECT TIME '12:30:45' >= TIME '12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-55]
SELECT TIME '12:30:45' <= TIME '12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-56]
SELECT CAST(TIME '12:30:45' AS TIMETZ) > TIMETZ '10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-57]
SELECT CAST(TIME '12:30:45' AS TIMETZ) < TIMETZ '14:45:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-58]
SELECT CAST(TIME '12:30:45' AS TIMETZ) >= TIMETZ '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-59]
SELECT CAST(TIME '12:30:45' AS TIMETZ) <= TIMETZ '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-60]
SELECT TIMETZ '12:30:45+08:00' > CAST(TIME '10:15:30' AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-61]
SELECT TIMETZ '12:30:45+08:00' < CAST(TIME '14:45:00' AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-62]
SELECT TIMETZ '12:30:45+08:00' >= CAST(TIME '12:30:45' AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-63]
SELECT TIMETZ '12:30:45+08:00' <= CAST(TIME '12:30:45' AS TIMETZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-64]
SELECT TIMETZ '12:30:45+08:00' > TIMETZ '10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-65]
SELECT TIMETZ '12:30:45+08:00' < TIMETZ '14:45:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-66]
SELECT TIMETZ '12:30:45+08:00' >= TIMETZ '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-67]
SELECT TIMETZ '12:30:45+08:00' <= TIMETZ '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Date literal comparisons
--#[comparison-datetime-68]
SELECT DATE '2023-12-25' > DATE '2023-06-15' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-69]
SELECT DATE '2023-12-25' < DATE '2024-01-01' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-70]
SELECT DATE '2023-12-25' >= DATE '2023-12-25' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-71]
SELECT DATE '2023-12-25' <= DATE '2023-12-25' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-72]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP) > TIMESTAMP '2023-06-15 08:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-73]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP) < TIMESTAMP '2024-01-01 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-74]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP) >= TIMESTAMP '2023-12-25 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-75]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP) <= TIMESTAMP '2023-12-25 23:59:59' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-76]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMPTZ) > TIMESTAMPTZ '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-77]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMPTZ) < TIMESTAMPTZ '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-78]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMPTZ) >= TIMESTAMPTZ '2023-12-25 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-79]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMPTZ) <= TIMESTAMPTZ '2023-12-25 23:59:59+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestamp literal comparisons
--#[comparison-datetime-80]
SELECT TIMESTAMP '2023-12-25 12:30:45' > CAST(DATE '2023-06-15' AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-81]
SELECT TIMESTAMP '2023-12-25 12:30:45' < CAST(DATE '2024-01-01' AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-82]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= CAST(DATE '2023-12-25' AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-83]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= CAST(DATE '2023-12-25' AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-84]
SELECT TIMESTAMP '2023-12-25 12:30:45' > TIMESTAMP '2023-06-15 08:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-85]
SELECT TIMESTAMP '2023-12-25 12:30:45' < TIMESTAMP '2024-01-01 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-86]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= TIMESTAMP '2023-12-25 12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-87]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= TIMESTAMP '2023-12-25 12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-88]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) > TIMESTAMPTZ '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-89]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) < TIMESTAMPTZ '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-90]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) >= TIMESTAMPTZ '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-91]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) <= TIMESTAMPTZ '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestampz literal comparisons
--#[comparison-datetime-92]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' > CAST(DATE '2023-06-15' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-93]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' < CAST(DATE '2024-01-01' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-94]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' >= CAST(DATE '2023-12-25' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-95]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' <= CAST(DATE '2023-12-25' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-96]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' > CAST(TIMESTAMP '2023-06-15 08:00:00' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-97]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' < CAST(TIMESTAMP '2024-01-01 00:00:00' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-98]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' >= CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-99]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' <= CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMPTZ) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-100]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' > TIMESTAMPTZ '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-101]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' < TIMESTAMPTZ '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-102]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' >= TIMESTAMPTZ '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-103]
SELECT TIMESTAMPTZ '2023-12-25 12:30:45+08:00' <= TIMESTAMPTZ '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
