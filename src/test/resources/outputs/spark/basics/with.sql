--#[with-00]
-- SELECT *
WITH `cte1` AS (SELECT `SIMPLE_T`.`a` AS `a`, `SIMPLE_T`.`b` AS `b`, `SIMPLE_T`.`c` AS `c` FROM `default`.`SIMPLE_T` AS `SIMPLE_T`) SELECT `cte1`.`a` AS `a`, `cte1`.`b` AS `b`, `cte1`.`c` AS `c` FROM `cte1` AS `cte1`;

--#[with-01]
-- SELECT * and alias for with list element
WITH `cte1` AS (SELECT `wle1`.`a` AS `a`, `wle1`.`b` AS `b`, `wle1`.`c` AS `c` FROM `default`.`SIMPLE_T` AS `wle1`) SELECT `cte1`.`a` AS `a`, `cte1`.`b` AS `b`, `cte1`.`c` AS `c` FROM `cte1` AS `cte1`;

--#[with-02]
-- SELECT list and alias for with list element
WITH `cte1` AS (SELECT `wle1`.`a` AS `a` FROM `default`.`SIMPLE_T` AS `wle1`) SELECT `cte1`.`a` AS `a` FROM `cte1` AS `cte1`;

--#[with-03]
-- CTE with aggregation
WITH `cte1` AS (SELECT `a` AS `a`, `count`(1) AS `cnt` FROM `default`.`SIMPLE_T` AS `SIMPLE_T` GROUP BY `SIMPLE_T`.`a`) SELECT `cte1`.`a` AS `a`, `cte1`.`cnt` AS `cnt` FROM `cte1` AS `cte1` WHERE `cte1`.`cnt` > CAST(1 AS BIGINT);

--#[with-04]
-- CTE with JOIN
WITH `cte1` AS (SELECT `t1`.`a` AS `a`, `t2`.`b` AS `b` FROM `default`.`SIMPLE_T` AS `t1` INNER JOIN `default`.`SIMPLE_T` AS `t2` ON `t1`.`a` = `t2`.`a`) SELECT `cte1`.`a` AS `a`, `cte1`.`b` AS `b` FROM `cte1` AS `cte1`;

-- #[with-05]
-- Nested CTE reference - not supported
-- WITH `cte1` AS (SELECT `SIMPLE_T`.`a` AS `a`, `SIMPLE_T`.`b` AS `b` FROM `default`.`SIMPLE_T` AS `SIMPLE_T`), `cte2` AS (SELECT `cte1`.`a` AS `a` FROM `cte1` AS `cte1`), `cte3` AS (SELECT `cte2`.`a` AS `a` FROM `cte2` AS `cte2`) SELECT `cte3`.`a` AS `a` FROM `cte3` AS `cte3`;

--#[with-06]
-- CTE with subquery
WITH `cte1` AS (SELECT `SIMPLE_T`.`a` AS `a` FROM `default`.`SIMPLE_T` AS `SIMPLE_T` WHERE `SIMPLE_T`.`b` > (SELECT `avg`(`SIMPLE_T`.`b`) AS `_1` FROM `default`.`SIMPLE_T` AS `SIMPLE_T`)) SELECT `cte1`.`a` AS `a` FROM `cte1` AS `cte1`;

-- #[with-07]
-- CTE used multiple times - not supported, alias is lost with join
-- WITH `cte1` AS (SELECT `SIMPLE_T`.`a` AS `a`, `SIMPLE_T`.`b` AS `b` FROM `default`.`SIMPLE_T` AS `SIMPLE_T`) SELECT `c1`.`a` AS `a`, `c1`.`b` AS `b`, `c2`.`a` AS `a`, `c2`.`b` AS `b` FROM `cte1` AS `c1` INNER JOIN `cte1` AS `c2` ON `c1`.`a` = `c2`.`a`;
