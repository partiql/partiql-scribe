-- Expressions: arithmetic, operators, casts, CASE, literals, aggregates, and names.

--#[arith]
SELECT b + 1, b - 1, b * 2, b / 2 FROM SIMPLE_T;

--#[arith-unary]
SELECT -b, +b, -(b + 1) FROM SIMPLE_T;

--#[arith-precedence]
SELECT b + 1 * 2 - 3 / 4, (b + 1) * 2 FROM SIMPLE_T;

--#[arith-exponent]
SELECT b ^ 2 FROM SIMPLE_T;

-- `&` has the precedence of `||`, below `+`: b & b + 1 is b & (b + 1).
--#[bitwise-and]
SELECT b & 1 FROM SIMPLE_T;

--#[bitwise-and-precedence]
SELECT b & b + 1, (b & b) + 1, b & b * 2 FROM SIMPLE_T;

--#[bitwise-and-in-where]
SELECT a FROM SIMPLE_T WHERE b & 1 = 1;

--#[bitwise-and-chain]
SELECT b & 1 & 3 FROM SIMPLE_T;

--#[bitwise-and-null]
SELECT b & NULL, NULL & 1 FROM SIMPLE_T;

--#[concat]
SELECT c || 'x', c || c || 'y' FROM SIMPLE_T;

--#[concat-with-arith]
SELECT c || b + 1 FROM SIMPLE_T;

--#[case-searched]
SELECT CASE WHEN b > 0 THEN 'pos' WHEN b < 0 THEN 'neg' ELSE 'zero' END FROM SIMPLE_T;

--#[case-searched-no-else]
SELECT CASE WHEN a THEN 1 END FROM SIMPLE_T;

--#[case-simple]
SELECT CASE b WHEN 1 THEN 'one' WHEN 2 THEN 'two' ELSE 'many' END FROM SIMPLE_T;

--#[case-nested]
SELECT CASE WHEN a THEN CASE WHEN b > 0 THEN 1 ELSE 2 END ELSE 3 END FROM SIMPLE_T;

--#[cast]
SELECT CAST(b AS VARCHAR(10)), CAST(c AS INT), CAST(b AS DOUBLE PRECISION) FROM SIMPLE_T;

--#[cast-shorthand]
SELECT b::VARCHAR, c::BIGINT, b::DECIMAL(10, 2), b::SMALLINT FROM SIMPLE_T;

--#[cast-shorthand-chain]
SELECT b::INT::VARCHAR FROM SIMPLE_T;

--#[cast-shorthand-precedence]
SELECT -b::INT, b + 1::INT FROM SIMPLE_T;

--#[cast-types]
SELECT CAST(c AS TEXT), CAST(c AS VARCHAR(MAX)), CAST(c AS CHAR), CAST(c AS CHAR(3)), CAST(b AS NUMERIC), CAST(b AS REAL), CAST(b AS FLOAT), CAST(b AS BOOLEAN) FROM SIMPLE_T;

--#[cast-datetime]
SELECT CAST(c AS DATE), CAST(c AS TIMESTAMP) FROM SIMPLE_T;

--#[convert]
SELECT CONVERT(INT, c), CONVERT(VARCHAR(10), b) FROM SIMPLE_T;

--#[literals]
SELECT 1, 1.5, .5, 'abc', 'it''s', TRUE, FALSE, NULL FROM SIMPLE_T;

--#[literal-string-backslash]
SELECT 'a\'b', 'line\nbreak', 'back\\slash' FROM SIMPLE_T;

--#[literal-string-backslash-unknown]
SELECT 'a\qb' FROM SIMPLE_T;

--#[literal-date]
SELECT DATE '2024-01-31' FROM SIMPLE_T;

--#[literal-timestamp]
SELECT TIMESTAMP '2024-01-31 12:34:56', TIMESTAMP '2024-01-31 12:34:56.789' FROM SIMPLE_T;

--#[literal-date-format]
SELECT DATE '01/31/2024' FROM SIMPLE_T;

--#[literal-interval]
SELECT timestamp_1 + INTERVAL '1 day' FROM T;

--#[aggregates]
SELECT COUNT(*), COUNT(b), COUNT(DISTINCT b), SUM(b), AVG(b), MAX(c), MIN(c) FROM SIMPLE_T;

--#[aggregates-all]
SELECT SUM(ALL b), COUNT(ALL c) FROM SIMPLE_T;

--#[aggregates-in-expressions]
SELECT SUM(b) / COUNT(*), MAX(b) - MIN(b), CAST(AVG(b) AS INT) FROM SIMPLE_T;

--#[aggregates-case-insensitive]
SELECT count(*), Sum(b) FROM SIMPLE_T;

--#[names-nested-field]
SELECT d.e FROM T;

--#[names-qualified-nested-field]
SELECT t.d.e FROM T AS t;

--#[parenthesized]
SELECT ((b)), (b + (1 * 2)) FROM SIMPLE_T;

--#[cast-type-aliases]
SELECT CAST(b AS INT2), CAST(b AS INT4), CAST(b AS INT8), CAST(b AS FLOAT4), CAST(b AS FLOAT8), CAST(c AS BPCHAR), CAST(c AS NVARCHAR(5)), CAST(c AS CHARACTER VARYING(6)), CAST(c AS TIMESTAMP WITHOUT TIME ZONE) FROM SIMPLE_T;

--#[cast-type-bounds]
SELECT CAST(c AS VARCHAR(65535)), CAST(c AS CHAR(4096)), CAST(b AS DECIMAL(38, 37)) FROM SIMPLE_T;

--#[cast-varchar-too-long]
SELECT CAST(c AS VARCHAR(65536)) FROM SIMPLE_T;

--#[cast-varchar-zero]
SELECT CAST(c AS VARCHAR(0)) FROM SIMPLE_T;

--#[cast-decimal-scale-over-precision]
SELECT CAST(b AS DECIMAL(5, 6)) FROM SIMPLE_T;

--#[cast-length-not-an-int]
SELECT CAST(c AS VARCHAR(99999999999)) FROM SIMPLE_T;

--#[case-sensitive-identifiers-off]
SELECT "C" AS "Mixed" FROM SIMPLE_T;

-- Redshift's widest number is DECIMAL(38); the planner would round a longer literal.
--#[numeric-literal-38-digits]
SELECT 1.0000000000000000000000000000000000001 AS v FROM SIMPLE_T;

--#[numeric-literal-39-digits]
SELECT 1.000000000000000000000000000000000000001 AS v FROM SIMPLE_T;

--#[integer-literal-39-digits]
SELECT 100000000000000000000000000000000000000 AS v FROM SIMPLE_T;
