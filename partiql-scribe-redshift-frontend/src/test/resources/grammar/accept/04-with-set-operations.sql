-- WITH (common table expressions) and set operations.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_WITH_clause.html
-- https://docs.aws.amazon.com/redshift/latest/dg/r_UNION.html

--#[with]
WITH c AS (SELECT a FROM t) SELECT a FROM c;

--#[with-several]
WITH c1 AS (SELECT a FROM t), c2 AS (SELECT a FROM c1 WHERE a > 1) SELECT * FROM c1 JOIN c2 ON c1.a = c2.a;

--#[with-column-aliases]
WITH c (x, y) AS (SELECT a, b FROM t) SELECT x, y FROM c;

--#[with-recursive]
WITH RECURSIVE r (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM r WHERE n < 10) SELECT n FROM r;

--#[with-recursive-hierarchy]
WITH RECURSIVE john_org (id, name, manager_id, level) AS (
    SELECT id, name, manager_id, 1 AS level FROM employee WHERE name = 'John'
    UNION ALL
    SELECT e.id, e.name, e.manager_id, level + 1 FROM employee e, john_org j WHERE e.manager_id = j.id
)
SELECT id, name, manager_id, level FROM john_org ORDER BY level;

--#[with-set-operation-body]
WITH c AS (SELECT a FROM t) SELECT a FROM c UNION SELECT a FROM u;

--#[with-order-limit]
WITH c AS (SELECT a FROM t) SELECT a FROM c ORDER BY a LIMIT 1;

--#[with-nested-in-subquery]
SELECT * FROM (WITH c AS (SELECT a FROM t) SELECT a FROM c) AS s;

--#[with-keyword-name]
WITH date AS (SELECT a FROM t) SELECT a FROM date;

--#[union]
SELECT a FROM t UNION SELECT a FROM u;

--#[union-all]
SELECT a FROM t UNION ALL SELECT a FROM u;

--#[union-distinct]
SELECT a FROM t UNION DISTINCT SELECT a FROM u;

--#[intersect]
SELECT a FROM t INTERSECT SELECT a FROM u;

--#[intersect-all]
SELECT a FROM t INTERSECT ALL SELECT a FROM u;

--#[except]
SELECT a FROM t EXCEPT SELECT a FROM u;

--#[except-all]
SELECT a FROM t EXCEPT ALL SELECT a FROM u;

--#[minus]
SELECT a FROM t MINUS SELECT a FROM u;

--#[set-operation-chain]
SELECT a FROM t UNION SELECT a FROM u INTERSECT SELECT a FROM v EXCEPT SELECT a FROM w;

--#[set-operation-parenthesized]
(SELECT a FROM t UNION SELECT a FROM u) INTERSECT (SELECT a FROM v EXCEPT SELECT a FROM w);

--#[set-operation-parenthesized-query]
(SELECT a FROM t);

--#[set-operation-parenthesized-with-order]
(SELECT a FROM t ORDER BY a LIMIT 5) UNION ALL (SELECT a FROM u ORDER BY a LIMIT 5);

--#[set-operation-order-limit]
SELECT a FROM t UNION SELECT a FROM u ORDER BY 1 DESC LIMIT 10 OFFSET 5;

--#[set-operation-no-from]
SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3;
