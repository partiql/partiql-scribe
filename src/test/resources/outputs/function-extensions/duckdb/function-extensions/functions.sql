-- DuckDB returns 2017-01-03 03:04:05.006
--#[date-add]
date_add(TIMESTAMP '2017-01-02 03:04:05.006', to_days(1));

-- Current timestamp converted to UTC (not run: time-dependent)
--#[utcnow]
timezone('UTC', current_timestamp);

-- True when the collection holds the value (not run: needs a table)
--#[contains]
SELECT contains("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

-- DuckDB returns 12649216
--#[hex-to-bigint]
CAST(CASE WHEN left('00C10300', 1) = '-' THEN -CAST(CAST('0x' || substring('00C10300', 2) AS UBIGINT) AS HUGEINT) WHEN left('00C10300', 1) = '+' THEN CAST('0x' || substring('00C10300', 2) AS UBIGINT) ELSE CAST('0x' || '00C10300' AS UBIGINT) END AS BIGINT);

-- DuckDB returns -255
--#[hex-to-bigint-negative]
CAST(CASE WHEN left('-FF', 1) = '-' THEN -CAST(CAST('0x' || substring('-FF', 2) AS UBIGINT) AS HUGEINT) WHEN left('-FF', 1) = '+' THEN CAST('0x' || substring('-FF', 2) AS UBIGINT) ELSE CAST('0x' || '-FF' AS UBIGINT) END AS BIGINT);

-- DuckDB returns -9223372036854775808
--#[hex-to-bigint-min]
CAST(CASE WHEN left('-8000000000000000', 1) = '-' THEN -CAST(CAST('0x' || substring('-8000000000000000', 2) AS UBIGINT) AS HUGEINT) WHEN left('-8000000000000000', 1) = '+' THEN CAST('0x' || substring('-8000000000000000', 2) AS UBIGINT) ELSE CAST('0x' || '-8000000000000000' AS UBIGINT) END AS BIGINT);

-- DuckDB returns 1483326245
--#[to-unixtime]
CAST(epoch(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.006')) AS BIGINT);

-- DuckDB returns 1483326245
--#[to-unixtime-fraction]
CAST(epoch(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.6')) AS BIGINT);

-- DuckDB returns -1
--#[to-unixtime-pre-epoch]
CAST(epoch(date_trunc('second', TIMESTAMP '1969-12-31 23:59:59.6')) AS BIGINT);

-- DuckDB returns 253402300799
--#[to-unixtime-max]
CAST(epoch(date_trunc('second', TIMESTAMP '9999-12-31 23:59:59.999999')) AS BIGINT);

-- DuckDB returns 8.0
--#[pow]
"pow"(2, 3);

-- DuckDB returns 2020-08-13 23:20:30.123000
--#[date-trunc-millisecond]
date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-08-13 23:20:30.000000
--#[date-trunc-second]
date_trunc('second', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-08-13 23:20:00.000000
--#[date-trunc-minute]
date_trunc('minute', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-08-13 23:00:00.000000
--#[date-trunc-hour]
date_trunc('hour', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-08-13 00:00:00.000000
--#[date-trunc-day]
date_trunc('day', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-12-28 00:00:00.000000
--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

-- DuckDB returns 2020-08-01 00:00:00.000000
--#[date-trunc-month]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-07-01 00:00:00.000000
--#[date-trunc-quarter]
date_trunc('quarter', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-01-01 00:00:00.000000
--#[date-trunc-year]
date_trunc('year', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-01-01 00:00:00.000000
--#[date-trunc-upper-case-part]
date_trunc('year', TIMESTAMP '2020-08-13 23:20:30.123456');

-- DuckDB returns 2020-02-01 00:00:00.000000
--#[date-trunc-leap-day]
date_trunc('month', TIMESTAMP '2020-02-29 12:00:00');

-- DuckDB returns 2020-07-01 00:00:00.000000
--#[date-trunc-date]
date_trunc('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP));

-- DuckDB returns 2019-12-30 00:00:00.000000
--#[date-trunc-date-week]
date_trunc('week', CAST(DATE '2020-01-01' AS TIMESTAMP));

-- Rejected: the target uses the session time zone instead of the value's own offset
--#[date-trunc-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: Trino has no microsecond
--#[date-trunc-microsecond]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: Spark and Trino have no decade
--#[date-trunc-decade]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: plural spellings such as days are not accepted
--#[date-trunc-plural-part]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- DuckDB returns 2020-12-31 23:59:59.999
--#[to-char]
strftime(TIMESTAMP '2020-12-31 23:59:59.999999', '%Y-%m-%d %H:%M:%S.%g');

-- DuckDB returns 2020-08-13 23:20:30.123
--#[to-char-lower-case]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%Y-%m-%d %H:%M:%S.%g');

-- DuckDB returns 2020/08/13
--#[to-char-slash]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%Y/%m/%d');

-- DuckDB returns 13.08.2020
--#[to-char-dot]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%d.%m.%Y');

-- DuckDB returns 2020_08_13
--#[to-char-underscore]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%Y_%m_%d');

-- DuckDB returns 23;20;30
--#[to-char-semicolon]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%H;%M;%S');

-- DuckDB returns 08, 13 2020
--#[to-char-comma-space]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%m, %d %Y');

-- DuckDB returns 2020 01 08 2
--#[to-char-digits]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%Y 01 %m 2');

-- DuckDB returns 20200813232030123
--#[to-char-no-separators]
strftime(TIMESTAMP '2020-08-13 23:20:30.123456', '%Y%m%d%H%M%S%g');

-- DuckDB returns 12:00 PM
--#[to-char-noon]
strftime(TIMESTAMP '2020-08-13 12:00:00', '%I:%M %p');

-- DuckDB returns 12:30 AM
--#[to-char-midnight]
strftime(TIMESTAMP '2020-08-13 00:30:00', '%I:%M %p');

-- DuckDB returns 2020-366
--#[to-char-day-of-year]
strftime(TIMESTAMP '2020-12-31 08:00:00', '%Y-%j');

-- DuckDB returns 05/03/04
--#[to-char-two-digit-year]
strftime(TIMESTAMP '2005-03-04 05:06:07', '%y/%m/%d');

-- DuckDB returns 30.045 30.045
--#[to-char-ff3]
strftime(TIMESTAMP '2020-08-13 23:20:30.045678', '%S.%g %S.%g');

-- DuckDB returns 2020/08/13 12:00:00 AM
--#[to-char-date]
strftime(CAST(DATE '2020-08-13' AS TIMESTAMP), '%Y/%m/%d %I:%M:%S %p');

-- Rejected: the target uses the session time zone instead of the value's own offset
--#[to-char-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: double-quoted text is quoted differently in each target
--#[to-char-double-quote]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a backtick
--#[to-char-backtick]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a single quote is a quote character in Spark and Trino patterns
--#[to-char-single-quote]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a backslash
--#[to-char-backslash]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: % starts a DuckDB strftime specifier
--#[to-char-percent]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: # is reserved in Java patterns
--#[to-char-hash]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: [ and ] mark optional sections in Java patterns
--#[to-char-brackets]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a pipe
--#[to-char-pipe]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a plus sign
--#[to-char-plus]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: a letter that is not a pattern
--#[to-char-letter]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: names are padded and localized differently
--#[to-char-month-name]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: day names
--#[to-char-day-name]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: fill mode
--#[to-char-fill-mode]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: ordinal suffixes
--#[to-char-ordinal]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: patterns outside the supported subset
--#[to-char-quarter]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: microseconds
--#[to-char-microseconds]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: mixed case is text, not a pattern
--#[to-char-mixed-case]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: AM and PM only in upper case
--#[to-char-lower-case-meridiem]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: an empty template
--#[to-char-empty]
[ScribeException{code=UNSUPPORTED_OPERATION}];
