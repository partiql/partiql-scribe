-- Inputs that are not valid queries against the catalog: syntax errors, unknown names, and forms Redshift rejects.

--#[syntax-error]
SELECT a FROM;

--#[syntax-error-no-from]
SELECT 1;

--#[syntax-error-unknown-function]
SELECT ROUND(b, 2) FROM SIMPLE_T;

--#[syntax-error-two-statements]
SELECT a FROM SIMPLE_T; SELECT b FROM SIMPLE_T;

--#[unknown-column]
SELECT nope FROM SIMPLE_T;

--#[unknown-table]
SELECT a FROM nope;

--#[unknown-qualifier]
SELECT u.a FROM SIMPLE_T AS t;

-- Redshift rejects the column as ambiguous; the planner reports it as not found.
--#[ambiguous-column]
SELECT b FROM SIMPLE_T, T;

--#[ordinal-out-of-range]
SELECT a FROM SIMPLE_T ORDER BY 2;

--#[top-and-limit]
SELECT TOP 5 a FROM SIMPLE_T LIMIT 5;

--#[alias-not-visible-in-outer-query]
SELECT x FROM (SELECT a AS y FROM SIMPLE_T) AS s;

-- Redshift has no such function with this number of arguments.
--#[nullif-one-argument]
SELECT NULLIF(b) FROM SIMPLE_T;

--#[nvl2-two-arguments]
SELECT NVL2(c, 'set') FROM SIMPLE_T;

--#[decode-two-arguments]
SELECT DECODE(b, 1) FROM SIMPLE_T;

-- Redshift identifiers are at most 127 bytes; a longer one is refused before the planner sees it.
--#[identifier-longer-than-127-bytes]
SELECT b AS xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx FROM SIMPLE_T;

--#[upper-two-arguments]
SELECT UPPER(c, c) FROM SIMPLE_T;

--#[concat-one-argument]
SELECT CONCAT(c) FROM SIMPLE_T;

--#[concat-three-arguments]
SELECT CONCAT(c, c, c) FROM SIMPLE_T;

--#[power-one-argument]
SELECT POWER(b) FROM SIMPLE_T;
