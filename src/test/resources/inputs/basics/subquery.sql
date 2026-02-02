-- Scalar subquery coercion
--#[subquery-00]
1 = (SELECT b FROM T);

-- Row value subquery coercion
-- TODO: ERROR: SELECT constructor with 2 attributes cannot be coerced to a scalar. Found constructor type: struct(a: bool, b: int4, [Open(value=false), UniqueAttrs(value=true), Ordered])
-- Tracking with https://github.com/partiql/partiql-lang-kotlin/issues/1841
-- #[subquery-01]
-- (false, 1) = (SELECT a, b FROM T);

-- Scalar subquery coercion with aggregation
--#[subquery-02]
-- 100 = (SELECT MAX(t.b) FROM T as t)
100 = (SELECT COUNT(*) FROM T);

-- Comparison operators with subqueries
--#[subquery-03]
SELECT a FROM T WHERE b = (SELECT b FROM T WHERE b = 1);

--#[subquery-04]
SELECT a FROM T WHERE b > (SELECT b FROM T WHERE b = 1);

--#[subquery-05]
SELECT b FROM T WHERE b < (SELECT b FROM T WHERE b = 1);

--#[subquery-06]
SELECT v FROM T WHERE b >= (SELECT b FROM T WHERE b = 1);

--#[subquery-07]
SELECT a FROM T WHERE b <= (SELECT b FROM T WHERE b = 1);

--#[subquery-08]
SELECT b FROM T WHERE b <> (SELECT b FROM T WHERE b = 1);

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

-- Nested subqueries
--#[subquery-13]
SELECT b FROM T WHERE b IN (SELECT b FROM T WHERE b > (SELECT b FROM T WHERE b = 0));

-- Subquery in SELECT clause
--#[subquery-14]
SELECT b, (SELECT v FROM T AS t2 WHERE t2.b = 0) AS match_v FROM T;

-- Multiple subqueries in WHERE
--#[subquery-15]
SELECT v FROM T WHERE b > (SELECT b FROM T WHERE b > 0) AND EXISTS (SELECT a FROM T AS t2 WHERE t2.b > 0);

-- Correlated subquery with aggregation
--#[subquery-16]
SELECT a FROM T as t1 WHERE b > (SELECT avg(t2.b) FROM T AS t2 WHERE t1.a = t2.a);

-- Correlated IN subquery
--#[subquery-17]
SELECT b FROM T as t1 WHERE t1.b IN (SELECT t2.b FROM T AS t2 WHERE t2.a = t1.a);

-- Correlated subquery in SELECT
--#[subquery-18]
SELECT a, (SELECT MAX(t2.b) FROM T AS t2 WHERE t2.a = t1.a) AS max_b FROM T as t1;

-- Multiple correlated subqueries
--#[subquery-19]
SELECT a FROM T as t1 WHERE b > (SELECT AVG(t2.b) FROM T AS t2 WHERE t2.a = t1.a) AND v IN (SELECT t3.v FROM T AS t3 WHERE t3.b = t1.b);

-- Nested correlated subqueries
--#[subquery-20]
SELECT a FROM T as t1 WHERE b > (SELECT AVG(t2.b) FROM T AS t2 WHERE t2.a = t1.a AND t2.b > (SELECT MIN(t3.b) FROM T AS t3 WHERE t3.a = t1.a));

-- Correlated with multiple references
--#[subquery-21]
SELECT a FROM T as t1 WHERE (SELECT SUM(t2.b) FROM T AS t2 WHERE t2.a = t1.a AND t2.v = t1.v) > 10;
