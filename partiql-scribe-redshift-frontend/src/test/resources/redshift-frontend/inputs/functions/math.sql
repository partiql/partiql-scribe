-- The math functions: POWER, its synonym POW, and the exponentiation operator `^`. All three are written as `pow(a, b)`,
-- which PartiQL's built-in functions do not define; the test session's catalog defines it over two DOUBLE PRECISION
-- arguments, as Redshift's POWER converts its arguments, and the planner inserts the conversions.

--#[power]
SELECT POWER(b, 2), POW(b, 2) FROM SIMPLE_T;

--#[power-of-expression]
SELECT POWER(b * 2.0, 2), POWER(CAST(b AS DOUBLE PRECISION) / 3, 2) FROM SIMPLE_T;

--#[power-of-literals]
SELECT POWER(1024.0, 3), POW(2, 10), POWER(2, -1) FROM SIMPLE_T;

--#[power-in-where]
SELECT a FROM SIMPLE_T WHERE POWER(b, 2) > 100;

--#[power-of-conditional]
SELECT POWER(NVL(b, 0), 2), NVL(POWER(b, 2), 0) FROM SIMPLE_T;

--#[power-nested]
SELECT POWER(POWER(b, 2), 2) FROM SIMPLE_T;

--#[caret]
SELECT b ^ 2, 2 ^ b FROM SIMPLE_T;

-- `^` is left-associative, binds tighter than `*` and looser than a sign: 2 ^ 3 ^ 2 is (2 ^ 3) ^ 2, -2 ^ 2 is (-2) ^ 2,
-- and 2 * 3 ^ 2 is 2 * (3 ^ 2).
--#[caret-precedence]
SELECT 2 ^ 3 ^ 2, -2 ^ 2, 2 * 3 ^ 2, (2 * 3) ^ 2, 2 ^ (3 ^ 2) FROM SIMPLE_T;

--#[caret-and-power]
SELECT POWER(b, 2) ^ 2 FROM SIMPLE_T;

--#[math-functions-mixed-case-names]
SELECT Power(B, 2), pow(b, 2) FROM simple_t;
