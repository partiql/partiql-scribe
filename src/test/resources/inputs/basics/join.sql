-- Comma join (implicit cross join)
--#[join-00]
SELECT T1.a, T2.b, T1.c FROM T AS T1, T AS T2;

-- Explicit cross join
--#[join-01]
SELECT T1.b, T2.c, T1.z FROM T AS T1 CROSS JOIN T AS T2;

-- Basic join with TRUE condition
--#[join-02]
SELECT T1.c, T2.a, T1.v FROM T AS T1 JOIN T AS T2 ON TRUE;

-- Comma join with three tables
--#[join-03]
SELECT T1.a, T2.b, T3.c FROM T AS T1, T AS T2, T AS T3;

-- Cross join with three tables
--#[join-04]
SELECT T1.b, T2.c, T3.z FROM T AS T1 CROSS JOIN T AS T2 CROSS JOIN T AS T3;

-- Join with three tables using TRUE condition
--#[join-05]
SELECT T1.a, T2.v, T3.b FROM T AS T1 JOIN T AS T2 ON TRUE JOIN T AS T3 ON TRUE;

-- Left cross join
--#[join-06]
SELECT T1.c, T2.z, T1.a FROM T AS T1 LEFT CROSS JOIN T AS T2;

-- Left join with TRUE condition
--#[join-07]
SELECT T1.b, T2.c, T1.v FROM T AS T1 LEFT JOIN T AS T2 ON TRUE;

-- Full join with TRUE condition
--#[join-08]
SELECT T1.a, T2.b, T1.z FROM T AS T1 FULL JOIN T AS T2 ON TRUE;

-- Inner join with equality condition
--#[join-09]
SELECT T1.a, T2.c, T1.v FROM T AS T1 INNER JOIN T AS T2 ON T1.b = T2.b;

-- Inner join with specific columns
--#[join-10]
SELECT T1.a, T2.b FROM T AS T1 INNER JOIN T AS T2 ON T1.b = T2.b;

-- Right join
--#[join-11]
SELECT T1.c, T2.a, T2.z FROM T AS T1 RIGHT JOIN T AS T2 ON T1.b = T2.b;

-- Right outer join (explicit)
--#[join-12]
SELECT T1.b, T2.v, T1.a FROM T AS T1 RIGHT OUTER JOIN T AS T2 ON T1.b = T2.b;

-- Full outer join (explicit)
--#[join-13]
SELECT T1.c, T2.b, T1.z FROM T AS T1 FULL OUTER JOIN T AS T2 ON T1.b = T2.b;

-- Join with multiple AND conditions
--#[join-14]
SELECT T1.a, T2.v, T1.b FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b AND T1.c = T2.c;

-- Join with OR condition
--#[join-15]
SELECT T1.c, T2.z, T1.a FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b OR T1.c = T2.c;

-- Three table inner joins
--#[join-16]
SELECT T1.a, T2.c, T3.v FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b JOIN T AS T3 ON T2.b = T3.b;

-- Three table left joins
--#[join-17]
SELECT T1.b, T2.z, T3.a FROM T AS T1 LEFT JOIN T AS T2 ON T1.b = T2.b LEFT JOIN T AS T3 ON T2.b = T3.b;

-- Mixed join types (inner and left)
--#[join-18]
SELECT T1.c, T2.a, T3.v FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b LEFT JOIN T AS T3 ON T2.b = T3.b;

-- Self join
--#[join-19]
SELECT T1.a, T2.c, T1.z FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b;

-- Join with WHERE clause
--#[join-20]
SELECT T1.b, T2.v, T1.a FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b WHERE T1.c = 'active';

-- Left join with NULL check (anti-join pattern)
--#[join-21]
SELECT T1.c, T1.z, T1.a FROM T AS T1 LEFT JOIN T AS T2 ON T1.b = T2.b WHERE T2.b IS NULL;

-- Join with aggregation and GROUP BY
--#[join-22]
SELECT T1.c, COUNT(*) FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b GROUP BY T1.c;

-- Left join with aggregation
--#[join-23]
SELECT T1.b, SUM(T2.b) FROM T AS T1 LEFT JOIN T AS T2 ON T1.b = T2.b GROUP BY T1.b;

-- Join with ORDER BY
--#[join-24]
SELECT T1.a, T2.b, T1.v FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b ORDER BY T1.c;

-- Join with multiple ORDER BY columns
--#[join-25]
SELECT T1.c, T2.z, T1.a FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b ORDER BY T1.c, T2.timestamp_1 DESC;

-- Join with subquery
--#[join-26]
SELECT T1.a, T2.c, T1.v FROM T AS T1 JOIN (SELECT b, c FROM T WHERE a = true) AS T2 ON T1.b = T2.b;

-- Join with complex condition (comparison)
--#[join-27]
SELECT T1.b, T2.c, T1.z FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b AND T1.timestamp_1 > T2.timestamp_1;

-- Join with range condition
--#[join-28]
SELECT T1.a, T2.v, T1.c FROM T AS T1 JOIN T AS T2 ON T1.timestamp_1 <= T2.timestamp_1 AND T1.timestamp_2 >= T2.timestamp_1;

-- Natural join (not supported yet)
-- --#[join-29]
-- SELECT T1.a, T2.b, T1.v FROM T AS T1 NATURAL JOIN T AS T2;

-- Four table joins
--#[join-30]
SELECT T1.a, T2.c, T3.v, T4.z FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b JOIN T AS T3 ON T2.b = T3.b JOIN T AS T4 ON T3.b = T4.b;

-- Join with DISTINCT
--#[join-31]
SELECT DISTINCT T1.b FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b;

-- Join with LIMIT
--#[join-32]
SELECT T1.c, T2.a, T1.v FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b LIMIT 10;

-- Join with HAVING clause
--#[join-33]
SELECT T2.c, COUNT(*) FROM T AS T1 JOIN T AS T2 ON T1.b = T2.b GROUP BY T2.c HAVING COUNT(*) > 5;

-- Join with Path navigation
--#[join-34]
SELECT E.a FROM T AS E, E.b AS item;

--#[join-35]
SELECT E.a FROM T AS E, E."array" AS item;

-- Correlated: explicit INNER JOIN path ON TRUE
--#[join-36]
SELECT item FROM T AS E INNER JOIN E."array" AS item ON true;

-- Correlated: implicit path (unqualified)
--#[join-37]
SELECT item FROM T AS E INNER JOIN "array" AS item ON true;

-- Correlated: path lateral with ON condition
--#[join-38]
SELECT item FROM T AS E INNER JOIN E."array" AS item ON item > 1;

-- Correlated: subquery referencing LHS
--#[join-39]
SELECT T2.b FROM T AS T1 INNER JOIN (SELECT T.b FROM T WHERE T.b <= T1.b) AS T2 ON true;

-- Correlated: LEFT JOIN path lateral
--#[join-40]
SELECT E.a, item FROM T AS E LEFT JOIN E."array" AS item ON true;

-- Correlated: ON condition with LHS reference
--#[join-41]
SELECT item FROM T AS E INNER JOIN E."array" AS item ON item = E.b;

-- Correlated: chained correlated joins
--#[join-42]
SELECT item FROM EXCLUDE_T_NESTED_LIST AS E, E.a AS nested, nested.nested_list AS item;

-- Correlated: mixed correlated and non-correlated
--#[join-43]
SELECT item FROM T AS E, T AS T2, E."array" AS item;

-- Non-correlated: subquery references outer not LHS
-- A scalar subquery (used as an expression in SELECT) must return at most 1 row at runtime.
--#[join-44]
SELECT (SELECT E.b FROM T AS E INNER JOIN T AS T2 ON T2.b > O.b LIMIT 1) FROM T AS O;

-- Non-correlated despite deep nesting
--#[join-45]
SELECT T2.b FROM T AS T1 INNER JOIN (SELECT T3.b FROM T AS T3 INNER JOIN (SELECT T.b FROM T) AS T4 ON true) AS T2 ON true;

-- Non-correlated: subquery in WHERE with IN (references outer scope O)
--#[join-46]
SELECT O.b FROM T AS O WHERE O.b IN (SELECT E.b FROM T AS E INNER JOIN T AS T2 ON T2.b > O.b);

-- Non-correlated: subquery in FROM with join inside
--#[join-47]
SELECT O.b FROM T AS O WHERE O.b IN (SELECT sub.b FROM (SELECT E.b FROM T AS E INNER JOIN T AS T2 ON T2.b > E.b) AS sub WHERE sub.b > O.b);
