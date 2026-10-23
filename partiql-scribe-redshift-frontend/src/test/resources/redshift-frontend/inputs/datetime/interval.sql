-- A literal with a qualifier: the PartiQL literal has the leading precision 9 and 6 fractional digits of seconds.
--#[interval-qualified-00]
SELECT INTERVAL '1 2:3:4.5' DAY TO SECOND AS v FROM T;

--#[interval-qualified-01]
SELECT INTERVAL '100' DAY AS v FROM T;

--#[interval-qualified-02]
SELECT INTERVAL '10.234' SECOND(3) AS v FROM T;

--#[interval-qualified-03]
SELECT INTERVAL '-10-3' YEAR TO MONTH AS v FROM T;

-- In Redshift, the days and the time of a DAY TO SECOND value have their own signs.
--#[interval-qualified-04]
SELECT INTERVAL '-10 -3:4:5' DAY TO SECOND AS v FROM T;

--#[interval-qualified-05]
SELECT INTERVAL '-10 3:4:5' DAY TO SECOND AS v FROM T;

-- A negative value is written from its first nonzero field.
--#[interval-qualified-06]
SELECT INTERVAL '-0:30' HOUR TO MINUTE AS v FROM T;

--#[interval-qualified-07]
SELECT INTERVAL '-0.5' SECOND AS v FROM T;

-- Fields after the first must be in range.
--#[interval-qualified-08]
SELECT INTERVAL '70:70.123' MINUTE TO SECOND AS v FROM T;

-- Redshift rounds or truncates extra fractional digits.
--#[interval-qualified-09]
SELECT INTERVAL '1.234567' SECOND(3) AS v FROM T;

--#[interval-qualified-10]
SELECT INTERVAL '5 HOURS' HOUR AS v FROM T;

--#[interval-qualified-11]
SELECT INTERVAL '2' DAY(3) AS v FROM T;

--#[interval-qualified-12]
SELECT INTERVAL '1-2' YEAR TO DAY AS v FROM T;

--#[interval-qualified-13]
SELECT INTERVAL '1' SECOND(7) AS v FROM T;

-- The leading field holds the larger units.
--#[interval-qualified-14]
SELECT INTERVAL '25:00' HOUR TO MINUTE AS v1, INTERVAL '+5' DAY AS v2 FROM T;

-- A literal without a qualifier is rewritten with a qualifier.
--#[interval-unqualified-00]
SELECT INTERVAL '7 days' AS v FROM T;

--#[interval-unqualified-01]
SELECT timestamp_1 + INTERVAL '1w, 1h, 1m, 1s' AS v FROM T;

--#[interval-unqualified-02]
SELECT timestamp_1 - INTERVAL '4 months' AS v FROM T;

--#[interval-unqualified-03]
SELECT INTERVAL '2 years' AS v, INTERVAL '1 year 2 months' AS w FROM T;

-- Each quantity has its own sign.
--#[interval-unqualified-04]
SELECT timestamp_1 + INTERVAL '-1 hour 30 minutes' AS v FROM T;

--#[interval-unqualified-05]
SELECT t.col_date + INTERVAL '0.5 days' AS v FROM T_ALL_TYPES AS t;

-- Redshift keeps 36 hours, where PartiQL has 1 day and 12 hours: the same duration in datetime arithmetic.
--#[interval-unqualified-06]
SELECT timestamp_1 + INTERVAL '36 hours' AS v FROM T;

--#[interval-unqualified-07]
SELECT INTERVAL '36 hours' AS v FROM T;

--#[interval-unqualified-08]
SELECT INTERVAL '1 month 2 days' AS v FROM T;

-- INTERVAL types are not translated.
--#[interval-type-00]
SELECT c::interval day to second AS v FROM T;

--#[interval-type-01]
SELECT CAST(c AS INTERVAL) AS v FROM T;
