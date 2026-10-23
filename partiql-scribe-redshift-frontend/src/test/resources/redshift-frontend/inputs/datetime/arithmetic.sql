-- date + integer adds days.
--#[date-plus-integer-00]
SELECT t.col_date + 1 AS v FROM T_ALL_TYPES AS t;

--#[date-plus-integer-01]
SELECT t.col_date - t.col_int32 AS v FROM T_ALL_TYPES AS t;

--#[date-plus-integer-02]
SELECT 1 + t.col_date AS v FROM T_ALL_TYPES AS t;

-- The unit of timestamp - number is not documented.
--#[timestamp-minus-number-00]
SELECT timestamp_1 - 1 AS v FROM T;

--#[date-plus-decimal-00]
SELECT t.col_date + 1.5 AS v FROM T_ALL_TYPES AS t;

-- date - date is a number of days.
--#[date-minus-date-00]
SELECT t.col_date - DATE '2020-01-01' AS v FROM T_ALL_TYPES AS t;

--#[date-minus-date-01]
SELECT (t.col_date + 1) - t.col_date AS v FROM T_ALL_TYPES AS t;

-- The difference of two timestamps is an interval in both languages.
--#[timestamp-minus-timestamp-00]
SELECT timestamp_2 - timestamp_1 AS v FROM T;

--#[timestamp-minus-timestamp-01]
SELECT t.col_timestampz - t.col_timestamp AS v FROM T_ALL_TYPES AS t;

--#[time-minus-time-00]
SELECT t.col_time - t.col_time AS v FROM T_ALL_TYPES AS t;

--#[timetz-minus-timetz-00]
SELECT t.col_timez - t.col_timez AS v FROM T_ALL_TYPES AS t;

--#[timestamp-plus-interval-00]
SELECT timestamp_1 + INTERVAL '1' DAY AS v FROM T;

--#[timestamp-plus-interval-01]
SELECT t.col_timestamp + t.col_d2s * 3 AS v FROM T_INTERVALS AS t;

--#[timestamp-plus-interval-02]
SELECT t.col_y2mon + t.col_timestampz AS v FROM T_INTERVALS AS t;

-- date + interval returns DATE for a YEAR TO MONTH interval and TIMESTAMP otherwise.
--#[date-plus-interval-00]
SELECT t.col_date - INTERVAL '1' MONTH AS v FROM T_ALL_TYPES AS t;

--#[date-plus-interval-01]
SELECT t.col_date + t.col_h AS v FROM T_INTERVALS AS t;

--#[date-plus-interval-02]
SELECT t.col_date + INTERVAL '3 hours, 35 minutes' AS v FROM T_ALL_TYPES AS t;

-- An interval literal without a qualifier gives a TIMESTAMP.
--#[date-plus-interval-03]
SELECT t.col_date - INTERVAL '4 months' AS v FROM T_ALL_TYPES AS t;

--#[time-plus-interval-00]
SELECT t.col_time + t.col_d2s AS v FROM T_INTERVALS AS t;

--#[time-plus-interval-01]
SELECT t.col_time + t.col_y AS v FROM T_INTERVALS AS t;

-- timetz + interval returns the time in UTC.
--#[timetz-plus-interval-00]
SELECT t.col_timez + t.col_h AS v FROM T_INTERVALS AS t;

-- date + time is the timestamp of the date at that time.
--#[date-plus-time-00]
SELECT t.col_date + t.col_time AS v FROM T_ALL_TYPES AS t;

--#[date-plus-time-01]
SELECT t.col_time + t.col_date AS v FROM T_ALL_TYPES AS t;

--#[date-plus-timetz-00]
SELECT t.col_date + t.col_timez AS v FROM T_ALL_TYPES AS t;

--#[interval-plus-interval-00]
SELECT t.col_d + t.col_h AS v FROM T_INTERVALS AS t;

--#[interval-plus-interval-01]
SELECT t.col_y2mon + t.col_d2s AS v FROM T_INTERVALS AS t;

--#[date-plus-string-00]
SELECT t.col_date + '1 day' AS v FROM T_ALL_TYPES AS t;
