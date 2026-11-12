-- DATE_ADD is reserved syntax, so it is delimited to call the extension (PartiQL: 2017-01-03 03:04:05.006)
--#[date-add]
"date_add"('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

-- The current timestamp in UTC (PartiQL: the time of the query)
--#[utcnow]
utcnow();

-- True when the collection holds the value (PartiQL: true for a payload whose items include x)
--#[contains]
SELECT contains(p.items, 'x') AS c FROM payload AS p;

-- Parses a hexadecimal string (PartiQL: 12649216)
--#[hex-to-bigint]
hex_to_bigint('00C10300');

-- A leading sign is accepted, like Long.parseLong (PartiQL: -255)
--#[hex-to-bigint-negative]
hex_to_bigint('-FF');

-- The most negative BIGINT (PartiQL: -9223372036854775808)
--#[hex-to-bigint-min]
hex_to_bigint('-8000000000000000');

-- Seconds since 1970-01-01 (PartiQL: 1483326245)
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

-- A power (PartiQL: 8.0)
--#[pow]
pow(2, 3);

-- Truncates to the millisecond (PartiQL: 2020-08-13 23:20:30.123)
--#[date-trunc-millisecond]
date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the second (PartiQL: 2020-08-13 23:20:30)
--#[date-trunc-second]
date_trunc('second', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the minute (PartiQL: 2020-08-13 23:20)
--#[date-trunc-minute]
date_trunc('minute', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the hour (PartiQL: 2020-08-13 23:00)
--#[date-trunc-hour]
date_trunc('hour', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the day (PartiQL: 2020-08-13 00:00)
--#[date-trunc-day]
date_trunc('day', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Weeks start on Monday, even across a year boundary (PartiQL: 2020-12-28 00:00)
--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

-- Truncates to the month (PartiQL: 2020-08-01 00:00)
--#[date-trunc-month]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the quarter (PartiQL: 2020-07-01 00:00)
--#[date-trunc-quarter]
date_trunc('quarter', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Truncates to the year (PartiQL: 2020-01-01 00:00)
--#[date-trunc-year]
date_trunc('year', TIMESTAMP '2020-08-13 23:20:30.123456');

-- The part is case-insensitive and lowercased in the output (PartiQL: 2020-01-01 00:00)
--#[date-trunc-upper-case-part]
date_trunc('YEAR', TIMESTAMP '2020-08-13 23:20:30.123456');

-- A leap day truncated to the month (PartiQL: 2020-02-01 00:00)
--#[date-trunc-leap-day]
date_trunc('month', TIMESTAMP '2020-02-29 12:00:00');

-- A date is truncated as midnight and returns a timestamp (PartiQL: 2020-07-01 00:00)
--#[date-trunc-date]
date_trunc('quarter', DATE '2020-08-13');

-- A date truncated to the week (PartiQL: 2019-12-30 00:00)
--#[date-trunc-date-week]
date_trunc('week', DATE '2020-01-01');

-- A zoned value is truncated in its own offset; only Trino keeps the offset (PartiQL: 2020-08-13 00:00:00-05:00)
--#[date-trunc-zoned]
date_trunc('day', TIMESTAMP WITH TIME ZONE '2020-08-13 23:20:30-05:00');

-- Rejected: Trino has no microsecond (PartiQL raises an error)
--#[date-trunc-microsecond]
date_trunc('microsecond', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Rejected: Spark and Trino have no decade (PartiQL raises an error)
--#[date-trunc-decade]
date_trunc('decade', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Rejected: plural spellings such as days are not accepted (PartiQL raises an error)
--#[date-trunc-plural-part]
date_trunc('days', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Every element with - : and . separators; the fraction truncates instead of rounding into the next year (PartiQL: 2020-12-31 23:59:59.999)
--#[to-char]
to_char(TIMESTAMP '2020-12-31 23:59:59.999999', 'YYYY-MM-DD HH24:MI:SS.MS');

-- Elements also match in lower case (PartiQL: 2020-08-13 23:20:30.123)
--#[to-char-lower-case]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'yyyy-mm-dd hh24:mi:ss.ms');

-- Slash separators (PartiQL: 2020/08/13)
--#[to-char-slash]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYY/MM/DD');

-- Dot separators (PartiQL: 13.08.2020)
--#[to-char-dot]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'DD.MM.YYYY');

-- Underscore separators (PartiQL: 2020_08_13)
--#[to-char-underscore]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYY_MM_DD');

-- Semicolon separators (PartiQL: 23;20;30)
--#[to-char-semicolon]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'HH24;MI;SS');

-- Comma and space separators (PartiQL: 08, 13 2020)
--#[to-char-comma-space]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'MM, DD YYYY');

-- Digits are copied as text (PartiQL: 2020 01 08 2)
--#[to-char-digits]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYY 01 MM 2');

-- Adjacent elements without separators (PartiQL: 20200813232030123)
--#[to-char-no-separators]
to_char(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYYMMDDHH24MISSMS');

-- The 12-hour clock at noon is 12 PM (PartiQL: 12:00 PM)
--#[to-char-noon]
to_char(TIMESTAMP '2020-08-13 12:00:00', 'HH12:MI AM');

-- The 12-hour clock at midnight is 12 AM; HH is HH12 (PartiQL: 12:30 AM)
--#[to-char-midnight]
to_char(TIMESTAMP '2020-08-13 00:30:00', 'HH:MI PM');

-- Day of year on the last day of a leap year (PartiQL: 2020-366)
--#[to-char-day-of-year]
to_char(TIMESTAMP '2020-12-31 08:00:00', 'YYYY-DDD');

-- Two-digit years keep the leading zero (PartiQL: 05/03/04)
--#[to-char-two-digit-year]
to_char(TIMESTAMP '2005-03-04 05:06:07', 'YY/MM/DD');

-- FF3 is the same as MS (PartiQL: 30.045 30.045)
--#[to-char-ff3]
to_char(TIMESTAMP '2020-08-13 23:20:30.045678', 'SS.FF3 SS.MS');

-- A date is formatted as midnight (PartiQL: 2020/08/13 12:00:00 AM)
--#[to-char-date]
to_char(DATE '2020-08-13', 'yyyy/mm/dd hh12:mi:ss AM');

-- A zoned value is formatted in its own offset; only Trino keeps the offset (PartiQL: 2020-08-13 23:20)
--#[to-char-zoned]
to_char(TIMESTAMP WITH TIME ZONE '2020-08-13 23:20:30-05:00', 'YYYY-MM-DD HH24:MI');

-- Rejected: double-quoted text is quoted differently in each target (PartiQL: 2020-08-13T23:20)
--#[to-char-double-quote]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY-MM-DD"T"HH24:MI');

-- Rejected: a backtick (PartiQL: 2020`08`13)
--#[to-char-backtick]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY`MM`DD');

-- Rejected: a single quote is a quote character in Spark and Trino patterns (PartiQL: 2020'08)
--#[to-char-single-quote]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY''MM');

-- Rejected: a backslash (PartiQL: 2020\08)
--#[to-char-backslash]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY\MM');

-- Rejected: % starts a DuckDB strftime specifier (PartiQL: 2020%08)
--#[to-char-percent]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY%MM');

-- Rejected: # is reserved in Java patterns (PartiQL: 2020#08)
--#[to-char-hash]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY#MM');

-- Rejected: [ and ] mark optional sections in Java patterns (PartiQL: 2020[08])
--#[to-char-brackets]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY[MM]');

-- Rejected: a pipe (PartiQL: 2020|08)
--#[to-char-pipe]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY|MM');

-- Rejected: a plus sign (PartiQL: 2020+08)
--#[to-char-plus]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY+MM');

-- Rejected: a letter that is not a pattern (PartiQL: 23h20)
--#[to-char-letter]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'HH24hMI');

-- Rejected: names are padded and localized differently (PartiQL: 13 August    2020)
--#[to-char-month-name]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'DD Month YYYY');

-- Rejected: day names (PartiQL: Thu, 13)
--#[to-char-day-name]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'Dy, DD');

-- Rejected: fill mode (PartiQL: 13)
--#[to-char-fill-mode]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'FMDD');

-- Rejected: ordinal suffixes (PartiQL: 13th)
--#[to-char-ordinal]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'DDth');

-- Rejected: patterns outside the supported subset (PartiQL: 2020-3)
--#[to-char-quarter]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'YYYY-Q');

-- Rejected: microseconds (PartiQL: 30.000000)
--#[to-char-microseconds]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'SS.US');

-- Rejected: mixed case is text, not a pattern (PartiQL: Mm)
--#[to-char-mixed-case]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'Mm');

-- Rejected: AM and PM only in upper case (PartiQL: 11 pm)
--#[to-char-lower-case-meridiem]
to_char(TIMESTAMP '2020-08-13 23:20:30', 'HH12 am');

-- Rejected: an empty template (PartiQL: NULL)
--#[to-char-empty]
to_char(TIMESTAMP '2020-08-13 23:20:30', '');
