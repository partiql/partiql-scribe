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
SELECT a, LAG(a) OVER (ORDER BY a) FROM T;

--#[window-08]
SELECT a, LEAD(a, 1) OVER (ORDER BY a) FROM T;

--#[window-09]
SELECT a, LAG(a, 2, NULL) OVER (ORDER BY a) FROM T;

--#[window-10]
SELECT a, LEAD(a, 2, FALSE) OVER (ORDER BY a) FROM T;

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
SELECT t.a AS _id, t.b AS _name, RANK() OVER _w1 AS _rank_1, RANK() OVER _w2 AS _rank_2, DENSE_RANK() OVER _w1 AS _dense_rank_1, DENSE_RANK() OVER _w2 AS _dense_rank_2, ROW_NUMBER() OVER _w1 as _row_number_1, ROW_NUMBER() OVER _w2 as _row_number_2, LAG(t.b, 1, -1) OVER _w1 AS _lag_1, LAG(t.b, 1, -1) OVER _w2 AS _lag_2, LEAD(t.b, 1, -1) OVER _w1 AS _lead_1, LEAD(t.b, 1, -1) OVER _w2 AS _lead_2 FROM T AS t WINDOW _w1 AS (PARTITION BY t.a ORDER BY t.b, t.c), _w2 AS (PARTITION BY t.a ORDER BY t.b DESC, t.c DESC);


-- SUM over a partition
--#[window-23]
SELECT a, SUM(b) OVER (PARTITION BY a) FROM T;

-- AVG over a partition with ORDER BY
--#[window-24]
SELECT a, AVG(b) OVER (PARTITION BY a ORDER BY c) FROM T;

-- COUNT(*) over a partition
--#[window-25]
SELECT a, COUNT(*) OVER (PARTITION BY a) FROM T;

-- PartiQL does not support window frames yet (https://github.com/partiql/partiql-lang-kotlin/issues/1837)
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

-- Window function with GROUP BY
--#[window-34]
SELECT a, COUNT(*), ROW_NUMBER() OVER (ORDER BY COUNT(*)) FROM T GROUP BY a;

-- MAX with PARTITION BY and ORDER BY
--#[window-35]
SELECT a, MAX(b) OVER (PARTITION BY a ORDER BY c) FROM T;

-- MIN over a named window with ORDER BY
--#[window-36]
SELECT a, MIN(b) OVER w FROM T WINDOW w AS (PARTITION BY a ORDER BY c);

-- COUNT(DISTINCT) over a partition
--#[window-37]
SELECT a, COUNT(DISTINCT b) OVER (PARTITION BY a) FROM T;

-- SUM over the whole input (empty OVER)
--#[window-38]
SELECT a, SUM(b) OVER () FROM T;

-- Two aggregate window functions in one query
--#[window-39]
SELECT a, COUNT(b) OVER (ORDER BY c), MAX(b) OVER (ORDER BY c) FROM T;

-- Aggregate window function over a GROUP BY aggregate
--#[window-40]
SELECT a, SUM(COUNT(*)) OVER () FROM T GROUP BY a;

-- Window in a correlated derived table under a scalar subquery
--#[window-42]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c) AS s) AS m FROM T AS o;

-- Window in an uncorrelated scalar subquery
--#[window-43]
SELECT o.a AS a, (SELECT MAX(t.b) OVER () AS m FROM T AS t LIMIT 1) AS m FROM T AS o;

-- Window in a correlated IN subquery
--#[window-44]
SELECT o.a AS a FROM T AS o WHERE o.b IN (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c);

-- Window in a correlated EXISTS subquery
--#[window-45]
SELECT o.a AS a FROM T AS o WHERE EXISTS (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c);

-- Window in a correlated lateral derived table
--#[window-46]
SELECT o.a AS a, s.m AS m FROM T AS o, (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c) AS s;

-- ROW_NUMBER in a correlated subquery
--#[window-47]
SELECT o.a AS a, (SELECT MAX(s.n) AS n FROM (SELECT ROW_NUMBER() OVER (ORDER BY t.b) AS n FROM T AS t WHERE t.c = o.c) AS s) AS n FROM T AS o;

-- Window in a subquery over a CTE of the outer query (not a correlation)
--#[window-48]
WITH w AS (SELECT t.b AS b, t.c AS c FROM T AS t) SELECT o.a AS a, (SELECT MAX(x.b) OVER () AS m FROM w AS x LIMIT 1) AS m FROM T AS o;

-- Correlation inside a CASE in a subquery with a window
--#[window-49]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE CASE WHEN t.c = o.c THEN TRUE ELSE FALSE END) AS s) AS m FROM T AS o;

-- Correlation in a subquery body with its own WITH clause
--#[window-50]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (WITH w AS (SELECT t.b AS b, t.c AS c FROM T AS t) SELECT MAX(x.b) OVER () AS m FROM w AS x WHERE x.c = o.c) AS s) AS m FROM T AS o;

-- Aggregate over a named window without ORDER BY
--#[window-51]
SELECT a, MIN(b) OVER w FROM T WINDOW w AS (PARTITION BY a);

-- Subquery over an outer CTE that is also correlated
--#[window-52]
WITH w AS (SELECT t.b AS b, t.c AS c FROM T AS t) SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(x.b) OVER () AS m FROM w AS x WHERE x.c = o.c) AS s) AS m FROM T AS o;

-- Window in a correlated NOT EXISTS subquery
--#[window-53]
SELECT o.a AS a FROM T AS o WHERE NOT EXISTS (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c);

-- Correlated EXISTS and correlated scalar subqueries with windows in one query
--#[window-54]
SELECT o.a AS a FROM T AS o WHERE EXISTS (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t.c = o.c) AND o.b = (SELECT MAX(s.m) AS m FROM (SELECT MAX(u.b) OVER () AS m FROM T AS u WHERE u.c = o.c) AS s);

-- Correlation in the select list, outside the window function
--#[window-55]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () + o.b AS m FROM T AS t) AS s) AS m FROM T AS o;

-- Correlation in a subquery nested in the subquery with the window
--#[window-56]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE EXISTS (SELECT u.a AS a FROM T AS u WHERE u.c = o.c)) AS s) AS m FROM T AS o;

-- Correlated subquery whose window is only in an uncorrelated derived table
--#[window-57]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () AS m FROM T AS t) AS s WHERE s.m > o.b) AS m FROM T AS o;

-- Correlation in a path index
--#[window-58]
SELECT o.a AS a, (SELECT MAX(s.m) AS m FROM (SELECT MAX(t.b) OVER () AS m FROM T AS t WHERE t."array"[o.b] = 1) AS s) AS m FROM T AS o;
