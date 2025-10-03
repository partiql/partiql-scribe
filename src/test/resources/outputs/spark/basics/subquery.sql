-- #[subquery-00]
-- Spark does not support top level expression.
-- 1 = (SELECT b FROM T);

-- #[subquery-01]
-- Spark does not support top level expression.
-- (false, 1) = (SELECT a, b FROM T);

-- #[subquery-02]
-- Spark does not support top level expression.
-- 100 = (SELECT COUNT(*) FROM T);

-- Comparison operators with subqueries
--#[subquery-03]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` = (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

--#[subquery-04]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

--#[subquery-05]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` < (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

--#[subquery-06]
SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE `T`.`b` >= (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

--#[subquery-07]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` <= (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

--#[subquery-08]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` <> (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- IN collection subquery
--#[subquery-09]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- NOT IN subquery
--#[subquery-10]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- EXISTS subquery
-- #[subquery-11]
-- SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE EXISTS (SELECT `t2`.`v` AS `v` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0);

-- NOT EXISTS subquery
-- #[subquery-12]
-- SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE NOT EXISTS (SELECT `t2`.`a` AS `a` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0 AND `t2`.`b` > `0`);

-- UNIQUE subquery
-- #[subquery-13]
-- SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE UNIQUE (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- Nested subqueries
--#[subquery-14]
SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0));

-- Subquery in SELECT clause
--#[subquery-15]
SELECT `T`.`b` AS `b`, (SELECT `t2`.`v` AS `v` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0) AS `match_v` FROM `default`.`T` AS `T`;

-- Multiple subqueries in WHERE
-- #[subquery-16]
-- SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0) AND EXISTS (SELECT `t2`.`a` AS `a` FROM `default`.`T` AS `t2` WHERE `t2`.`b` > 0);

-- Correlated subquery
--#[subquery-17]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` > (SELECT `t2`.`b` AS `b` FROM `default`.`T` AS `t2` WHERE `t2`.`a` = `t2`.`b`);

-- ANY/SOME with subquery
-- #[subquery-18]
-- SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > ANY (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- #[subquery-19]
-- SELECT `T`.`v` AS `v` FROM `default`.`T` AS `T` WHERE `T`.`b` = SOME (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);

-- ALL with subquery
-- #[subquery-20]
-- SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` WHERE `T`.`b` > ALL (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T` WHERE `T`.`b` > 0);