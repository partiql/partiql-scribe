--#[in-00]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`b`, array(1, 2));

--#[in-01]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`b`, array(1, 2));

--#[in-02]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`b`, array(1, 2));

--#[in-03]
SELECT `T`.* FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`b`, array(1, 2));

-- #[in-04]
-- SELECT `T`.* FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`b`, array(1, 2));
