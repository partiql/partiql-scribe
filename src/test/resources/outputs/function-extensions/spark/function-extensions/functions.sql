-- Spark returns 2017-01-03 03:04:05.006
--#[date-add]
TIMESTAMP_NTZ '2017-01-02 03:04:05.006' + `make_interval`(0, 0, 0, 1, 0, 0, 0);

-- Current timestamp converted to UTC (not run: time-dependent)
--#[utcnow]
`convert_timezone`('UTC', `current_timestamp`());

-- True when the collection holds the value (not run: needs a table)
--#[contains]
SELECT `array_contains`(`p`.`items`, 'x') AS `c` FROM `test`.`payload` AS `p`;

-- Spark returns 12649216
--#[hex-to-bigint]
CAST(`conv`('00C10300', 16, 10) AS BIGINT);



-- Spark returns 1483326245
--#[to-unixtime]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '2017-01-02 03:04:05.006' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '2017-01-02 03:04:05.006') <> `second`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') THEN 1 ELSE 0 END;

-- Spark returns 1483326245
--#[to-unixtime-fraction]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.6') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '2017-01-02 03:04:05.6' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '2017-01-02 03:04:05.6') <> `second`(TIMESTAMP_NTZ '2017-01-02 03:04:05.6') THEN 1 ELSE 0 END;

-- Spark returns -1
--#[to-unixtime-pre-epoch]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '1969-12-31 23:59:59.6') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '1969-12-31 23:59:59.6' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '1969-12-31 23:59:59.6') <> `second`(TIMESTAMP_NTZ '1969-12-31 23:59:59.6') THEN 1 ELSE 0 END;

-- Spark returns 253402300799
--#[to-unixtime-max]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '9999-12-31 23:59:59.999999' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') <> `second`(TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') THEN 1 ELSE 0 END;

-- Spark returns 8.0
--#[pow]
`pow`(2, 3);

-- Spark returns 2020-08-13 23:20:30.123000
--#[date-trunc-millisecond]
`date_trunc`('millisecond', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-08-13 23:20:30.000000
--#[date-trunc-second]
`date_trunc`('second', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-08-13 23:20:00.000000
--#[date-trunc-minute]
`date_trunc`('minute', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-08-13 23:00:00.000000
--#[date-trunc-hour]
`date_trunc`('hour', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-08-13 00:00:00.000000
--#[date-trunc-day]
`date_trunc`('day', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-12-28 00:00:00.000000
--#[date-trunc-week]
`date_trunc`('week', TIMESTAMP_NTZ '2021-01-03 23:59:00');

-- Spark returns 2020-08-01 00:00:00.000000
--#[date-trunc-month]
`date_trunc`('month', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-07-01 00:00:00.000000
--#[date-trunc-quarter]
`date_trunc`('quarter', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-01-01 00:00:00.000000
--#[date-trunc-year]
`date_trunc`('year', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-01-01 00:00:00.000000
--#[date-trunc-upper-case-part]
`date_trunc`('year', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

-- Spark returns 2020-02-01 00:00:00.000000
--#[date-trunc-leap-day]
`date_trunc`('month', TIMESTAMP_NTZ '2020-02-29 12:00:00');

-- Spark returns 2020-07-01 00:00:00.000000
--#[date-trunc-date]
`date_trunc`('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP_NTZ));

-- Spark returns 2019-12-30 00:00:00.000000
--#[date-trunc-date-week]
`date_trunc`('week', CAST(DATE '2020-01-01' AS TIMESTAMP_NTZ));

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

-- Spark returns 2020-12-31 23:59:59.999
--#[to-char]
`date_format`(TIMESTAMP_NTZ '2020-12-31 23:59:59.999999', 'yyyy-MM-dd HH:mm:ss.SSS');

-- Spark returns 2020-08-13 23:20:30.123
--#[to-char-lower-case]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyy-MM-dd HH:mm:ss.SSS');

-- Spark returns 2020/08/13
--#[to-char-slash]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyy/MM/dd');

-- Spark returns 13.08.2020
--#[to-char-dot]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'dd.MM.yyyy');

-- Spark returns 2020_08_13
--#[to-char-underscore]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyy_MM_dd');

-- Spark returns 23;20;30
--#[to-char-semicolon]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'HH;mm;ss');

-- Spark returns 08, 13 2020
--#[to-char-comma-space]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'MM, dd yyyy');

-- Spark returns 2020 01 08 2
--#[to-char-digits]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyy 01 MM 2');

-- Spark returns 20200813232030123
--#[to-char-no-separators]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyyMMddHHmmssSSS');

-- Spark returns 12:00 PM
--#[to-char-noon]
`date_format`(TIMESTAMP_NTZ '2020-08-13 12:00:00', 'hh:mm a');

-- Spark returns 12:30 AM
--#[to-char-midnight]
`date_format`(TIMESTAMP_NTZ '2020-08-13 00:30:00', 'hh:mm a');

-- Spark returns 2020-366
--#[to-char-day-of-year]
`date_format`(TIMESTAMP_NTZ '2020-12-31 08:00:00', 'yyyy-DDD');

-- Spark returns 05/03/04
--#[to-char-two-digit-year]
`date_format`(TIMESTAMP_NTZ '2005-03-04 05:06:07', 'yy/MM/dd');

-- Spark returns 30.045 30.045
--#[to-char-ff3]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.045678', 'ss.SSS ss.SSS');

-- Spark returns 2020/08/13 12:00:00 AM
--#[to-char-date]
`date_format`(CAST(DATE '2020-08-13' AS TIMESTAMP_NTZ), 'yyyy/MM/dd hh:mm:ss a');

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
