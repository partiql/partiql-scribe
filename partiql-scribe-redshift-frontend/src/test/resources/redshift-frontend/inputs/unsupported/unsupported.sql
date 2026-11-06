-- Inputs the grammar accepts and the translation refuses: the functions that later changes translate, the forms with
-- no PartiQL counterpart yet, and operations PartiQL does not define on the operand types.

--#[function-power]
SELECT POWER(b, 2), POW(b, 2) FROM SIMPLE_T;

--#[function-concat]
SELECT CONCAT(c, 'x') FROM SIMPLE_T;

--#[function-lower-upper]
SELECT LOWER(c), UPPER(c) FROM SIMPLE_T;

--#[function-dateadd]
SELECT DATEADD(day, 1, timestamp_1) FROM T;

--#[function-datediff]
SELECT DATEDIFF(day, timestamp_1, timestamp_2) FROM T;

--#[function-getdate]
SELECT GETDATE() FROM SIMPLE_T;

--#[function-sysdate]
SELECT SYSDATE FROM SIMPLE_T;

--#[function-current-date]
SELECT CURRENT_DATE FROM SIMPLE_T;

--#[function-extract-epoch]
SELECT EXTRACT(EPOCH FROM timestamp_1) FROM T;

--#[function-in-where]
SELECT a FROM SIMPLE_T WHERE UPPER(c) = 'X';

--#[interval-literal]
SELECT INTERVAL '1 day' FROM SIMPLE_T;

--#[from-column-aliases]
SELECT s.x FROM (SELECT a FROM SIMPLE_T) AS s (x);

--#[type-mismatch-arith]
SELECT c + 1 FROM SIMPLE_T;


-- A scalar subquery as a function argument is not translated yet, as inside an IN list.
--#[subquery-as-nvl-argument]
SELECT NVL((SELECT MAX(b) FROM T), 0) FROM SIMPLE_T;

--#[subquery-as-nullif-argument]
SELECT NULLIF(b, (SELECT MAX(b) FROM T)) FROM SIMPLE_T;

--#[subquery-as-greatest-argument]
SELECT GREATEST(b, (SELECT MAX(b) FROM T)) FROM SIMPLE_T;

--#[subquery-as-decode-search-value]
SELECT DECODE(b, (SELECT MAX(b) FROM T), 'max', 'other') FROM SIMPLE_T;

--#[subquery-as-parenthesized-argument]
SELECT NVL(((SELECT MAX(b) FROM T)), 0) FROM SIMPLE_T;

--#[subquery-as-aggregate-argument]
SELECT MAX((SELECT MAX(b) FROM T)) FROM SIMPLE_T;
-- A subquery inside an argument expression is allowed (see the functions group); the argument itself may not be one.
--#[subquery-as-coalesce-argument]
SELECT COALESCE((SELECT MAX(b) FROM T), 0) FROM SIMPLE_T;

--#[subquery-as-nvl2-argument]
SELECT NVL2((SELECT MAX(b) FROM T), 1, 0) FROM SIMPLE_T;

--#[subquery-as-least-argument]
SELECT LEAST(b, (SELECT MAX(b) FROM T)) FROM SIMPLE_T;


-- A comparison or a boolean connective as the operand of an IS test: the PartiQL printers write `a AND b IS NULL`,
-- which reads as `a AND (b IS NULL)`. Written by the user, or by the rewrite of NVL2 and of a DECODE match on NULL.
--#[is-null-of-boolean-connective]
SELECT (a AND a) IS NULL FROM SIMPLE_T;

--#[isnull-of-boolean-connective]
SELECT (a OR a) ISNULL FROM SIMPLE_T;

--#[notnull-of-comparison]
SELECT (b = 1) NOTNULL FROM SIMPLE_T;

--#[is-true-of-boolean-connective]
SELECT (a AND a) IS TRUE FROM SIMPLE_T;

--#[is-null-of-between]
SELECT (b BETWEEN 1 AND 2) IS NULL FROM SIMPLE_T;

--#[nvl2-of-boolean-connective]
SELECT NVL2(a AND a, 1, 0) FROM SIMPLE_T;

--#[nvl2-of-not]
SELECT NVL2(NOT a, 1, 0) FROM SIMPLE_T;

--#[nvl2-of-comparison]
SELECT NVL2(b = 1, 1, 0) FROM SIMPLE_T;

--#[decode-of-boolean-connective-null-search]
SELECT DECODE(a AND a, NULL, 'none', 'some') FROM SIMPLE_T;

--#[decode-of-boolean-connective-column-search]
SELECT DECODE(a AND a, a, 'same', 'different') FROM SIMPLE_T;
