-- Scalar subquery coercion
--#[subquery-00]
1 = (SELECT b FROM T);

-- Row value subquery coercion
-- TODO: ERROR: SELECT constructor with 2 attributes cannot be coerced to a scalar. Found constructor type: struct(a: bool, b: int4, [Open(value=false), UniqueAttrs(value=true), Ordered])
-- #[subquery-01]
-- (false, 1) = (SELECT a, b FROM T);

-- Scalar subquery coercion with aggregation
--#[subquery-02]
-- 100 = (SELECT MAX(t.b) FROM T as t)
100 = (SELECT COUNT(*) FROM T);

-- Comparison operators with subqueries
--#[subquery-03]
SELECT a FROM T WHERE b = (SELECT b FROM T WHERE b > 0);

--#[subquery-04]
SELECT a FROM T WHERE b > (SELECT b FROM T WHERE b > 0);

--#[subquery-05]
SELECT b FROM T WHERE b < (SELECT b FROM T WHERE b > 0);

--#[subquery-06]
SELECT v FROM T WHERE b >= (SELECT b FROM T WHERE b > 0);

--#[subquery-07]
SELECT a FROM T WHERE b <= (SELECT b FROM T WHERE b > 0);

--#[subquery-08]
SELECT b FROM T WHERE b <> (SELECT b FROM T WHERE b > 0);

-- IN collection subquery
--#[subquery-09]
SELECT a FROM T WHERE b IN (SELECT b FROM T WHERE b > 0);

-- NOT IN subquery
--#[subquery-10]
SELECT a FROM T WHERE b NOT IN (SELECT b FROM T WHERE b > 0);

-- EXISTS subquery
--#[subquery-11]
SELECT b FROM T WHERE EXISTS (SELECT v FROM T AS t2 WHERE t2.b > 0);

-- NOT EXISTS subquery
--#[subquery-12]
SELECT v FROM T WHERE NOT EXISTS (SELECT a FROM T AS t2 WHERE t2.b > 0 AND t2.c > 0);

-- UNIQUE subquery
--#[subquery-13]
SELECT a FROM T WHERE UNIQUE (SELECT b FROM T AS t2 WHERE t2.b > 0);

-- Nested subqueries
--#[subquery-14]
SELECT b FROM T WHERE b IN (SELECT b FROM T WHERE b > (SELECT b FROM T WHERE b > 0));

-- Subquery in SELECT clause
--#[subquery-15]
SELECT b, (SELECT v FROM T AS t2 WHERE t2.b > 0) AS match_v FROM T;

-- Multiple subqueries in WHERE
--#[subquery-16]
SELECT v FROM T WHERE b > (SELECT b FROM T WHERE b > 0) AND EXISTS (SELECT a FROM T AS t2 WHERE t2.b > 0);

-- Correlated subquery
--#[subquery-17]
SELECT a FROM T WHERE b > (SELECT t2.b FROM T AS t2 WHERE t2.a = t2.b);

-- ANY/SOME with subquery
-- #[subquery-18]
SELECT b FROM T WHERE b > ANY (SELECT b FROM T WHERE b > 0);

-- #[subquery-19]
SELECT v FROM T WHERE b = SOME (SELECT b FROM T WHERE b > 0);

-- ALL with subquery
-- #[subquery-20]
SELECT a FROM T WHERE b > ALL (SELECT b FROM T WHERE b > 0);
