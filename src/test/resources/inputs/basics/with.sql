--#[with-00]
-- SELECT *
WITH cte1 AS (
    SELECT * FROM SIMPLE_T
)
SELECT * FROM cte1;

--#[with-01]
-- SELECT * and alias for with list element
WITH cte1 AS (
    SELECT * FROM SIMPLE_T AS wle1
)
SELECT * FROM cte1;

--#[with-02]
-- SELECT list and alias for with list element
WITH cte1 AS (
    SELECT wle1.a FROM SIMPLE_T AS wle1
)
SELECT * FROM cte1;

--#[with-03]
-- CTE with aggregation
WITH cte1 AS (
    SELECT a, COUNT(*) AS cnt FROM SIMPLE_T GROUP BY a
)
SELECT * FROM cte1 WHERE cnt > 1;

--#[with-04]
-- CTE with JOIN
WITH cte1 AS (
    SELECT t1.a, t2.b FROM SIMPLE_T AS t1 JOIN SIMPLE_T AS t2 ON t1.a = t2.a
)
SELECT * FROM cte1;

-- #[with-05]
-- Nested CTE reference - Planning error, https://github.com/partiql/partiql-lang-kotlin/issues/1868
-- WITH cte1 AS (
--     SELECT a, b FROM SIMPLE_T
-- ),
-- cte2 AS (
--     SELECT a FROM cte1
-- ),
-- cte3 AS (
--     SELECT a FROM cte2
-- )
-- SELECT * FROM cte3;

--#[with-06]
-- CTE with subquery
WITH cte1 AS (
    SELECT a FROM SIMPLE_T WHERE b > (SELECT AVG(b) FROM SIMPLE_T)
)
SELECT * FROM cte1;

--#[with-07]
-- CTE used multiple times - not supported, alias is lost with join. https://github.com/partiql/partiql-scribe/issues/138
WITH cte1 AS (
    SELECT a, b FROM SIMPLE_T
)
SELECT * FROM cte1 AS c1 JOIN cte1 AS c2 ON c1.a = c2.a;
