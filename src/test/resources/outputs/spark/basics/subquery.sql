-- Top level expressions are not supported in Spark

-- Comparison operators with subqueries
--#[subquery-03]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` = (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

--#[subquery-04]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

--#[subquery-05]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` < (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

--#[subquery-06]
SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE `T`.`b` >= (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

--#[subquery-07]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` <= (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

--#[subquery-08]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` <> (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 1);

-- IN collection subquery
--#[subquery-09]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- NOT IN subquery
--#[subquery-10]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- EXISTS subquery
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-11]
-- SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE EXISTS (SELECT `t2`.`v` AS `v` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0);

-- NOT EXISTS subquery
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-12]
-- SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE NOT EXISTS (SELECT `t2`.`a` AS `a` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0 AND `t2`.`b` > `0`);

-- Nested subqueries
--#[subquery-13]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` = 0));

-- Subquery in SELECT clause
--#[subquery-14]
SELECT `T`.`b` AS `b`, (SELECT `t2`.`v` AS `v` FROM `default`.`T` AS `t2` WHERE `t2`.`b` = 0) AS `match_v` FROM `default`.`T` AS `T`;

-- Multiple subqueries in WHERE
-- TODO Exists not implemented in scribe yet. Tracking with https://github.com/partiql/partiql-scribe/issues/104
-- #[subquery-15]
-- SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0) AND EXISTS (SELECT `t2`.`a` AS `a` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0);

-- Correlated subquery with aggregation
--#[subquery-16]
SELECT `t1`.`a` AS `a` FROM `default`.`T` AS `t1` WHERE `t1`.`b` > (SELECT `avg`(`t2`.`b`) AS `_1` FROM `default`.`T` AS `t2` WHERE `t1`.`a` = `t2`.`a`);

-- Correlated IN subquery
--#[subquery-17]
SELECT `t1`.`b` AS `b` FROM `default`.`T` AS `t1` WHERE `t1`.`b` IN (SELECT `t2`.`b` AS `b` FROM `default`.`T` AS `t2` WHERE `t2`.`a` = `t1`.`a`);

-- Correlated subquery in SELECT
--#[subquery-18]
SELECT `t1`.`a` AS `a`, (SELECT `max`(`t2`.`b`) AS `_1` FROM `default`.`T` AS `t2` WHERE `t2`.`a` = `t1`.`a`) AS `max_b` FROM `default`.`T` AS `t1`;

-- Multiple correlated subqueries
--#[subquery-19]
SELECT `t1`.`a` AS `a` FROM `default`.`T` AS `t1` WHERE (`t1`.`b` > (SELECT `avg`(`t2`.`b`) AS `_1` FROM `default`.`T` AS `t2` WHERE `t2`.`a` = `t1`.`a`)) AND `t1`.`v` IN (SELECT `t3`.`v` AS `v` FROM `default`.`T` AS `t3` WHERE `t3`.`b` = `t1`.`b`);

-- Nested correlated subqueries - not supported
-- #[subquery-20]
-- SELECT `t1`.`a` AS `a` FROM `default`.`T` AS `t1` WHERE `t1`.`b` > (SELECT `avg`(`t2`.`b`) AS `_1` FROM `default`.`T` AS `t2` WHERE (`t2`.`a` = `t1`.`a`) AND (`t2`.`b` > (SELECT `min`(`t3`.`b`) AS `_1` FROM `default`.`T` AS `t3` WHERE `t3`.`a` = `t1`.`a`)));

-- Correlated with multiple references
--#[subquery-21]
SELECT `t1`.`a` AS `a` FROM `default`.`T` AS `t1` WHERE (SELECT `sum`(`t2`.`b`) AS `_1` FROM `default`.`T` AS `t2` WHERE (`t2`.`a` = `t1`.`a`) AND (`t2`.`v` = `t1`.`v`)) > CAST(10 AS BIGINT);
