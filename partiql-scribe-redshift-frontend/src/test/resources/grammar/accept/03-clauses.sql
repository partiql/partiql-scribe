-- WHERE, GROUP BY, HAVING, QUALIFY, CONNECT BY, ORDER BY, LIMIT, and OFFSET.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html

--#[where]
SELECT a FROM t WHERE b > 1;

--#[where-compound]
SELECT a FROM t WHERE b > 1 AND (c = 'x' OR c IS NULL) AND NOT d;

--#[where-subquery]
SELECT a FROM t WHERE b IN (SELECT b FROM u) AND EXISTS (SELECT 1 FROM v WHERE v.id = t.id);

--#[group-by-columns]
SELECT a, b, COUNT(*) FROM t GROUP BY a, b;

--#[group-by-ordinals]
SELECT a, b, COUNT(*) FROM t GROUP BY 1, 2;

--#[group-by-expression]
SELECT DATE_TRUNC('month', d), SUM(x) FROM t GROUP BY DATE_TRUNC('month', d);

--#[group-by-all]
SELECT a, b, SUM(x) FROM t GROUP BY ALL;

--#[group-by-rollup]
SELECT a, b, SUM(x) FROM t GROUP BY ROLLUP (a, b);

--#[group-by-cube]
SELECT a, b, SUM(x) FROM t GROUP BY CUBE (a, b);

--#[group-by-grouping-sets]
SELECT a, b, SUM(x) FROM t GROUP BY GROUPING SETS ((a, b), (a), ());

--#[group-by-mixed]
SELECT a, b, c, SUM(x) FROM t GROUP BY a, ROLLUP (b, c);

--#[group-by-empty]
SELECT SUM(x) FROM t GROUP BY ();

--#[having]
SELECT a, COUNT(*) FROM t GROUP BY a HAVING COUNT(*) > 1;

--#[having-without-group-by]
SELECT COUNT(*) FROM t HAVING COUNT(*) > 0;

--#[having-complex]
SELECT a, SUM(x) FROM t GROUP BY a HAVING SUM(x) > 100 AND MIN(x) IS NOT NULL;

--#[qualify]
SELECT a, ROW_NUMBER() OVER (PARTITION BY b ORDER BY c) AS rn FROM t QUALIFY rn = 1;

--#[qualify-window-inline]
SELECT a FROM t QUALIFY ROW_NUMBER() OVER (PARTITION BY b ORDER BY c DESC) <= 3;

--#[qualify-after-having]
SELECT a, COUNT(*) AS n FROM t GROUP BY a HAVING n > 1 QUALIFY RANK() OVER (ORDER BY n DESC) = 1;

--#[connect-by]
SELECT id, name, LEVEL FROM employee START WITH manager_id IS NULL CONNECT BY PRIOR id = manager_id;

--#[connect-by-without-start]
SELECT id FROM employee CONNECT BY PRIOR id = manager_id ORDER BY id;

--#[order-by]
SELECT a FROM t ORDER BY a;

--#[order-by-directions]
SELECT a, b FROM t ORDER BY a ASC, b DESC;

--#[order-by-nulls]
SELECT a, b FROM t ORDER BY a NULLS FIRST, b DESC NULLS LAST;

--#[order-by-ordinal]
SELECT a, b FROM t ORDER BY 2, 1 DESC;

--#[order-by-expression]
SELECT a FROM t ORDER BY LOWER(a), a + 1, CASE WHEN b THEN 0 ELSE 1 END;

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

--#[limit-expression]
SELECT a FROM t LIMIT 5 + 5 OFFSET 2 * 10;

--#[limit-in-subquery]
SELECT * FROM (SELECT a FROM t ORDER BY a LIMIT 10) AS s;

--#[all-clauses]
SELECT a, COUNT(*) AS n
FROM t
WHERE b > 0
GROUP BY a
HAVING COUNT(*) > 1
QUALIFY ROW_NUMBER() OVER (ORDER BY n DESC) <= 10
ORDER BY n DESC, a
LIMIT 10 OFFSET 0;
