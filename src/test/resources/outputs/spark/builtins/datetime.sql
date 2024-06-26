--#[datetime-21]
SELECT TIMESTAMP '2017-01-02 03:04:05.006' + `make_interval`(0, 0, 0, 0, 0, 0, 1) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-22]
SELECT `unix_timestamp`(TIMESTAMP '2017-01-02 03:04:20.006') - `unix_timestamp`(TIMESTAMP '2017-01-02 03:04:05.006') AS `_1` FROM `default`.`T` AS `T`;
