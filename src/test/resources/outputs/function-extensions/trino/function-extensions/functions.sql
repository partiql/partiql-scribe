-- Trino returns 2017-01-03 03:04:05.006
--#[date-add]
date_add('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

-- Current timestamp converted to UTC (not run: time-dependent)
--#[utcnow]
at_timezone(current_timestamp, 'UTC');

-- True when the collection holds the value (not run: needs a table)
--#[contains]
SELECT "contains"("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

-- Trino returns 12649216
--#[hex-to-bigint]
from_base('00C10300', 16);

-- Trino returns -255
--#[hex-to-bigint-negative]
from_base('-FF', 16);

-- Trino returns -9223372036854775808
--#[hex-to-bigint-min]
from_base('-8000000000000000', 16);

-- Trino returns 1483326245
--#[to-unixtime]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.006'), 'UTC')) AS BIGINT);

-- Trino returns 1483326245
--#[to-unixtime-fraction]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.6'), 'UTC')) AS BIGINT);

-- Trino returns -1
--#[to-unixtime-pre-epoch]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '1969-12-31 23:59:59.6'), 'UTC')) AS BIGINT);

-- Trino returns 253402300799
--#[to-unixtime-max]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '9999-12-31 23:59:59.999999'), 'UTC')) AS BIGINT);

-- Trino returns 8.0E0
--#[pow]
"pow"(2, 3);

-- Trino returns 2020-08-13 23:20:30.123000
--#[date-trunc-millisecond]
date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-08-13 23:20:30.000000
--#[date-trunc-second]
date_trunc('second', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-08-13 23:20:00.000000
--#[date-trunc-minute]
date_trunc('minute', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-08-13 23:00:00.000000
--#[date-trunc-hour]
date_trunc('hour', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-08-13 00:00:00.000000
--#[date-trunc-day]
date_trunc('day', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-12-28 00:00:00
--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

-- Trino returns 2020-08-01 00:00:00.000000
--#[date-trunc-month]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-07-01 00:00:00.000000
--#[date-trunc-quarter]
date_trunc('quarter', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-01-01 00:00:00.000000
--#[date-trunc-year]
date_trunc('year', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-01-01 00:00:00.000000
--#[date-trunc-upper-case-part]
date_trunc('year', TIMESTAMP '2020-08-13 23:20:30.123456');

-- Trino returns 2020-02-01 00:00:00
--#[date-trunc-leap-day]
date_trunc('month', TIMESTAMP '2020-02-29 12:00:00');

-- Trino returns 2020-07-01 00:00:00.000
--#[date-trunc-date]
date_trunc('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP));

-- Trino returns 2019-12-30 00:00:00.000
--#[date-trunc-date-week]
date_trunc('week', CAST(DATE '2020-01-01' AS TIMESTAMP));

-- Trino returns 2020-08-13 00:00:00 -05:00
--#[date-trunc-zoned]
date_trunc('day', TIMESTAMP '2020-08-13 23:20:30-05:00');

-- Rejected: Trino has no microsecond
--#[date-trunc-microsecond]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: Spark and Trino have no decade
--#[date-trunc-decade]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Rejected: plural spellings such as days are not accepted
--#[date-trunc-plural-part]
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- Trino returns 2020-12-31 23:59:59.999
--#[to-char]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-12-31 23:59:59.999999'), 'yyyy-MM-dd HH:mm:ss.SSS');

-- Trino returns 2020-08-13 23:20:30.123
--#[to-char-lower-case]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyy-MM-dd HH:mm:ss.SSS');

-- Trino returns 2020/08/13
--#[to-char-slash]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyy/MM/dd');

-- Trino returns 13.08.2020
--#[to-char-dot]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'dd.MM.yyyy');

-- Trino returns 2020_08_13
--#[to-char-underscore]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyy_MM_dd');

-- Trino returns 23;20;30
--#[to-char-semicolon]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'HH;mm;ss');

-- Trino returns 08, 13 2020
--#[to-char-comma-space]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'MM, dd yyyy');

-- Trino returns 2020 01 08 2
--#[to-char-digits]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyy 01 MM 2');

-- Trino returns 20200813232030123
--#[to-char-no-separators]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyyMMddHHmmssSSS');

-- Trino returns 12:00 PM
--#[to-char-noon]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 12:00:00'), 'hh:mm a');

-- Trino returns 12:30 AM
--#[to-char-midnight]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 00:30:00'), 'hh:mm a');

-- Trino returns 2020-366
--#[to-char-day-of-year]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-12-31 08:00:00'), 'yyyy-DDD');

-- Trino returns 05/03/04
--#[to-char-two-digit-year]
format_datetime(date_trunc('millisecond', TIMESTAMP '2005-03-04 05:06:07'), 'yy/MM/dd');

-- Trino returns 30.045 30.045
--#[to-char-ff3]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.045678'), 'ss.SSS ss.SSS');

-- Trino returns 2020/08/13 12:00:00 AM
--#[to-char-date]
format_datetime(date_trunc('millisecond', CAST(DATE '2020-08-13' AS TIMESTAMP)), 'yyyy/MM/dd hh:mm:ss a');

-- Trino returns 2020-08-13 23:20
--#[to-char-zoned]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30-05:00'), 'yyyy-MM-dd HH:mm');

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
