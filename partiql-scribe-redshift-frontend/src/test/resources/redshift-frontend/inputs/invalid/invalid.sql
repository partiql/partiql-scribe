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

--#[ambiguous-column]
SELECT b FROM SIMPLE_T, T;

--#[ordinal-out-of-range]
SELECT a FROM SIMPLE_T ORDER BY 2;

--#[top-and-limit]
SELECT TOP 5 a FROM SIMPLE_T LIMIT 5;

--#[alias-not-visible-in-outer-query]
SELECT x FROM (SELECT a AS y FROM SIMPLE_T) AS s;
