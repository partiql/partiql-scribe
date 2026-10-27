-- DATE_TRUNC: the reference timestamp plus the whole units to the value.
--#[date-trunc-00]
SELECT DATE_TRUNC('month', timestamp_1) AS v FROM T;

-- Weeks start on Monday.
--#[date-trunc-01]
SELECT DATE_TRUNC('week', t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[date-trunc-02]
SELECT DATE_TRUNC('quarter', t.col_timestamp) AS q, DATE_TRUNC('mil', t.col_timestamp) AS m FROM T_ALL_TYPES AS t;

--#[date-trunc-03]
SELECT DATE_TRUNC('decade', timestamp_1) AS d, DATE_TRUNC('centuries', timestamp_1) AS c FROM T;

--#[date-trunc-04]
SELECT DATE_TRUNC('ms', timestamp_1) AS v FROM T;

--#[date-trunc-05]
SELECT DATE_TRUNC('day', 'Jun 1,2008 09:59:59') AS v FROM T;

-- The result type for a TIMESTAMPTZ value is not documented.
--#[date-trunc-06]
SELECT DATE_TRUNC('d', t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[date-trunc-07]
SELECT DATE_TRUNC(month, timestamp_1) AS v FROM T;

--#[date-trunc-08]
SELECT DATE_TRUNC('day', '6/1/2008 09:59:59') AS v FROM T;

--#[last-day-00]
SELECT LAST_DAY(timestamp_1) AS v FROM T;

--#[last-day-01]
SELECT LAST_DAY(t.col_date) AS v FROM T_ALL_TYPES AS t;

-- The last day of a month stays the last day.
--#[add-months-00]
SELECT ADD_MONTHS(t.col_date, 1) AS v FROM T_ALL_TYPES AS t;

--#[add-months-01]
SELECT ADD_MONTHS(timestamp_1, -2) AS v FROM T;

--#[add-months-02]
SELECT ADD_MONTHS(t.col_timestampz, 1) AS v FROM T_ALL_TYPES AS t;

--#[add-months-03]
SELECT ADD_MONTHS(t.col_timestamp, t.col_decimal) AS v FROM T_ALL_TYPES AS t;

--#[next-day-00]
SELECT NEXT_DAY(timestamp_1, 'Tuesday') AS v FROM T;

--#[next-day-01]
SELECT NEXT_DAY(t.col_date, 'su') AS v FROM T_ALL_TYPES AS t;

--#[next-day-02]
SELECT NEXT_DAY(timestamp_1, c) AS v FROM T;

--#[next-day-03]
SELECT NEXT_DAY(timestamp_1, 'Tomorrow') AS v FROM T;

-- The comparison functions return -1, 0, or 1.
--#[date-cmp-00]
SELECT DATE_CMP(t.col_date, DATE '2008-01-04') AS v FROM T_ALL_TYPES AS t;

--#[date-cmp-01]
SELECT DATE_CMP(t.col_date, '2008-01-04') AS v FROM T_ALL_TYPES AS t;

--#[date-cmp-02]
SELECT DATE_CMP_TIMESTAMPTZ(t.col_date, t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[date-cmp-03]
SELECT DATE_CMP(t.col_timestamp, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[timestamp-cmp-00]
SELECT TIMESTAMP_CMP(timestamp_1, timestamp_2) AS v FROM T;

--#[timestamp-cmp-01]
SELECT TIMESTAMP_CMP_TIMESTAMPTZ(t.col_timestamp, t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[timestamptz-cmp-00]
SELECT TIMESTAMPTZ_CMP_DATE(t.col_timestampz, '2008-01-04') AS v FROM T_ALL_TYPES AS t;

-- Only the UTC time zone is translated.
--#[convert-timezone-00]
SELECT CONVERT_TIMEZONE('UTC', timestamp_1) AS v FROM T;

--#[convert-timezone-01]
SELECT CONVERT_TIMEZONE('UTC', 'GMT', t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[convert-timezone-02]
SELECT CONVERT_TIMEZONE('utc', '2008-08-21 07:23:54') AS v FROM T;

--#[convert-timezone-03]
SELECT CONVERT_TIMEZONE('UTC', 'America/New_York', timestamp_1) AS v FROM T;

--#[convert-timezone-04]
SELECT CONVERT_TIMEZONE(c, timestamp_1) AS v FROM T;

--#[convert-timezone-05]
SELECT CONVERT_TIMEZONE('UTC') AS v FROM T;

--#[at-time-zone-00]
SELECT timestamp_1 AT TIME ZONE 'UTC' AS v FROM T;

--#[at-time-zone-01]
SELECT t.col_timestampz AT TIME ZONE 'utc' AS v FROM T_ALL_TYPES AS t;

--#[at-time-zone-02]
SELECT timestamp_1 AT TIME ZONE 'PST' AS v FROM T;

--#[at-time-zone-03]
SELECT t.col_date AT TIME ZONE 'UTC' AS v FROM T_ALL_TYPES AS t;

--#[timezone-00]
SELECT TIMEZONE('UTC', timestamp_1) AS v FROM T;

--#[timezone-01]
SELECT TIMEZONE('GMT', '2008-06-17 09:44:54') AS v FROM T;

--#[timezone-02]
SELECT TIMEZONE('PST', timestamp_1) AS v FROM T;

-- Functions without a PartiQL equivalent.
--#[months-between-00]
SELECT MONTHS_BETWEEN(t.col_date, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[to-timestamp-00]
SELECT TO_TIMESTAMP(c, 'YYYY-MM-DD HH24:MI:SS') AS v FROM T;

--#[to-char-00]
SELECT TO_CHAR(timestamp_1, 'YYYY-MM-DD') AS v FROM T;

--#[to-date-00]
SELECT TO_DATE(c, 'YYYY-MM-DD') AS v FROM T;

--#[interval-cmp-00]
SELECT INTERVAL_CMP('3 days', '1 year') AS v FROM T;
