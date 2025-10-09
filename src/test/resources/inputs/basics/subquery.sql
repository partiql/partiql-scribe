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

-- Correlated subquery
--#[subquery-16]
SELECT a FROM T as t1 WHERE b = (SELECT t2.b FROM T AS t2 WHERE t1.b = t2.b);
