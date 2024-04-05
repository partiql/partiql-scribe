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
SELECT `year`(`T`.`timestamp_2`) - `year`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-16]
SELECT `month`(`T`.`timestamp_2`) - `month`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-17]
SELECT `day`(`T`.`timestamp_2`) - `day`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-18]
SELECT `hour`(`T`.`timestamp_2`) - `hour`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-19]
SELECT `minute`(`T`.`timestamp_2`) - `minute`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-20]
SELECT `second`(`T`.`timestamp_2`) - `second`(`T`.`timestamp_1`) AS `_1` FROM `default`.`T` AS `T`;
