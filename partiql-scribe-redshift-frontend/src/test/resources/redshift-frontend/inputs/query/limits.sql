--#[offset-without-limit]
SELECT b FROM T ORDER BY b OFFSET 5;

--#[limit-all]
SELECT b FROM T ORDER BY b LIMIT ALL;

--#[offset-before-limit]
SELECT b FROM T ORDER BY b OFFSET 5 LIMIT 10;

--#[limit-zero]
SELECT b FROM T LIMIT 0;

--#[limit-column]
SELECT b FROM T LIMIT b;

--#[limit-negative]
SELECT b FROM T LIMIT -1;

--#[limit-expression]
SELECT b FROM T LIMIT 1 + 1;

--#[limit-null]
SELECT b FROM T LIMIT NULL;

--#[limit-too-large]
SELECT b FROM T LIMIT 2147483648;

--#[offset-column]
SELECT b FROM T OFFSET b;

-- PartiQL applies DISTINCT after LIMIT and OFFSET, so a DISTINCT query with LIMIT or OFFSET is a subquery.
--#[distinct-limit]
SELECT DISTINCT b FROM T ORDER BY b LIMIT 2;

--#[distinct-top]
SELECT TOP 5 DISTINCT c FROM T;

--#[distinct-order-by-position-offset]
SELECT DISTINCT b, c FROM T ORDER BY 2 DESC LIMIT 10 OFFSET 5;

--#[distinct-order-by-name]
SELECT DISTINCT b AS x FROM T ORDER BY x LIMIT 3;

--#[distinct-order-by-not-selected]
SELECT DISTINCT b FROM T ORDER BY c LIMIT 2;

--#[distinct-star-limit]
SELECT DISTINCT * FROM SIMPLE_T LIMIT 5;

--#[distinct-repeated-names-limit]
SELECT DISTINCT t1.b, t2.b FROM T t1 JOIN SIMPLE_T t2 ON t1.c = t2.c ORDER BY 1 LIMIT 3;

--#[distinct-without-limit]
SELECT DISTINCT b FROM T ORDER BY b;

--#[top]
SELECT TOP 2 * FROM SIMPLE_T ORDER BY b;

--#[select-without-from]
SELECT 1 AS v;

--#[select-without-from-subquery]
SELECT b FROM T WHERE b = (SELECT 1 AS one);

--#[select-star-without-from]
SELECT *;

--#[trailing-semicolons]
SELECT b FROM T;;

--#[multiple-statements]
SELECT b FROM T; SELECT c FROM T;
