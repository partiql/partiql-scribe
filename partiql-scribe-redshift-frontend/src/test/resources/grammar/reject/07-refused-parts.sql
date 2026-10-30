-- The parts of an accepted construct that have no counterpart in the target: operators, literal and type forms, and
-- clause forms that are refused.

--#[modulo] 1:10
SELECT a % b FROM t;

--#[square-root-operator] 1:8
SELECT |/ a FROM t;

--#[cube-root-operator] 1:8
SELECT ||/ a FROM t;

--#[absolute-value-operator] 1:8
SELECT @ a FROM t;

--#[bitwise-or] 1:10
SELECT a | b FROM t;

--#[bitwise-xor] 1:10
SELECT a # b FROM t;

--#[bitwise-not] 1:8
SELECT ~ a FROM t;

--#[shift-left] 1:10
SELECT a << 2 FROM t;

--#[shift-right] 1:10
SELECT a >> 2 FROM t;

--#[exponent-literal] 1:8
SELECT 1e3 FROM t;

--#[exponent-literal-decimal] 1:8
SELECT 1.5e-3 FROM t;

--#[exponent-literal-in-expression] 1:12
SELECT a * 1E10 FROM t;

--#[literal-time] 1:13
SELECT TIME '12:34:56' FROM t;

--#[literal-timetz] 1:15
SELECT TIMETZ '12:34:56+00' FROM t;

--#[literal-time-with-time-zone] 1:13
SELECT TIME WITH TIME ZONE '12:34:56+00' FROM t;

--#[literal-timestamptz] 1:20
SELECT TIMESTAMPTZ '2024-01-01 00:00:00+00' FROM t;

--#[literal-timestamp-with-time-zone] 1:18
SELECT TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+00' FROM t;

--#[type-time] 1:18
SELECT CAST(a AS TIME) FROM t;

--#[type-timetz] 1:18
SELECT CAST(a AS TIMETZ), CAST(a AS TIME WITH TIME ZONE) FROM t;

--#[type-timestamptz] 1:18
SELECT CAST(a AS TIMESTAMPTZ) FROM t;

--#[type-timestamp-with-time-zone] 1:28
SELECT CAST(a AS TIMESTAMP WITH TIME ZONE) FROM t;

--#[type-super] 1:11
SELECT a::SUPER FROM t;

--#[type-unknown-name] 1:18
SELECT CAST(a AS JSON) FROM t;

--#[between-symmetric] 1:43
SELECT a FROM t WHERE a BETWEEN SYMMETRIC 10 AND 1;

--#[between-asymmetric] 1:44
SELECT a FROM t WHERE a BETWEEN ASYMMETRIC 1 AND 10;

--#[is-distinct-from] 1:28
SELECT a FROM t WHERE a IS DISTINCT FROM b;

--#[two-statements] 1:18
SELECT a FROM t; SELECT b FROM u;

--#[two-statements-no-space] 1:17
SELECT a FROM t;SELECT b FROM u

--#[double-semicolon] 1:17
SELECT a FROM t;;

--#[nested-block-comment] 1:44
SELECT a FROM t /* outer /* inner */ still a comment */;

--#[limit-expression] 1:25
SELECT a FROM t LIMIT 5 + 5;

--#[limit-subquery] 1:23
SELECT a FROM t LIMIT (SELECT 10 FROM u);

--#[offset-expression] 1:24
SELECT a FROM t OFFSET n;

--#[count-star-distinct] 1:23
SELECT COUNT(DISTINCT *) FROM t;

--#[aggregate-two-arguments] 1:13
SELECT SUM(a, b) FROM t;

--#[aggregate-no-arguments] 1:12
SELECT MAX() FROM t;

--#[scalar-function-distinct] 1:14
SELECT UPPER(DISTINCT a) FROM t;

--#[scalar-function-no-arguments] 1:14
SELECT LOWER() FROM t;

--#[current-date-with-parens] 1:20
SELECT CURRENT_DATE() FROM t;

--#[sysdate-with-parens] 1:15
SELECT SYSDATE() FROM t;

--#[dateadd-two-arguments] 1:22
SELECT DATEADD(day, d) FROM t;

--#[datediff-four-arguments] 1:26
SELECT DATEDIFF(day, a, b, c) FROM t;

--#[extract-comma-form] 1:21
SELECT EXTRACT(EPOCH, ts) FROM t;

--#[cast-no-type] 1:17
SELECT CAST(a AS) FROM t;

--#[convert-reversed-arguments] 1:16
SELECT CONVERT(a, INT) FROM t;

--#[with-after-select] 1:17
SELECT a FROM t WITH c AS (SELECT 1 FROM u);
