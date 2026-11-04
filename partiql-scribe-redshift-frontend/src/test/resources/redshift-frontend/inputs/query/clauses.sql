-- WHERE, GROUP BY, HAVING, UNION, ORDER BY, LIMIT and OFFSET, lateral aliases, and subqueries.

--#[where]
SELECT a FROM SIMPLE_T WHERE b > 0;

--#[where-complex]
SELECT a FROM SIMPLE_T WHERE (b > 0 AND c IS NOT NULL) OR a AND c LIKE 'x%';

--#[group-by]
SELECT c, COUNT(*) FROM SIMPLE_T GROUP BY c;

--#[group-by-several]
SELECT a, c, SUM(b) FROM SIMPLE_T GROUP BY a, c;

--#[group-by-expression]
SELECT b + 1, COUNT(*) FROM SIMPLE_T GROUP BY b + 1;

--#[group-by-ordinal]
SELECT c, a, COUNT(*) FROM SIMPLE_T GROUP BY 1, 2;

--#[group-by-ordinal-of-expression]
SELECT b * 2 AS twice, COUNT(*) FROM SIMPLE_T GROUP BY 1;

--#[group-by-ordinal-out-of-range]
SELECT c, COUNT(*) FROM SIMPLE_T GROUP BY 3;

--#[group-by-ordinal-of-star]
SELECT *, COUNT(*) FROM SIMPLE_T GROUP BY 1;

--#[having]
SELECT c, COUNT(*) FROM SIMPLE_T GROUP BY c HAVING COUNT(*) > 1;

--#[having-complex]
SELECT c, SUM(b) FROM SIMPLE_T GROUP BY c HAVING SUM(b) > 100 AND MAX(b) IS NOT NULL;

--#[union]
SELECT b FROM SIMPLE_T UNION SELECT b FROM T;

--#[union-all]
SELECT b FROM SIMPLE_T UNION ALL SELECT b FROM T;

--#[union-chain]
SELECT b FROM SIMPLE_T UNION SELECT b FROM T UNION ALL SELECT da FROM T_DECIMALS;

--#[union-parenthesized]
(SELECT b FROM SIMPLE_T) UNION (SELECT b FROM T);

--#[union-order-limit]
SELECT b FROM SIMPLE_T UNION SELECT b FROM T ORDER BY b LIMIT 10;

--#[union-in-subquery]
SELECT s.b FROM (SELECT b FROM SIMPLE_T UNION ALL SELECT b FROM T) AS s;

--#[order-by]
SELECT a FROM SIMPLE_T ORDER BY a;

--#[order-by-direction]
SELECT a, b FROM SIMPLE_T ORDER BY a ASC, b DESC;

--#[order-by-nulls]
SELECT a, b FROM SIMPLE_T ORDER BY a NULLS FIRST, b DESC NULLS LAST;

--#[order-by-expression]
SELECT a FROM SIMPLE_T ORDER BY b + 1, CASE WHEN a THEN 0 ELSE 1 END;

--#[order-by-ordinal]
SELECT a, b FROM SIMPLE_T ORDER BY 2 DESC, 1;

--#[order-by-alias]
SELECT b + 1 AS x FROM SIMPLE_T ORDER BY x;

--#[order-by-alias-shadows-column]
SELECT b + 1 AS c FROM SIMPLE_T ORDER BY c;

--#[limit]
SELECT a FROM SIMPLE_T LIMIT 10;

--#[limit-all]
SELECT a FROM SIMPLE_T LIMIT ALL;

--#[limit-offset]
SELECT a FROM SIMPLE_T ORDER BY a LIMIT 10 OFFSET 20;

--#[offset-limit]
SELECT a FROM SIMPLE_T ORDER BY a OFFSET 20 LIMIT 10;

--#[offset-only]
SELECT a FROM SIMPLE_T ORDER BY a OFFSET 5;

--#[distinct-limit]
SELECT DISTINCT c FROM SIMPLE_T ORDER BY c LIMIT 5;

--#[distinct-offset]
SELECT DISTINCT a, b FROM SIMPLE_T OFFSET 2;

--#[distinct-top]
SELECT TOP 3 DISTINCT c FROM SIMPLE_T;

--#[distinct-limit-order-by-alias]
SELECT DISTINCT b + 1 AS x FROM SIMPLE_T ORDER BY x DESC LIMIT 5;

--#[distinct-limit-order-by-ordinal]
SELECT DISTINCT a, b FROM SIMPLE_T ORDER BY 2, 1 LIMIT 5;

--#[distinct-limit-order-by-star]
SELECT DISTINCT * FROM SIMPLE_T ORDER BY c LIMIT 5;

--#[distinct-limit-order-by-expression]
SELECT DISTINCT a FROM SIMPLE_T ORDER BY b + 1 LIMIT 5;

--#[parenthesized-query-order-by]
(SELECT a FROM SIMPLE_T ORDER BY a) ORDER BY a;

-- --#[lateral-alias-where] TODO(redshift)
SELECT b + 1 AS next FROM SIMPLE_T WHERE next > 10;

-- --#[lateral-alias-select] TODO(redshift)
SELECT b + 1 AS next, next * 2 AS after FROM SIMPLE_T;

-- --#[lateral-alias-group-by] TODO(redshift)
SELECT b * 2 AS twice, COUNT(*) FROM SIMPLE_T GROUP BY twice;

--#[lateral-alias-column-wins]
SELECT b + 1 AS c FROM SIMPLE_T WHERE c = 'x';

--#[all-clauses]
WITH c AS (SELECT a, b, c FROM SIMPLE_T)
SELECT DISTINCT c.c, COUNT(*) AS n
FROM c JOIN T AS u ON c.b = u.b
WHERE c.b > 0
GROUP BY c.c
HAVING COUNT(*) > 1
ORDER BY n DESC
LIMIT 10 OFFSET 5;

--#[subquery-scalar]
SELECT a, (SELECT MAX(b) FROM T) AS mb FROM SIMPLE_T;

--#[subquery-scalar-in-where]
SELECT a FROM SIMPLE_T WHERE b > (SELECT AVG(b) FROM T);

--#[subquery-in-having]
SELECT c, COUNT(*) FROM SIMPLE_T GROUP BY c HAVING COUNT(*) > (SELECT COUNT(*) FROM T);

--#[subquery-correlated-select]
SELECT t.a, (SELECT COUNT(*) FROM T AS u WHERE u.b = t.b) AS n FROM SIMPLE_T AS t;

--#[subquery-correlated-where]
SELECT t.a FROM SIMPLE_T AS t WHERE t.b > (SELECT AVG(u.b) FROM T AS u WHERE u.c = t.c);

--#[subquery-correlated-in]
SELECT t.a FROM SIMPLE_T AS t WHERE t.b IN (SELECT u.b FROM T AS u WHERE u.c = t.c);

--#[order-by-alias-in-expression]
SELECT b + 1 AS x FROM SIMPLE_T ORDER BY x * 2;

--#[order-by-alias-in-expression-column-wins]
SELECT b + 1 AS c FROM SIMPLE_T ORDER BY c || 'x';

-- The key is an item's expression, so the outer query sorts by that item's output name.
--#[distinct-limit-order-by-qualified-column]
SELECT DISTINCT x.b, y.b AS yb FROM T AS x JOIN T AS y ON x.a = y.a ORDER BY y.b LIMIT 5;

--#[distinct-limit-order-by-item-expression]
SELECT DISTINCT b + 1 AS x FROM SIMPLE_T ORDER BY b + 1 LIMIT 5;

--#[distinct-limit-order-by-ambiguous-name]
SELECT DISTINCT x.b, y.b FROM T AS x JOIN T AS y ON x.a = y.a ORDER BY b LIMIT 5;

--#[order-by-ambiguous-alias]
SELECT b AS x, c AS x FROM SIMPLE_T ORDER BY x;

--#[order-by-ordinal-of-star]
SELECT * FROM SIMPLE_T ORDER BY 1;

--#[order-by-ordinal-not-an-int]
SELECT a FROM SIMPLE_T ORDER BY 99999999999;

--#[limit-too-large]
SELECT a FROM SIMPLE_T LIMIT 99999999999999999999;
