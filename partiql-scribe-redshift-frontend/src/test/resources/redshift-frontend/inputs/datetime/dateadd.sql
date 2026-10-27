-- Adding years moves February 29 to March 1 in Redshift and to February 28 in PartiQL.
--#[dateadd-year-00]
SELECT DATEADD(year, 1, timestamp_1) AS v FROM T;

-- DATEADD returns TIMESTAMP for a DATE value.
--#[dateadd-year-01]
SELECT DATEADD(y, t.col_int32, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[dateadd-month-00]
SELECT DATEADD(month, 18, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[dateadd-day-00]
SELECT DATEADD(day, 30, timestamp_1) AS v FROM T;

-- m is minutes.
--#[dateadd-minute-00]
SELECT DATEADD(m, 30, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[dateadd-week-00]
SELECT DATEADD(week, 2, timestamp_1) AS v FROM T;

--#[dateadd-week-01]
SELECT DATEADD(w, t.col_int32, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[dateadd-millisecond-00]
SELECT DATEADD(ms, 1500, timestamp_1) AS v FROM T;

--#[dateadd-microsecond-00]
SELECT DATEADD(microsecond, t.col_int64, t.col_date) AS v FROM T_ALL_TYPES AS t;

-- TIME values wrap at midnight in both languages.
--#[dateadd-time-00]
SELECT DATEADD(hour, 12, TIME '13:24:55') AS v FROM T;

--#[dateadd-time-01]
SELECT DATEADD(ms, 5, t.col_time) AS v FROM T_ALL_TYPES AS t;

-- Redshift returns a TIMETZ value in UTC.
--#[dateadd-timetz-00]
SELECT DATEADD(hour, 2, t.col_timez) AS v FROM T_ALL_TYPES AS t;

--#[dateadd-timetz-01]
SELECT DATEADD(us, 5, TIMETZ '13:24:55-08:00') AS v FROM T;

-- A date part can be a character string.
--#[dateadd-quoted-00]
SELECT DATEADD('day', 1, timestamp_1) AS v FROM T;

-- Redshift converts a character string literal to a TIMESTAMP value.
--#[dateadd-string-00]
SELECT DATEADD(month, 18, '2008-02-28') AS v FROM T;

--#[dateadd-string-01]
SELECT DATEADD(day, 1, 'today') AS v FROM T;

--#[dateadd-getdate-00]
SELECT a FROM T WHERE timestamp_1 > DATEADD(day, -30, GETDATE());

-- The end of month rule of quarters, and the numbering of decades, centuries, and millennia, are not documented for
-- DATEADD.
--#[dateadd-quarter-00]
SELECT DATEADD(quarter, 1, timestamp_1) AS v FROM T;

--#[dateadd-century-00]
SELECT DATEADD(century, 1, timestamp_1) AS v FROM T;

--#[dateadd-dow-00]
SELECT DATEADD(dow, 1, timestamp_1) AS v FROM T;

-- The result type for a TIMESTAMPTZ value is not documented.
--#[dateadd-timestamptz-00]
SELECT DATEADD(hour, -2, t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

-- The interval must be an integer.
--#[dateadd-interval-00]
SELECT DATEADD(day, 2.5, timestamp_1) AS v FROM T;

--#[dateadd-time-day-00]
SELECT DATEADD(day, 1, t.col_time) AS v FROM T_ALL_TYPES AS t;

-- DATE_ADD is not a Redshift function.
--#[dateadd-synonym-00]
SELECT DATE_ADD('day', 1, timestamp_1) AS v FROM T;
