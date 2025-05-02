-- Scalar subquery coercion
--#[subquery-00]
1 = (SELECT b FROM T);

-- Row value subquery coercion
-- TODO: ERROR: SELECT constructor with 2 attributes cannot be coerced to a scalar. Found constructor type: struct(a: bool, b: int4, [Open(value=false), UniqueAttrs(value=true), Ordered])
-- #[subquery-01]
-- (false, 1) = (SELECT a, b FROM T);

-- IN collection subquery
--#[subquery-02]
SELECT UPPER(v) FROM T
WHERE b IN (SELECT b FROM T WHERE a);

-- Scalar subquery coercion with aggregation
--#[subquery-03]
-- 100 = (SELECT MAX(t.b) FROM T as t)
100 = (SELECT COUNT(*) FROM T);
