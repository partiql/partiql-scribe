--#[select-00]
SELECT a, b FROM T;

--#[select-01]
SELECT t.a AS x, t.b FROM T AS t;

--#[select-02]
SELECT * FROM T;

--#[select-03]
SELECT DISTINCT b FROM T t;

-- Redshift folds unquoted and quoted identifiers to lower case by default.
--#[select-04]
select A, "B" from t;

--#[select-05]
SELECT t.* FROM T t;

--#[where-00]
SELECT a FROM T WHERE b > 1 AND c <> 'x';

--#[where-01]
SELECT a FROM T WHERE b != 1 OR NOT a;

--#[where-02]
SELECT a FROM T WHERE b BETWEEN 1 AND 10;

--#[where-03]
SELECT a FROM T WHERE b NOT IN (1, 2, 3);

--#[where-04]
SELECT a FROM T WHERE c LIKE 'a%' AND z IS NOT NULL;

--#[where-05]
SELECT a FROM T WHERE c = 'it''s';

-- Redshift sorts NULL values last for ASC and first for DESC.
--#[order-00]
SELECT b FROM T ORDER BY b;

--#[order-01]
SELECT b FROM T ORDER BY b DESC;

--#[order-02]
SELECT b FROM T ORDER BY b DESC NULLS LAST LIMIT 10 OFFSET 5;

--#[top-00]
SELECT TOP 5 b FROM T;

--#[group-00]
SELECT c, COUNT(*) AS n, SUM(b) AS total FROM T GROUP BY c HAVING COUNT(*) > 1;

--#[group-01]
SELECT COUNT(DISTINCT c) AS n FROM T;

--#[join-00]
SELECT t1.a, t2.b FROM T t1 JOIN T t2 ON t1.b = t2.b;

--#[join-01]
SELECT t1.a, t2.b FROM T t1 LEFT OUTER JOIN T t2 ON t1.b = t2.b;

--#[subquery-00]
SELECT a FROM T WHERE b IN (SELECT b FROM T);

--#[subquery-01]
SELECT s.b FROM (SELECT b FROM T WHERE a) AS s;

--#[case-00]
SELECT CASE WHEN b > 0 THEN 'pos' WHEN b < 0 THEN 'neg' ELSE 'zero' END AS sign FROM T;

--#[case-01]
SELECT CASE b WHEN 1 THEN 'one' END AS n FROM T;

--#[comment-00]
SELECT a -- the a column
FROM /* the table */ T;
