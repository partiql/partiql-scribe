-- Redshift stores a TIMESTAMPTZ value in UTC; the literal is converted to UTC.
--#[timestamptz-literal-00]
SELECT TIMESTAMPTZ '2026-10-19 12:30:45+08:00' AS v FROM T;

--#[timestamptz-literal-01]
SELECT TIMESTAMPTZ '1997-12-17 07:37:16-08' AS v FROM T;

-- Without a time zone, the value is in the session time zone, which is assumed to be UTC.
--#[timestamptz-literal-02]
SELECT TIMESTAMPTZ '2026-10-19 12:30:45' AS v FROM T;

--#[timestamptz-literal-03]
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45.5-00:00' AS v1, TIMESTAMPTZ '2026-10-19 12:30:45 UTC' AS v2 FROM T;

--#[timestamptz-literal-04]
SELECT TIMESTAMPTZ 'epoch' AS v FROM T;

--#[timestamptz-literal-05]
SELECT TIMESTAMPTZ 'Jun 1,2008 09:59:59 EST' AS v FROM T;

-- Offsets are less than 16 hours.
--#[timestamptz-literal-06]
SELECT TIMESTAMPTZ '2026-10-19 12:30:45+16:00' AS v FROM T;

--#[timestamptz-literal-07]
SELECT TIMESTAMPTZ '0001-01-01 00:00:00+08:00' AS v1, TIMESTAMPTZ '2026-12-31 23:30:00-01:00' AS v2 FROM T;

-- Date formats whose meaning does not depend on the DateStyle setting.
--#[date-literal-00]
SELECT DATE '20080215' AS v1, DATE '2026-1-9' AS v2 FROM T;

--#[date-literal-01]
SELECT DATE '2000-Jan-31' AS v1, DATE 'Jan-31-2000' AS v2, DATE '31-Jan-2000' AS v3, DATE 'January 8, 1999' AS v4 FROM T;

-- The time of a DATE literal is ignored.
--#[date-literal-02]
SELECT DATE '20220502 04:05:06.789' AS v FROM T;

--#[date-literal-03]
SELECT DATE '1/8/1999' AS v FROM T;

--#[date-literal-04]
SELECT DATE 'today' AS v FROM T;

--#[timestamp-literal-01]
SELECT TIMESTAMP '2001-06-06' AS v1, TIMESTAMP '20080215 040506' AS v2, TIMESTAMP '20080215 04:05 PM' AS v3 FROM T;

-- Redshift ignores the time zone of a TIMESTAMP literal.
--#[timestamp-literal-02]
SELECT TIMESTAMP 'Jun 1,2008 09:59:59 EST' AS v FROM T;

--#[timestamp-literal-03]
SELECT TIMESTAMP 'epoch' AS v FROM T;

--#[timestamp-literal-04]
SELECT TIMESTAMP '2008-02-15T04:05:06' AS v FROM T;

-- 24:00:00 has no PartiQL value.
--#[timestamp-literal-05]
SELECT TIMESTAMP '2008-02-15 24:00:00' AS v FROM T;

--#[time-literal-01]
SELECT TIME '04:05' AS v1, TIME '04:05 PM' AS v2, TIME '040506' AS v3, TIME '12:30 AM' AS v4, TIME '12:30 PM' AS v5 FROM T;

--#[time-literal-02]
SELECT TIME '24:00:00' AS v FROM T;

--#[time-literal-03]
SELECT TIME '12:30:45+08:00' AS v FROM T;

-- A TIMETZ literal keeps its offset.
--#[timetz-literal-00]
SELECT TIMETZ '12:30:45+08' AS v1, TIMETZ '12:30:45-00:00' AS v2, TIMETZ '12:30:45 UTC' AS v3 FROM T;

--#[timetz-literal-01]
SELECT TIMETZ '12:30:45' AS v FROM T;

-- How Redshift compares TIMETZ values with an offset other than UTC is not documented.
--#[timetz-literal-02]
SELECT t.col_int32 FROM T_ALL_TYPES AS t WHERE t.col_timez = TIMETZ '12:30:45+08:00';

--#[timetz-literal-03]
SELECT t.col_int32 FROM T_ALL_TYPES AS t WHERE t.col_timez IN (TIMETZ '04:30:45+00:00', TIMETZ '05:30:45 UTC');
