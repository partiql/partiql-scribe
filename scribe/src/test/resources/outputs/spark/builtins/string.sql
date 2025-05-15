--#[trim-00]
SELECT trim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-01]
SELECT trim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-02]
SELECT ltrim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-03]
SELECT rtrim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-04]
SELECT trim(BOTH 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-05]
SELECT trim(LEADING 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-06]
SELECT trim(TRAILING 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;
