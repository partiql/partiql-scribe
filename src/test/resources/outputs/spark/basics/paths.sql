-- ----------------------------------------
--  Spark Path Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-00]
-- tuple navigation
SELECT `t`.`x`.`y` AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-01]
-- array navigation with literal
SELECT `t`.`x`[0] AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-02]
-- tuple navigation with array notation (1)
SELECT `t`.`x`.`y` AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-03]
-- tuple navigation with array notation (2)
SELECT `t`.`x`.`y` AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-04]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-sfw-05]
SELECT `t`.`x`.`y`[0].`y` AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-06]
SELECT `t`.`x`.`y`[0].`y` AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-07]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`.`y`[0][CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-08]
SELECT `t`.`x`.`y`.`y`.`y` AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-09]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`.`y`.`y`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-10]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`.`y`.`y`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

--#[paths-sfw-11]
SELECT `t`.`x`[0].`y`.`y` AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-12]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`[0].`y`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-13]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`[0].`y`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

-- #[paths-sfw-14]
-- Spark path step must an identifier, e.g. `x`.`y`
-- SELECT `t`.`x`.`y`.`y`[CAST(`t`.`z` AS STRING)] AS `v` FROM `default`.`T` AS `t`;

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-sfw-15]
SELECT `t`.`x`[(0 + 1)] AS `v` FROM `default`.`T` AS `t`;
