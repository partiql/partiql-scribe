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

-- Spark does not support time
-- --#[rel-aggregation-max-min-time]
-- SELECT `max`(`T`.`col_time`) AS `_1`, `min`(`T`.`col_time`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

-- Spark does not support time
-- --#[rel-aggregation-max-min-timez]
-- SELECT `max`(`T`.`col_timez`) AS `_1`, `min`(`T`.`col_timez`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-timestamp]
SELECT `max`(`T`.`col_timestamp`) AS `_1`, `min`(`T`.`col_timestamp`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-timestampz]
SELECT `max`(`T`.`col_timestampz`) AS `_1`, `min`(`T`.`col_timestampz`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-max-min-string]
SELECT `max`(`T`.`col_string`) AS `_1`, `min`(`T`.`col_string`) AS `_2` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[rel-aggregation-distinct-00]
SELECT `sum`(DISTINCT `T`.`b`) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-null-01]
SELECT `sum`(NULL) AS `_1` FROM `default`.`T` AS `T`;

--#[rel-aggregation-multiple-00]
SELECT `count`(1) AS `_1`, `sum`(`T`.`b`) AS `_2`, `avg`(`T`.`b`) AS `_3`, `min`(`T`.`b`) AS `_4`, `max`(`T`.`b`) AS `_5` FROM `default`.`T` AS `T`;

--#[rel-aggregation-multiple-01]
SELECT `T`.`a` AS `a`, `count`(1) AS `_1`, `sum`(`T`.`b`) AS `_2`, `avg`(`T`.`b`) AS `_3` FROM `default`.`T` AS `T` GROUP BY `T`.`a`;

--#[rel-aggregation-multiple-group-00]
SELECT `T`.`a` AS `a`, `T`.`c` AS `c`, `count`(1) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a`, `T`.`c`;

--#[rel-aggregation-multiple-group-01]
SELECT `T`.`a` AS `a`, `T`.`c` AS `c`, `sum`(`T`.`b`) AS `_1`, `avg`(`T`.`b`) AS `_2` FROM `default`.`T` AS `T` GROUP BY `T`.`a`, `T`.`c`;

--#[rel-aggregation-multiple-group-02]
SELECT `sum`(`T`.`b`) AS `_1`, `T`.`a` AS `a`, `avg`(`T`.`b`) AS `_2`, `T`.`c` AS `c` FROM `default`.`T` AS `T` GROUP BY `T`.`a`, `T`.`c`;

--#[rel-aggregation-where-group-00]
SELECT `T`.`a` AS `a`, `count`(1) AS `_1` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0 GROUP BY `T`.`a`;

