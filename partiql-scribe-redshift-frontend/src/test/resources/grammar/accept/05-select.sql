-- SELECT: the select list, WITH, FROM and joins, WHERE, GROUP BY, HAVING, UNION, ORDER BY, LIMIT and OFFSET, and
-- subqueries. https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html

--#[select-items]
SELECT a, b, c FROM t;

--#[select-alias-as]
SELECT a AS x, b AS "Quoted Alias" FROM t;

--#[select-alias-bare]
SELECT a x, t.b y FROM t;

--#[select-star]
SELECT * FROM t;

--#[select-qualified-star]
SELECT t.*, u.* FROM t, u;

--#[select-schema-qualified-star]
SELECT s.t.* FROM s.t;

--#[select-star-and-items]
SELECT *, a + 1 AS a1 FROM t;

--#[select-distinct]
SELECT DISTINCT a, b FROM t;

--#[select-all]
SELECT ALL a FROM t;

--#[select-top]
SELECT TOP 10 a FROM t ORDER BY a;

--#[select-top-distinct]
SELECT TOP 5 DISTINCT a FROM t;

--#[select-lateral-alias]
SELECT a + 1 AS a1, a1 * 2 AS a2 FROM t WHERE a1 > 0 GROUP BY a1;

--#[select-exclude]
SELECT * EXCLUDE a FROM t;

--#[select-exclude-list]
SELECT * EXCLUDE (a, b) FROM t;

--#[select-exclude-qualified-star]
SELECT t.* EXCLUDE (a) FROM t;

--#[select-exclude-qualified-columns]
SELECT a.col1, b.* EXCLUDE (b.col2, b.col3) FROM table1 AS a, table2 AS b;

--#[select-exclude-qualified-column-bare]
SELECT * EXCLUDE t.a FROM t;

--#[with]
WITH c AS (SELECT a FROM t) SELECT a FROM c;

--#[with-columns]
WITH c (x, y) AS (SELECT a, b FROM t) SELECT x, y FROM c;

--#[with-several]
WITH c1 AS (SELECT a FROM t), c2 AS (SELECT a FROM c1 WHERE a > 0) SELECT * FROM c2;

--#[with-in-subquery]
SELECT a FROM (WITH c AS (SELECT a FROM t) SELECT a FROM c) s;

--#[from-table]
SELECT a FROM t;

--#[from-qualified-table]
SELECT a FROM s.t, db.s.u;

--#[from-alias]
SELECT x.a FROM t AS x, u y;

--#[from-several]
SELECT a FROM t, u, v;

--#[from-subquery]
SELECT s.a FROM (SELECT a FROM t) AS s;

--#[from-subquery-bare-alias]
SELECT s.a FROM (SELECT a FROM t) s;

--#[from-subquery-column-aliases]
SELECT s.x, s.y FROM (SELECT a, b FROM t) AS s (x, y);

--#[from-subquery-nested]
SELECT a FROM (SELECT a FROM (SELECT a FROM t) s1) s2;

--#[join-inner]
SELECT a FROM t JOIN u ON t.id = u.id;

--#[join-inner-keyword]
SELECT a FROM t INNER JOIN u ON t.id = u.id;

--#[join-left]
SELECT a FROM t LEFT JOIN u ON t.id = u.id;

--#[join-left-outer]
SELECT a FROM t LEFT OUTER JOIN u ON t.id = u.id;

--#[join-right]
SELECT a FROM t RIGHT JOIN u ON t.id = u.id;

--#[join-right-outer]
SELECT a FROM t RIGHT OUTER JOIN u ON t.id = u.id;

--#[join-full]
SELECT a FROM t FULL JOIN u ON t.id = u.id;

--#[join-full-outer]
SELECT a FROM t FULL OUTER JOIN u ON t.id = u.id;

--#[join-cross]
SELECT a FROM t CROSS JOIN u;

--#[join-comma]
SELECT a FROM t, u WHERE t.id = u.id;

--#[join-chain]
SELECT a FROM t JOIN u ON t.id = u.id LEFT JOIN v ON u.id = v.id CROSS JOIN w;

--#[join-aliases]
SELECT x.a FROM t AS x JOIN u AS y ON x.id = y.id;

--#[join-subquery]
SELECT a FROM t JOIN (SELECT id FROM u WHERE b > 0) s ON t.id = s.id;

--#[join-parenthesized]
SELECT a FROM t JOIN (u JOIN v ON u.id = v.id) ON t.id = u.id;

--#[join-condition-complex]
SELECT a FROM t JOIN u ON t.id = u.id AND t.d BETWEEN u.d1 AND u.d2 OR u.flag;

--#[join-mixed-comma]
SELECT a FROM t JOIN u ON t.id = u.id, v;

--#[where]
SELECT a FROM t WHERE a > 0;

--#[where-complex]
SELECT a FROM t WHERE (a > 0 AND b IS NOT NULL) OR c IN (1, 2) AND d LIKE 'x%';

--#[group-by]
SELECT a, COUNT(*) FROM t GROUP BY a;

--#[group-by-several]
SELECT a, b, SUM(c) FROM t GROUP BY a, b;

--#[group-by-expression]
SELECT a + 1, DATEDIFF(day, d, CURRENT_DATE), COUNT(*) FROM t GROUP BY a + 1, DATEDIFF(day, d, CURRENT_DATE);

--#[group-by-ordinal]
SELECT a, b, COUNT(*) FROM t GROUP BY 1, 2;

--#[having]
SELECT a, COUNT(*) FROM t GROUP BY a HAVING COUNT(*) > 1;

--#[having-complex]
SELECT a, SUM(b) FROM t GROUP BY a HAVING SUM(b) > 100 AND MAX(c) IS NOT NULL;

--#[union]
SELECT a FROM t UNION SELECT a FROM u;

--#[union-all]
SELECT a FROM t UNION ALL SELECT a FROM u;

--#[union-distinct]
SELECT a FROM t UNION DISTINCT SELECT a FROM u;

--#[union-chain]
SELECT a FROM t UNION SELECT a FROM u UNION ALL SELECT a FROM v;

--#[union-parenthesized]
(SELECT a FROM t) UNION (SELECT a FROM u);

--#[union-order-limit]
SELECT a FROM t UNION SELECT a FROM u ORDER BY a LIMIT 10;

--#[union-in-subquery]
SELECT a FROM (SELECT a FROM t UNION ALL SELECT a FROM u) s;

--#[union-with]
WITH c AS (SELECT a FROM t) SELECT a FROM c UNION SELECT a FROM u;

--#[order-by]
SELECT a FROM t ORDER BY a;

--#[order-by-direction]
SELECT a FROM t ORDER BY a ASC, b DESC;

--#[order-by-nulls]
SELECT a FROM t ORDER BY a NULLS FIRST, b DESC NULLS LAST, c ASC NULLS LAST;

--#[order-by-expression]
SELECT a FROM t ORDER BY a + b, LOWER(c), CASE WHEN d THEN 0 ELSE 1 END;

--#[order-by-ordinal]
SELECT a, b FROM t ORDER BY 1, 2 DESC;

--#[order-by-alias]
SELECT a AS x FROM t ORDER BY x;

--#[limit]
SELECT a FROM t LIMIT 10;

--#[limit-all]
SELECT a FROM t LIMIT ALL;

--#[limit-offset]
SELECT a FROM t ORDER BY a LIMIT 10 OFFSET 20;

--#[offset-limit]
SELECT a FROM t ORDER BY a OFFSET 20 LIMIT 10;

--#[offset-only]
SELECT a FROM t ORDER BY a OFFSET 5;

--#[clauses-all]
WITH c AS (SELECT a, b FROM t)
SELECT DISTINCT a, COUNT(*) AS n
FROM c JOIN u ON c.a = u.a
WHERE b > 0
GROUP BY a
HAVING COUNT(*) > 1
ORDER BY n DESC
LIMIT 10 OFFSET 5;

--#[subquery-in-select]
SELECT a, (SELECT MAX(b) FROM u) AS mb FROM t;

--#[subquery-in-from]
SELECT a FROM (SELECT a FROM t WHERE b > 0) s;

--#[subquery-in-where]
SELECT a FROM t WHERE a IN (SELECT a FROM u) AND b > (SELECT AVG(b) FROM u);

--#[subquery-in-having]
SELECT a, COUNT(*) FROM t GROUP BY a HAVING COUNT(*) > (SELECT COUNT(*) FROM u);

--#[subquery-correlated-select]
SELECT a, (SELECT COUNT(*) FROM u WHERE u.t_id = t.id) AS n FROM t;

--#[subquery-correlated-where]
SELECT a FROM t WHERE b > (SELECT AVG(b) FROM u WHERE u.grp = t.grp);

--#[subquery-correlated-outer-alias]
SELECT x.a FROM t AS x WHERE x.b IN (SELECT y.b FROM u AS y WHERE y.id = x.id);
