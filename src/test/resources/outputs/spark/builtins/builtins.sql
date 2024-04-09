--#[datetime-08]
SELECT CURRENT_DATE AS `CURRENT_DATE` FROM `default`.`T` AS `T`;

--#[datetime-09]
SELECT CURRENT_DATE + `make_interval`(0, 0, 0, 0, 0, 0, 5) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-10]
SELECT CURRENT_DATE + `make_interval`(0, 0, 0, 0, 0, 5, 0) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-11]
SELECT CURRENT_DATE + `make_interval`(0, 0, 0, 0, 5, 0, 0) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-12]
SELECT CURRENT_DATE + `make_interval`(0, 0, 0, 5, 0, 0, 0) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-13]
SELECT CURRENT_DATE + `make_interval`(0, 5, 0, 0, 0, 0, 0) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-14]
SELECT CURRENT_DATE + `make_interval`(5, 0, 0, 0, 0, 0, 0) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-15]
SELECT `floor`(`months_between`(`T`.`timestamp_2`, `T`.`timestamp_1`) / 12) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-16]
SELECT `floor`(`months_between`(`T`.`timestamp_2`, `T`.`timestamp_1`)) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-17]
SELECT `date_diff`(`T`.`timestamp_2`, `T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-18]
SELECT `floor`((`unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`)) / 3600) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-19]
SELECT `floor`((`unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`)) / 60) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-20]
SELECT `unix_timestamp`(`T`.`timestamp_2`) - `unix_timestamp`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;
