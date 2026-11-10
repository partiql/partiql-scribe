--#[quoted-identifiers-00]
SELECT `t`.`c` AS `a``b` FROM `default`.`T` AS `t`;

--#[quoted-identifiers-01]
SELECT `t`.`c` AS `a"b` FROM `default`.`T` AS `t`;

--#[quoted-identifiers-02]
SELECT `t`.`x`.`a``b` AS `v` FROM `default`.`T` AS `t`;

--#[quoted-identifiers-03]
SELECT `t`.`x`.`a"b` AS `v` FROM `default`.`T` AS `t`;

--#[quoted-identifiers-04]
SELECT `v`.`a``b` AS `a``b` FROM (SELECT `t`.`c` AS `a``b` FROM `default`.`T` AS `t`) AS `v`;
