-- FROM: tables, subqueries, joins, and WITH.

--#[from-alias]
SELECT x.a FROM SIMPLE_T AS x;

--#[from-alias-bare]
SELECT x.a FROM SIMPLE_T x;

--#[from-qualified-table]
SELECT a FROM default.SIMPLE_T;

--#[from-subquery]
SELECT s.a FROM (SELECT a FROM SIMPLE_T) AS s;

--#[from-subquery-bare-alias]
SELECT s.a FROM (SELECT a, b FROM SIMPLE_T WHERE b > 0) s;

--#[from-subquery-no-alias]
SELECT a FROM (SELECT a FROM SIMPLE_T);

--#[from-comma-join]
SELECT t.a, u.c FROM SIMPLE_T AS t, T AS u WHERE t.b = u.b;

--#[join-inner]
SELECT t.a, u.c FROM SIMPLE_T AS t JOIN T AS u ON t.b = u.b;

--#[join-inner-keyword]
SELECT t.a FROM SIMPLE_T AS t INNER JOIN T AS u ON t.b = u.b;

--#[join-left]
SELECT t.a, u.c FROM SIMPLE_T AS t LEFT JOIN T AS u ON t.b = u.b;

--#[join-left-outer]
SELECT t.a FROM SIMPLE_T AS t LEFT OUTER JOIN T AS u ON t.b = u.b;

--#[join-right]
SELECT t.a FROM SIMPLE_T AS t RIGHT JOIN T AS u ON t.b = u.b;

--#[join-full-outer]
SELECT t.a FROM SIMPLE_T AS t FULL OUTER JOIN T AS u ON t.b = u.b;

--#[join-cross]
SELECT t.a, u.c FROM SIMPLE_T AS t CROSS JOIN T AS u;

--#[join-chain]
SELECT t.a FROM SIMPLE_T AS t JOIN T AS u ON t.b = u.b LEFT JOIN T_DECIMALS AS d ON u.b = d.da;

--#[join-condition-complex]
SELECT t.a FROM SIMPLE_T AS t JOIN T AS u ON t.b = u.b AND u.c IS NOT NULL OR t.a;

--#[join-subquery]
SELECT t.a FROM SIMPLE_T AS t JOIN (SELECT b FROM T WHERE a) AS s ON t.b = s.b;

--#[join-parenthesized]
SELECT t.a FROM SIMPLE_T AS t JOIN (T AS u JOIN T_DECIMALS AS d ON u.b = d.da) ON t.b = u.b;

--#[join-parenthesized-alias]
SELECT t.a FROM SIMPLE_T AS t JOIN (T AS u JOIN T_DECIMALS AS d ON u.b = d.da) AS j ON t.b = j.b;

--#[with]
WITH c AS (SELECT a, b FROM SIMPLE_T) SELECT a FROM c;

--#[with-columns]
WITH c (x, y) AS (SELECT a, b FROM SIMPLE_T) SELECT x FROM c;

--#[with-several]
WITH c1 AS (SELECT a, b FROM SIMPLE_T), c2 AS (SELECT b FROM c1 WHERE a) SELECT b FROM c2;

--#[with-shadows-table]
WITH T AS (SELECT a FROM SIMPLE_T) SELECT a FROM T;

--#[with-in-subquery]
SELECT s.a FROM (WITH c AS (SELECT a FROM SIMPLE_T) SELECT a FROM c) AS s;

--#[with-join]
WITH c AS (SELECT b FROM SIMPLE_T) SELECT t.a FROM T AS t JOIN c ON t.b = c.b;

-- A common table expression sees the ones before it; a later name is a table.
--#[with-later-cte-is-a-table]
WITH x AS (SELECT * FROM SIMPLE_T), simple_t AS (SELECT a FROM T) SELECT a FROM x;

--#[with-shadows-table-of-same-name]
WITH t AS (SELECT a FROM T) SELECT a FROM t;
