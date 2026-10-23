-- DATEDIFF counts the boundaries of the date part between two values; PartiQL DATE_DIFF counts whole units. The
-- translation counts the whole units from a reference timestamp to each value.
--#[datediff-day-00]
SELECT DATEDIFF(day, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-day-01]
SELECT DATEDIFF(days, t.col_date, t.col_timestamp) AS v FROM T_ALL_TYPES AS t;

-- The number of days between two DATE values is PartiQL DATE_DIFF(DAY, ...).
--#[datediff-day-02]
SELECT DATEDIFF(d, t.col_date, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[datediff-year-00]
SELECT DATEDIFF(year, t.col_date, t.col_timestamp) AS v FROM T_ALL_TYPES AS t;

--#[datediff-month-00]
SELECT DATEDIFF(mon, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-hour-00]
SELECT DATEDIFF(hour, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-minute-00]
SELECT DATEDIFF(minute, t.col_date, t.col_timestamp) AS v FROM T_ALL_TYPES AS t;

--#[datediff-second-00]
SELECT DATEDIFF(s, timestamp_1, timestamp_2) AS v FROM T;

-- Milliseconds and microseconds: whole seconds and the fraction of EXTRACT(SECOND).
--#[datediff-millisecond-00]
SELECT DATEDIFF(ms, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-microsecond-00]
SELECT DATEDIFF(microsecond, timestamp_1, timestamp_2) AS v FROM T;

-- TIME values count from midnight.
--#[datediff-time-00]
SELECT DATEDIFF(hour, t.col_time, TIME '15:24:45') AS v FROM T_ALL_TYPES AS t;

--#[datediff-time-01]
SELECT DATEDIFF(ms, t.col_time, t.col_time) AS v FROM T_ALL_TYPES AS t;

-- TIMETZ values count from midnight in UTC.
--#[datediff-timetz-00]
SELECT DATEDIFF(hour, TIMETZ '20:00:00-08:00', t.col_timez) AS v FROM T_ALL_TYPES AS t;

-- A date part can be a character string.
--#[datediff-quoted-00]
SELECT DATEDIFF('day', timestamp_1, timestamp_2) AS v FROM T;

-- Redshift converts character string literals to TIMESTAMP values.
--#[datediff-string-00]
SELECT DATEDIFF(hour, '2023-01-01', '2023-01-03 05:04:03') AS v FROM T;

--#[datediff-string-01]
SELECT DATEDIFF(day, t.col_date, '2008-01-04') AS v FROM T_ALL_TYPES AS t;

-- Redshift can read a character string with a time next to a DATE value as a DATE.
--#[datediff-string-02]
SELECT DATEDIFF(hour, t.col_date, '2009-01-01 10:00:00') AS v FROM T_ALL_TYPES AS t;

-- A character string with a time zone can be read as a TIMESTAMPTZ value.
--#[datediff-string-03]
SELECT DATEDIFF(day, 'Jun 1,2008 09:59:59 EST', timestamp_1) AS v FROM T;

--#[datediff-string-04]
SELECT DATEDIFF(day, c, timestamp_1) AS v FROM T;

--#[datediff-nested-00]
SELECT DATEDIFF(day, DATEADD(day, 1, timestamp_1), timestamp_2) AS v FROM T;

-- Quarter and week boundaries are not documented for DATEDIFF.
--#[datediff-quarter-00]
SELECT DATEDIFF(qtr, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-week-00]
SELECT DATEDIFF(week, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-decade-00]
SELECT DATEDIFF(decade, timestamp_1, timestamp_2) AS v FROM T;

--#[datediff-dow-00]
SELECT DATEDIFF(dow, timestamp_1, timestamp_2) AS v FROM T;

-- Redshift has no DATEDIFF for TIMESTAMPTZ values.
--#[datediff-timestamptz-00]
SELECT DATEDIFF(day, t.col_timestampz, t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[datediff-time-day-00]
SELECT DATEDIFF(day, t.col_time, t.col_time) AS v FROM T_ALL_TYPES AS t;

--#[datediff-mixed-00]
SELECT DATEDIFF(hour, t.col_time, t.col_timestamp) AS v FROM T_ALL_TYPES AS t;

--#[datediff-super-00]
SELECT DATEDIFF(day, t.col_any, t.col_date) AS v FROM T_ALL_TYPES AS t;
