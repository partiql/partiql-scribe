--#[cast-00]
SELECT CAST('1' AS INT) AS `_1` FROM `default`.`T` AS `T`;

--#[cast-01]
SELECT CAST('1' AS INT) AS `_1` FROM `default`.`T` AS `T`;

--#[cast-02]
SELECT CAST('1' AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

--#[cast-03]
SELECT CAST('1' AS BIGINT) AS `_1` FROM `default`.`T` AS `T`;

-- #[cast-04]
-- SELECT CAST(1 AS REAL) AS `_1` FROM `default`.`T` AS `T`;

--#[cast-05]
SELECT CAST(1 AS DOUBLE) AS `_1` FROM `default`.`T` AS `T`;

-- 0 scale is provided by PLK 0.14.9's AST -> Plan conversion
--#[cast-06]
SELECT CAST(1 AS DECIMAL) AS `res` FROM `default`.`T` AS `T`;

--#[cast-07]
SELECT CAST(1 AS DECIMAL(5,0)) AS `res` FROM `default`.`T` AS `T`;

--#[cast-08]
SELECT CAST(1 AS DECIMAL(5,2)) AS `res` FROM `default`.`T` AS `T`;
