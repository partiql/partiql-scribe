--#[date-add]
TIMESTAMP_NTZ '2017-01-02 03:04:05.006' + `make_interval`(0, 0, 0, 1, 0, 0, 0);

--#[utcnow]
`convert_timezone`('UTC', `current_timestamp`());

--#[contains]
SELECT `array_contains`(`p`.`items`, 'x') AS `c` FROM `test`.`payload` AS `p`;

--#[hex-to-bigint]
CAST(`conv`('00C10300', 16, 10) AS BIGINT);

--#[to-unixtime]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') AS BIGINT);

--#[to-unixtime-fraction]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.6') AS BIGINT);

--#[to-unixtime-pre-epoch]
CAST(`unix_timestamp`(TIMESTAMP_NTZ '1969-12-31 23:59:59.6') AS BIGINT);

--#[pow]
`pow`(2, 3);
