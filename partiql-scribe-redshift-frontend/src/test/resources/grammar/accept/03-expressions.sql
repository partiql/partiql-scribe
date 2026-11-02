-- Expressions and operators: arithmetic, `^`, `&`, `||`, datetime arithmetic, scalar subqueries, CASE, and casts.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html

--#[arith]
SELECT a + b, a - b, a * b, a / b FROM t;

--#[arith-unary]
SELECT -a, +a, - -a, -(a + b) FROM t;

--#[arith-precedence]
SELECT a + b * c - d / e, (a + b) * c FROM t;

--#[arith-exponent]
SELECT a ^ 2, 2 ^ a ^ b, -a ^ 2 FROM t;

--#[bitwise-and]
SELECT a & b, a & 1 FROM t;

--#[concat]
SELECT a || b, a || ' ' || b, 'x' || 1 FROM t;

--#[concat-with-arith]
SELECT a || b + c FROM t;

--#[datetime-plus-days]
SELECT d + 1, d - 1, d + n FROM t;

--#[datetime-difference]
SELECT d1 - d2 FROM t;

--#[datetime-plus-interval]
SELECT ts + INTERVAL '1 day', ts - INTERVAL '2 hours', d + INTERVAL '3 months' FROM t;

--#[datetime-literal-arith]
SELECT DATE '2024-01-01' + 30, TIMESTAMP '2024-01-01 00:00:00' + INTERVAL '1 year' FROM t;

--#[scalar-subquery]
SELECT (SELECT MAX(b) FROM u) FROM t;

--#[scalar-subquery-in-expression]
SELECT a - (SELECT AVG(a) FROM t) AS diff FROM t;

--#[scalar-subquery-correlated]
SELECT (SELECT COUNT(*) FROM u WHERE u.t_id = t.id) FROM t;

--#[case-searched]
SELECT CASE WHEN a > 0 THEN 'pos' WHEN a < 0 THEN 'neg' ELSE 'zero' END FROM t;

--#[case-searched-no-else]
SELECT CASE WHEN a > 0 THEN 1 END FROM t;

--#[case-simple]
SELECT CASE a WHEN 1 THEN 'one' WHEN 2 THEN 'two' ELSE 'many' END FROM t;

--#[case-nested]
SELECT CASE WHEN a THEN CASE WHEN b THEN 1 ELSE 2 END ELSE 3 END FROM t;

--#[case-in-condition]
SELECT a FROM t WHERE CASE WHEN b IS NULL THEN 0 ELSE b END > 1;

--#[cast]
SELECT CAST(a AS INT), CAST('2024-01-01' AS DATE), CAST(NULL AS VARCHAR(10)) FROM t;

--#[cast-shorthand]
SELECT a::INT, '1'::DECIMAL(10, 2), a::INT::VARCHAR FROM t;

--#[cast-shorthand-precedence]
SELECT -a::INT, a + b::INT, (a + b)::INT FROM t;

--#[cast-of-expression]
SELECT CAST(a + b AS BIGINT), CAST(CASE WHEN a THEN 1 ELSE 0 END AS BOOLEAN) FROM t;

--#[convert]
SELECT CONVERT(INT, a), CONVERT(VARCHAR, a + 1) FROM t;

--#[parenthesized]
SELECT ((a)), (a + (b * c)) FROM t;
