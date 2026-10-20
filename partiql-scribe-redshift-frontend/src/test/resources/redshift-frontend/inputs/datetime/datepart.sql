-- EXTRACT(SECOND ...) returns whole seconds in Redshift.
--#[extract-second-00]
SELECT EXTRACT(SECOND FROM timestamp_1) AS s FROM T;

--#[extract-second-01]
SELECT EXTRACT(sec FROM t.col_time) AS s FROM T_ALL_TYPES AS t;

--#[extract-millisecond-00]
SELECT EXTRACT(ms FROM timestamp_1) AS ms FROM T;

--#[extract-quarter-00]
SELECT EXTRACT(quarter FROM timestamp_1) AS q FROM T;

-- The day of the week, 0 for Sunday.
--#[extract-dow-00]
SELECT EXTRACT(dow FROM t.col_date) AS d FROM T_ALL_TYPES AS t;

--#[extract-doy-00]
SELECT EXTRACT(doy FROM timestamp_1) AS d FROM T;

-- The ISO 8601 week.
--#[extract-week-00]
SELECT EXTRACT(week FROM timestamp_1) AS w FROM T;

-- Decade 1 is the years 1 to 9; centuries and millennia start with year 1.
--#[extract-decade-00]
SELECT EXTRACT(decade FROM timestamp_1) AS d, EXTRACT(century FROM timestamp_1) AS c, EXTRACT(mil FROM timestamp_1) AS m FROM T;

--#[extract-century-00]
SELECT EXTRACT(c FROM t.col_date) AS c FROM T_ALL_TYPES AS t;

-- EXTRACT returns DOUBLE PRECISION for a TIMESTAMPTZ value, with the fields in UTC.
--#[extract-timestamptz-00]
SELECT EXTRACT(hour FROM t.col_timestampz) AS h FROM T_ALL_TYPES AS t;

--#[extract-timestamptz-01]
SELECT EXTRACT(year FROM TIMESTAMPTZ '2026-01-01 02:30:45+08:00') AS y FROM T;

--#[extract-timestamptz-02]
SELECT EXTRACT(second FROM t.col_timestampz) AS s FROM T_ALL_TYPES AS t;

-- The local fields of a TIMETZ value.
--#[extract-timetz-00]
SELECT EXTRACT(hour FROM t.col_timez) AS h FROM T_ALL_TYPES AS t;

--#[extract-interval-00]
SELECT EXTRACT(day FROM timestamp_2 - timestamp_1) AS d FROM T;

-- The seconds of an interval keep the fraction.
--#[extract-interval-01]
SELECT EXTRACT(month FROM t.col_y2mon) AS m, EXTRACT(hour FROM t.col_d2s) AS h, EXTRACT(second FROM t.col_d2s) AS s FROM T_INTERVALS AS t;

--#[extract-interval-02]
SELECT EXTRACT(year FROM t.col_d2s) AS y FROM T_INTERVALS AS t;

--#[extract-string-00]
SELECT EXTRACT(year FROM '2008-01-01 10:00:00') AS y FROM T;

--#[extract-microsecond-00]
SELECT EXTRACT(microsecond FROM timestamp_1) AS us FROM T;

--#[extract-time-year-00]
SELECT EXTRACT(year FROM t.col_time) AS y FROM T_ALL_TYPES AS t;

--#[extract-super-00]
SELECT EXTRACT(hour FROM t.x) AS v FROM T AS t;

-- DATE_PART returns DOUBLE PRECISION; the date part is a name or a character string.
--#[date-part-00]
SELECT DATE_PART(year, t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[date-part-01]
SELECT DATE_PART('dow', timestamp_1) AS v FROM T;

--#[date-part-02]
SELECT PGDATE_PART(minute, t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[date-part-03]
SELECT DATE_PART(w, '2008-06-17') AS v FROM T;

-- The epoch of a DATE value is whole days in seconds.
--#[date-part-epoch-00]
SELECT DATE_PART(epoch, t.col_date) AS v FROM T_ALL_TYPES AS t;

-- Whether the epoch of a timestamp includes the fraction is not documented.
--#[date-part-epoch-01]
SELECT DATE_PART(epoch, timestamp_1) AS v FROM T;

-- The DOUBLE PRECISION value of the seconds is not specified.
--#[date-part-second-00]
SELECT DATE_PART(sec, timestamp_1) AS v FROM T;

--#[date-part-millisecond-00]
SELECT DATE_PART(ms, timestamp_1) AS v FROM T;

--#[date-part-time-00]
SELECT DATE_PART(hour, t.col_time) AS v FROM T_ALL_TYPES AS t;

--#[date-part-year-00]
SELECT DATE_PART_YEAR(t.col_date) AS v FROM T_ALL_TYPES AS t;

--#[date-part-year-01]
SELECT DATE_PART_YEAR(t.col_timestampz) AS v FROM T_ALL_TYPES AS t;
