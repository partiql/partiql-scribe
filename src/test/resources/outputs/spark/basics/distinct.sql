--#[distinct-00]
SELECT DISTINCT `T`.`a` AS `a` FROM `default`.`T` AS `T`;

--#[distinct-01]
SELECT DISTINCT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T`;

--#[distinct-02]
(SELECT DISTINCT `T`.`a` AS `a` FROM `default`.`T` AS `T`) UNION DISTINCT (SELECT DISTINCT `T`.`a` AS `a` FROM `default`.`T` AS `T`);

--#[distinct-03]
(SELECT DISTINCT `T`.`a` AS `a` FROM `default`.`T` AS `T`) UNION DISTINCT (SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T`);

--#[distinct-04]
(SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T`) UNION DISTINCT (SELECT DISTINCT `T`.`a` AS `a` FROM `default`.`T` AS `T`);
