--#[in-00]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-01]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-02]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-03]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);

--#[in-04]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);

--#[in-05]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);
