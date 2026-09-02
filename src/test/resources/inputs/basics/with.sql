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

--#[with-05]
-- Nested CTE reference - sibling references
WITH cte1 AS (
    SELECT a, b FROM SIMPLE_T
),
cte2 AS (
    SELECT a FROM cte1
),
cte3 AS (
    SELECT a FROM cte2
)
SELECT * FROM cte3;

--#[with-06]
-- Sibling CTE references (cte2 refs cte1, cte3 refs cte2) joined together in the body
WITH cte1 AS (
    SELECT a, b FROM SIMPLE_T
),
cte2 AS (
    SELECT a FROM cte1
),
cte3 AS (
    SELECT a FROM cte2
)
SELECT * FROM cte1 JOIN cte2 ON cte1.a = cte2.a JOIN cte3 ON cte2.a = cte3.a;

--#[with-07]
-- CTE with subquery
WITH cte1 AS (
    SELECT a FROM SIMPLE_T WHERE b > (SELECT AVG(b) FROM SIMPLE_T)
)
SELECT * FROM cte1;

--#[with-08]
-- CTE used multiple times - not supported, alias is lost with join. https://github.com/partiql/partiql-scribe/issues/138
WITH cte1 AS (
    SELECT a, b FROM SIMPLE_T
)
SELECT * FROM cte1 AS c1 JOIN cte1 AS c2 ON c1.a = c2.a;

--#[with-09]
-- CTE with window function, COUNT(*), and GROUP BY in outer query
WITH cte1 AS (
    SELECT a, b FROM SIMPLE_T
)
SELECT ROW_NUMBER() OVER (ORDER BY a) AS rn, COUNT(*) AS cnt, a FROM cte1 GROUP BY a;

--#[with-10]
-- Nested WITH clauses
WITH cte1 AS (
  WITH cte2 AS (SELECT a, b FROM SIMPLE_T)
  SELECT a, b FROM cte2
)
SELECT a, b FROM (
  WITH cte3 AS (SELECT a, b FROM cte1)
  SELECT a, b FROM cte3
);
