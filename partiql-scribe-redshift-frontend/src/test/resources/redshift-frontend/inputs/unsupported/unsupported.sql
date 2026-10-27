-- ILIKE is case-insensitive for ASCII letters only.
--#[ilike]
SELECT a FROM T WHERE c ILIKE '%é%';

--#[unknown-function]
SELECT MD5(c) AS v FROM T;

--#[datepart-week]
SELECT DATEADD(week, 1, timestamp_1) AS v FROM T;

--#[super-type]
SELECT x::super AS v FROM T;

--#[extract-week]
SELECT EXTRACT(week FROM timestamp_1) AS w FROM T;

--#[extract-timezone-hour]
SELECT EXTRACT(timezone_hour FROM TIMESTAMPTZ '2026-10-19 12:30:45+08:00') AS h FROM T;

--#[extract-epoch]
SELECT EXTRACT(epoch FROM timestamp_1) AS e FROM T;

-- Redshift accepts time zone names; PartiQL accepts only offsets.
--#[timetz-zone-name]
SELECT TIMETZ '12:30:45 PST' AS v FROM T;

-- Redshift ignores an offset in a TIMESTAMP literal; the translation removes it.
--#[timestamp-with-offset]
SELECT TIMESTAMP '2026-10-19 12:30:45+08:00' AS v FROM T;

--#[time-precision-7]
SELECT TIME '12:30:45.1234567' AS v FROM T;

-- OBJECT_TRANSFORM with KEEP and SET paths is recognized.
--#[object-transform]
SELECT OBJECT_TRANSFORM(t.x KEEP '"a"' SET '"b"', 1) AS v FROM T t;
