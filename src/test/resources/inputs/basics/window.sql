-- Basic window functions with OVER clause
--#[window-01]
SELECT ROW_NUMBER() OVER (ORDER BY a) FROM T;

--#[window-02]
SELECT RANK() OVER (ORDER BY a DESC) FROM T;

--#[window-03]
SELECT DENSE_RANK() OVER (ORDER BY a) FROM T;

-- Window functions with PARTITION BY
--#[window-04]
SELECT a, ROW_NUMBER() OVER (PARTITION BY b ORDER BY a) FROM T;

--#[window-05]
SELECT a, RANK() OVER (PARTITION BY b ORDER BY a DESC) FROM T;

--#[window-06]
SELECT a, DENSE_RANK() OVER (PARTITION BY b ORDER BY a) FROM T;

-- LAG and LEAD functions
--#[window-07]
SELECT a, LAG(a, 1) OVER (ORDER BY a) FROM T;

--#[window-08]
SELECT a, LEAD(a, 1) OVER (ORDER BY a) FROM T;

--#[window-09]
SELECT a, LAG(a, 2, 'DEFAULT') OVER (ORDER BY a) FROM T;

--#[window-10]
SELECT a, LEAD(a, 2, 'DEFAULT') OVER (ORDER BY a) FROM T;

-- Window functions with NULLS handling
--#[window-11]
SELECT a, LAG(a, 1) IGNORE NULLS OVER (ORDER BY a) FROM T;

--#[window-12]
SELECT a, LAG(a, 1) RESPECT NULLS OVER (ORDER BY a) FROM T;

--#[window-13]
SELECT a, LEAD(a, 1) IGNORE NULLS OVER (ORDER BY a) FROM T;

--#[window-14]
SELECT a, LEAD(a, 1) RESPECT NULLS OVER (ORDER BY a) FROM T;

-- Window clause definitions
--#[window-15]
SELECT a, ROW_NUMBER() OVER w FROM T WINDOW w AS (ORDER BY a);

--#[window-16]
SELECT a, RANK() OVER w1, DENSE_RANK() OVER w2 FROM T WINDOW w1 AS (ORDER BY a), w2 AS (ORDER BY b DESC);

--#[window-17]
SELECT a, ROW_NUMBER() OVER w FROM T WINDOW w AS (PARTITION BY b ORDER BY a);

-- Multiple window functions with named windows
--#[window-18]
SELECT a, b, ROW_NUMBER() OVER w1 AS rn1, RANK() OVER w1 AS rank1, LAG(a, 1) OVER w1 AS lag1 FROM T WINDOW w1 AS (PARTITION BY b ORDER BY a);

-- Complex window with multiple partitions and orders
--#[window-19]
SELECT a, b, c, ROW_NUMBER() OVER w1 AS rn, RANK() OVER w2 AS rank_desc FROM T WINDOW w1 AS (PARTITION BY a ORDER BY b, c), w2 AS (PARTITION BY a ORDER BY b DESC, c DESC);

-- Multiple ORDER BY columns
--#[window-20]
SELECT a, b, ROW_NUMBER() OVER (ORDER BY a, b, c) FROM T;

--#[window-21]
SELECT a, b, RANK() OVER (PARTITION BY a ORDER BY b ASC, c DESC) FROM T;

-- Complex comprehensive example
--#[window-22]
SELECT t.a AS _id, t.b AS _name, RANK() OVER _w1 AS _rank_1, RANK() OVER _w2 AS _rank_2, DENSE_RANK() OVER _w1 AS _dense_rank_1, DENSE_RANK() OVER _w2 AS _dense_rank_2, ROW_NUMBER() OVER _w1 as _row_number_1, ROW_NUMBER() OVER _w2 as _row_number_2, LAG(t.b, 1, 'UNKNOWN') OVER _w1 AS _lag_1, LAG(t.b, 1, 'UNKNOWN') OVER _w2 AS _lag_2, LEAD(t.b, 1, 'UNKNOWN') OVER _w1 AS _lead_1, LEAD(t.b, 1, 'UNKNOWN') OVER _w2 AS _lead_2 FROM T AS t WINDOW _w1 AS (PARTITION BY t.a ORDER BY t.b, t.c), _w2 AS (PARTITION BY t.a ORDER BY t.b DESC, t.c DESC);


-- PartiQL does not support the feature, aggregation, window frame etc
-- Aggregate functions as window functions
-- --#[window-23]
-- SELECT a, SUM(b) OVER (PARTITION BY a) FROM T;

-- --#[window-24]
-- SELECT a, AVG(b) OVER (PARTITION BY a ORDER BY c) FROM T;

-- --#[window-25]
-- SELECT a, COUNT(*) OVER (PARTITION BY a) FROM T;

-- --#[window-26]
-- SELECT a, MAX(b) OVER (ORDER BY a ROWS UNBOUNDED PRECEDING) FROM T;

-- --#[window-27]
-- SELECT a, MIN(b) OVER (ORDER BY a ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING) FROM T;

-- Frame specifications
-- --#[window-28]
-- SELECT a, SUM(b) OVER (ORDER BY a ROWS UNBOUNDED PRECEDING) FROM T;

-- --#[window-29]
-- SELECT a, SUM(b) OVER (ORDER BY a ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM T;

-- --#[window-30]
-- SELECT a, SUM(b) OVER (ORDER BY a ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING) FROM T;

-- --#[window-31]
-- SELECT a, SUM(b) OVER (ORDER BY a ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING) FROM T;

-- RANGE frame specifications
-- --#[window-32]
-- SELECT a, SUM(b) OVER (ORDER BY a RANGE UNBOUNDED PRECEDING) FROM T;

-- --#[window-33]
-- SELECT a, SUM(b) OVER (ORDER BY a RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM T;