--#[rel-aggregation-count-00]
SELECT `count`(1) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-count-01]
SELECT `count`(1) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-count-02]
SELECT `count`(`T`.`a`) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-count-03]
SELECT `count`(1) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a`;

--#[rel-aggregation-max-00]
SELECT `max`(`T`.`b`) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-max-01]
SELECT `max`(`T`.`b`) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a`;

--#[rel-aggregation-max-min-date]
SELECT `max`(`T`.`col_date`) AS `_1`, `min`(`T`.`col_date`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-time]
SELECT `max`(`T`.`col_time`) AS `_1`, `min`(`T`.`col_time`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-timez]
SELECT `max`(`T`.`col_timez`) AS `_1`, `min`(`T`.`col_timez`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-timestamp]
SELECT `max`(`T`.`col_timestamp`) AS `_1`, `min`(`T`.`col_timestamp`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-timestampz]
SELECT `max`(`T`.`col_timestampz`) AS `_1`, `min`(`T`.`col_timestampz`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-string]
SELECT `max`(`T`.`col_string`) AS `_1`, `min`(`T`.`col_string`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;
