--#[date-add]
TIMESTAMP_NTZ '2017-01-02 03:04:05.006' + `make_interval`(0, 0, 0, 1, 0, 0, 0);

--#[utcnow]
`convert_timezone`('UTC', `current_timestamp`());

--#[contains]
SELECT `array_contains`(`p`.`items`, 'x') AS `c` FROM `test`.`payload` AS `p`;

--#[hex-to-bigint]
CAST(`conv`('00C10300', 16, 10) AS BIGINT);



--#[to-unixtime]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '2017-01-02 03:04:05.006' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '2017-01-02 03:04:05.006') <> `second`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') THEN 1 ELSE 0 END;

--#[to-unixtime-fraction]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.6') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '2017-01-02 03:04:05.6' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '2017-01-02 03:04:05.6') <> `second`(TIMESTAMP_NTZ '2017-01-02 03:04:05.6') THEN 1 ELSE 0 END;

--#[to-unixtime-pre-epoch]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '1969-12-31 23:59:59.6') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '1969-12-31 23:59:59.6' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '1969-12-31 23:59:59.6') <> `second`(TIMESTAMP_NTZ '1969-12-31 23:59:59.6') THEN 1 ELSE 0 END;

--#[to-unixtime-max]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') AS BIGINT) - CASE WHEN TIMESTAMP_NTZ '9999-12-31 23:59:59.999999' < TIMESTAMP_NTZ '1970-01-01 00:00:00' AND `date_part`('SECOND', TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') <> `second`(TIMESTAMP_NTZ '9999-12-31 23:59:59.999999') THEN 1 ELSE 0 END;

--#[pow]
`pow`(2, 3);

--#[date-trunc]
`date_trunc`('month', TIMESTAMP_NTZ '2020-08-13 23:20:30.123456');

--#[date-trunc-date]
`date_trunc`('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP_NTZ));

--#[date-trunc-week]
`date_trunc`('week', TIMESTAMP_NTZ '2021-01-03 23:59:00');

--#[date-trunc-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[date-trunc-unsupported-part]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char]
`date_format`(TIMESTAMP_NTZ '2020-12-31 23:59:59.999999', 'yyyy-MM-dd HH:mm:ss.SSS');

--#[to-char-date]
`date_format`(CAST(DATE '2020-08-13' AS TIMESTAMP_NTZ), 'yyyy/MM/dd hh:mm:ss a');

--#[to-char-no-separators]
`date_format`(TIMESTAMP_NTZ '2020-08-13 23:20:30.123456', 'yyyyMMddHHmmssSSS');

--#[to-char-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char-unsupported]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char-mixed-case]
[ScribeException{code=UNSUPPORTED_OPERATION}];
