-- DATE_ADD is reserved syntax; delimit it to exercise the registered extension.
--#[date-add]
"date_add"('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
utcnow();

--#[contains]
SELECT contains(p.items, 'x') AS c FROM payload AS p;

--#[hex-to-bigint]
hex_to_bigint('00C10300');

-- A leading sign is accepted, like Long.parseLong (PartiQL: -255)
--#[hex-to-bigint-negative]
hex_to_bigint('-FF');

-- The most negative BIGINT (PartiQL: -9223372036854775808)
--#[hex-to-bigint-min]
hex_to_bigint('-8000000000000000');

--#[to-unixtime]
to_unixtime(TIMESTAMP '2017-01-02 03:04:05.006');

-- A fraction >= .5 must truncate, not round (PartiQL: 1483326245)
--#[to-unixtime-fraction]
to_unixtime(TIMESTAMP '2017-01-02 03:04:05.6');

-- Before 1970, truncation is toward negative infinity (PartiQL: -1)
--#[to-unixtime-pre-epoch]
to_unixtime(TIMESTAMP '1969-12-31 23:59:59.6');

-- Far from 1970 a DOUBLE epoch can't hold microseconds; must not round up (PartiQL: 253402300799)
--#[to-unixtime-max]
to_unixtime(TIMESTAMP '9999-12-31 23:59:59.999999');

--#[pow]
pow(2, 3);

-- PartiQL: 2020-08-01 00:00:00
--#[date-trunc]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

-- A date is truncated as midnight and returns a timestamp (PartiQL: 2020-07-01 00:00:00)
--#[date-trunc-date]
date_trunc('QUARTER', DATE '2020-08-13');

-- Weeks start on Monday (PartiQL: 2020-12-28 00:00:00)
--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

-- Truncated in the value's own offset (PartiQL: 2020-08-13 00:00:00-05:00); only Trino keeps the offset
--#[date-trunc-zoned]
date_trunc('day', TIMESTAMP WITH TIME ZONE '2020-08-13 23:20:30-05:00');

-- Not every target supports decade
--#[date-trunc-unsupported-part]
date_trunc('decade', TIMESTAMP '2020-08-13 23:20:30');

-- The fraction truncates; it must not round into the next year (PartiQL: 2020-12-31 23:59:59.999)
--#[to-char]
to_char(TIMESTAMP '2020-12-31 23:59:59.999999', 'YYYY-MM-DD HH24:MI:SS.MS');

-- A date is formatted as midnight (PartiQL: 2020/08/13 12:00:00 AM)
--#[to-char-date]
to_char(DATE '2020-08-13', 'yyyy/mm/dd hh12:mi:ss AM');

-- PartiQL: 20200813232030123
--#[to-char-no-separators]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYYMMDDHH24MISSMS');

-- Formatted in the value's own offset (PartiQL: 2020-08-13 23:20); only Trino keeps the offset
--#[to-char-zoned]
to_char(TIMESTAMP WITH TIME ZONE '2020-08-13 23:20:30-05:00', 'YYYY-MM-DD HH24:MI');

-- Names and fill mode are padded or localized differently across targets
--#[to-char-unsupported]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'FMMonth DD');

-- Mixed case is text, not a pattern (PartiQL: Mm)
--#[to-char-mixed-case]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'Mm');
