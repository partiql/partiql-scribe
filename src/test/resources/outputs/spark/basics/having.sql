--#[having-00]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` GROUP BY `T`.`b` HAVING `count`(`T`.`b`) > CAST(1 AS BIGINT);

--#[having-01]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` < 1 GROUP BY `T`.`b` HAVING `count`(`T`.`b`) > CAST(1 AS BIGINT);

--#[having-02]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `count`(1) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a`, `T`.`b` HAVING (`count`(1) > CAST(1 AS BIGINT)) AND (`T`.`b` > 5);

--#[having-03]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b` FROM `default`.`T` AS `T` GROUP BY `T`.`a`, `T`.`b` HAVING (`T`.`a` = true) AND (`T`.`b` < 20);

