--#[time-cast-00]
SELECT CAST(c AS TIME) AS v1, c::timetz AS v2, c::time with time zone AS v3 FROM T;

--#[timestamp-cast-00]
SELECT c::timestamptz AS v1, CAST(c AS TIMESTAMP WITH TIME ZONE) AS v2, c::timestamp without time zone AS v3 FROM T;

--#[time-literal-00]
SELECT TIME '12:30:45' AS v1, TIME '12:30:45.123456' AS v2, TIMETZ '12:30:45+08:00' AS v3 FROM T;

--#[timestamp-literal-00]
SELECT TIMESTAMPTZ '2026-10-19 12:30:45+08:00' AS v1, TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45-07:00' AS v2 FROM T;

--#[time-compare-00]
SELECT a FROM T WHERE TIME '12:30:45' > TIME '10:15:30';

--#[extract-00]
SELECT EXTRACT(YEAR FROM timestamp_1) AS y, EXTRACT(month FROM timestamp_1) AS m, EXTRACT(d FROM timestamp_1) AS d FROM T;

--#[extract-01]
SELECT EXTRACT(hour FROM TIME '12:30:45') AS h, EXTRACT(minute FROM TIME '12:30:45') AS m FROM T;

-- Redshift returns INTEGER; PartiQL returns a DECIMAL with the fraction. The translation adds a note.
--#[extract-02]
SELECT EXTRACT(SECOND FROM timestamp_1) AS s FROM T;

-- A quoted datepart is valid Redshift.
--#[extract-03]
SELECT EXTRACT('year' FROM timestamp_1) AS y FROM T;
