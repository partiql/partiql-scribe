-- #[subquery-00]
-- Spark does not support top level expression.
-- 1 = (SELECT b FROM T);

-- #[subquery-01]
-- Spark does not support top level expression.
-- (false, 1) = (SELECT a, b FROM T);

-- IN collection subquery
--#[subquery-02]
SELECT `upper`(`T`.`v`) AS `_1`
    FROM `default`.`T` AS `T`
    WHERE `array_contains`(`T`.`b`,
            SELECT `T`.`b` AS `b`
                FROM `default`.`T` AS `T`
                WHERE `T`.`a`)

-- #[subquery-03]
-- Spark does not support top level expression.
-- 100 = (SELECT COUNT(*) FROM T);
