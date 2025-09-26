--#[datetime-08]
SELECT CURRENT_DATE AS `CURRENT_DATE` FROM `default`.`T` AS `T`;

--#[datetime-09]
SELECT INTERVAL '5' SECOND + `T`.`timestamp_1` AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-10]
SELECT INTERVAL '5' MINUTE + `T`.`timestamp_1` AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-11]
SELECT INTERVAL '5' HOUR + `T`.`timestamp_1` AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-12]
SELECT INTERVAL '5' DAY + CURRENT_DATE AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-13]
SELECT INTERVAL '5' MONTH + CURRENT_DATE AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-14]
SELECT INTERVAL '5' YEAR + CURRENT_DATE AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-15]
SELECT CAST(`months_between`(`T`.`timestamp_2`, `T`.`timestamp_1`) / 12 AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-16]
SELECT CAST(`months_between`(`T`.`timestamp_2`, `T`.`timestamp_1`) AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-17]
SELECT `date_diff`(`T`.`timestamp_2`, `T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-18]
SELECT CAST((`unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`)) / 3600 AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-19]
SELECT CAST((`unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`)) / 60 AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

-- example
-- SELECT CAST((`unix_timestamp`(TIMESTAMP'1997-02-28T10:29:00Z') - `unix_timestamp`(TIMESTAMP'1997-02-28T10:30:01Z')) / 60 AS BIGINT);

--#[datetime-20]
SELECT `unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;
