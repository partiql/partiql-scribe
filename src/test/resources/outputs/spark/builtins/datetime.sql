--#[datetime-21]
SELECT TIMESTAMP_NTZ '2017-01-02 03:04:05.006' + `make_interval`(0, 0, 0, 0, 0, 0, 1) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-22]
SELECT `unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:20.006') - `unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') AS `_1` FROM `default`.`T` AS `T`;

-- Check UTCNOW()
--#[datetime-23]
SELECT `convert_timezone`('UTC', `current_timestamp`()) AS `_1` FROM `default`.`T` AS `T`;
