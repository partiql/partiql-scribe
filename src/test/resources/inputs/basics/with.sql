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
