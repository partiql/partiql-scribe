-- Operators: arithmetic, concatenation, bitwise, mathematical prefix operators, the cast shorthand, AT TIME ZONE.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html

--#[arithmetic]
SELECT a + b, a - b, a * b, a / b, a % b, a ^ b FROM t;

--#[arithmetic-precedence]
SELECT a + b * c - d / e % f ^ g FROM t;

--#[arithmetic-parenthesized]
SELECT (a + b) * (c - d) / ((e)) FROM t;

--#[unary-sign]
SELECT -a, +a, - -a, -(a + b), -1, +1.5 FROM t;

--#[concatenation]
SELECT a || b, a || ' ' || b, 'x' || 1 FROM t;

--#[bitwise]
SELECT a & b, a | b, a << 2, a >> 1, ~a FROM t;

--#[math-prefix]
SELECT @ a, |/ a, ||/ a, @ -a FROM t;

--#[cast-shorthand]
SELECT a::INT, b::VARCHAR, c::DECIMAL(10, 2), d::TIMESTAMP, '1'::BIGINT FROM t;

--#[cast-shorthand-chained]
SELECT a::VARCHAR::INT, (a + b)::DECIMAL(5, 1), -a::INT, t.a::DATE FROM t;

--#[cast-shorthand-in-expression]
SELECT a::DECIMAL / b::DECIMAL * 100, a::VARCHAR || b::VARCHAR FROM t;

--#[at-time-zone]
SELECT ts AT TIME ZONE 'UTC', ts AT TIME ZONE 'America/Los_Angeles' AT TIME ZONE 'UTC', ts AT TIME ZONE tz FROM t;

--#[at-time-zone-in-expression]
SELECT (ts AT TIME ZONE 'UTC')::DATE, ts AT TIME ZONE 'UTC' + INTERVAL '1 day' FROM t;

--#[operators-with-literals]
SELECT 1 + 2 * 3, 10 / 4, 7 % 3, 2 ^ 10, 'a' || 'b', 5 & 3, 5 | 3, 1 << 4;

--#[operators-with-functions]
SELECT ABS(a - b) * 2, LENGTH(a || b) + 1, ROUND(a / b, 2) FROM t;

--#[operators-with-subquery]
SELECT a - (SELECT AVG(a) FROM t), (SELECT COUNT(*) FROM u) * 2 FROM t;

--#[operators-with-case]
SELECT CASE WHEN a > 0 THEN a ELSE -a END * 2 + 1 FROM t;
