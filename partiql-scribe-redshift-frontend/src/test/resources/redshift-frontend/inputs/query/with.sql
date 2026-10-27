-- A reference to a common table expression becomes the query of the common table expression.
--#[with]
WITH x AS (SELECT b FROM T) SELECT b FROM x;

-- A common table expression with the name of a table hides the table.
--#[with-shadows-table]
WITH simple_t AS (SELECT b FROM T WHERE b > 1) SELECT b FROM simple_t;

--#[with-subquery-reference]
WITH x AS (SELECT b FROM T) SELECT b FROM x WHERE b = (SELECT MAX(b) FROM x);

--#[with-sibling-and-join]
WITH x AS (SELECT b FROM T), y AS (SELECT b FROM x WHERE b > 1) SELECT y.b FROM y JOIN x ON y.b = x.b;

--#[with-alias]
WITH x AS (SELECT b, c FROM T) SELECT q.c FROM x AS q WHERE q.b > 1;

--#[with-column-list]
WITH x(q) AS (SELECT b, c FROM T) SELECT q FROM x;

--#[with-column-list-star]
WITH x(q, r) AS (SELECT * FROM SIMPLE_T) SELECT q, r, c FROM x;

--#[with-column-list-too-long]
WITH x(q, r) AS (SELECT b FROM T) SELECT q FROM x;

--#[with-set-operation]
WITH x AS (SELECT b FROM T UNION SELECT b FROM SIMPLE_T) SELECT b FROM x ORDER BY b;

--#[with-in-subquery]
SELECT a FROM T WHERE b IN (WITH x AS (SELECT b FROM SIMPLE_T) SELECT b FROM x);

--#[with-recursive]
WITH RECURSIVE x(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM x WHERE n < 3) SELECT n FROM x;
