-- Literals: numbers, strings, booleans, NULL, datetime literals, intervals.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_Literals.html

--#[integers]
SELECT 0, 1, 42, 9223372036854775807, 99999999999999999999999999999999999999;

--#[decimals]
SELECT 1.5, 0.001, 100.0, .5, 5., 123456789.123456789;

--#[floats]
SELECT 1e3, 1E3, 1.5e-3, 1.5E+10, .5e2, 5.e1, 1e+0;

--#[strings]
SELECT 'a', '', 'hello world', 'with "double" quotes';

--#[strings-escaped-quote]
SELECT 'it''s', '''', 'a''b''c';

--#[strings-backslash]
SELECT 'back\slash', 'a\'b', '\\', 'tab\tnewline\n';

--#[strings-unicode]
SELECT 'héllo', '日本語', 'emoji 🙂';

--#[strings-multiline]
SELECT 'line one
line two';

--#[booleans-null]
SELECT TRUE, FALSE, NULL, true, false, null;

--#[date]
SELECT DATE '2024-01-31', date '2024-01-31';

--#[time]
SELECT TIME '12:34:56', TIME '12:34:56.789', TIME WITHOUT TIME ZONE '12:34:56';

--#[timetz]
SELECT TIMETZ '12:34:56+00', TIME WITH TIME ZONE '12:34:56-08:00';

--#[timestamp]
SELECT TIMESTAMP '2024-01-31 12:34:56', TIMESTAMP '2024-01-31 12:34:56.123456', TIMESTAMP WITHOUT TIME ZONE '2024-01-31 00:00:00';

--#[timestamptz]
SELECT TIMESTAMPTZ '2024-01-31 12:34:56+00', TIMESTAMP WITH TIME ZONE '2024-01-31 12:34:56 UTC';

--#[interval-string]
SELECT INTERVAL '1 day', INTERVAL '2 hours 30 minutes', INTERVAL '1 year 2 months', INTERVAL '0.5 days', INTERVAL '-3 weeks';

--#[interval-qualified]
SELECT INTERVAL '1' YEAR, INTERVAL '3' MONTH, INTERVAL '7' DAY, INTERVAL '12' HOUR, INTERVAL '30' MINUTE, INTERVAL '45' SECOND;

--#[interval-qualified-range]
SELECT INTERVAL '1-6' YEAR TO MONTH, INTERVAL '1 12:00:00' DAY TO SECOND, INTERVAL '12:30' HOUR TO MINUTE, INTERVAL '10:15:30.5' HOUR TO SECOND(1);

--#[interval-precision]
SELECT INTERVAL '1.5' SECOND(3), INTERVAL '1 00:00:00.123' DAY(2) TO SECOND(3);

--#[interval-arithmetic]
SELECT d + INTERVAL '1 day', ts - INTERVAL '1 hour', CURRENT_DATE - INTERVAL '30 days' FROM t;

--#[literal-in-condition]
SELECT * FROM t WHERE d >= DATE '2024-01-01' AND ts < TIMESTAMP '2024-02-01 00:00:00' AND flag = TRUE AND n <> 1.5e3;
