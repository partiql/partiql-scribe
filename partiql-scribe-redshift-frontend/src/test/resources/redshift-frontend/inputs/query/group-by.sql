-- GROUP BY keys referenced after the aggregation: the output names each referenced key with an alias, because
-- PartiQL does not resolve the key expression after GROUP BY.
--#[group-key-order-by]
SELECT c, COUNT(*) AS n FROM T GROUP BY c ORDER BY c;

--#[group-key-order-by-desc]
SELECT c, COUNT(*) AS n FROM T GROUP BY c ORDER BY c DESC, n;

--#[group-key-having]
SELECT c, COUNT(*) AS n FROM T GROUP BY c HAVING c <> 'x';

--#[group-key-having-and-order-by]
SELECT c, z, COUNT(*) AS n FROM T GROUP BY c, z HAVING z IS NOT NULL ORDER BY z;

--#[group-key-super-path]
SELECT d.e AS e, COUNT(*) AS n FROM T GROUP BY d.e ORDER BY d.e;

-- An ORDER BY or HAVING expression equal to an expression key refers to the key.
--#[group-key-expression-order-by]
SELECT UPPER(c) AS v, COUNT(*) AS n FROM T GROUP BY UPPER(c) ORDER BY UPPER(c);

--#[group-key-expression-having]
SELECT UPPER(c) AS v, COUNT(*) AS n FROM T GROUP BY UPPER(c) HAVING UPPER(c) <> 'X';

-- The qualified and the unqualified column are the same key.
--#[group-key-qualification]
SELECT UPPER(t.c) AS u, COUNT(*) AS n FROM T t GROUP BY UPPER(c);

-- The HAVING of the subquery refers to its own FROM item `t`, not to the outer one.
--#[group-key-subquery]
SELECT t.b FROM T t WHERE t.b IN (SELECT t.b FROM SIMPLE_T t GROUP BY t.b HAVING t.b > 1);

--#[group-key-window-partition]
SELECT c, z, RANK() OVER (PARTITION BY z ORDER BY c) AS r FROM T GROUP BY c, z;

--#[group-key-aggregate-order-by]
SELECT c FROM T GROUP BY c ORDER BY COUNT(*) DESC, c LIMIT 5;

-- GROUP BY and ORDER BY positions are the select items.
--#[group-by-position]
SELECT c, COUNT(*) AS n FROM T GROUP BY 1;

--#[group-by-position-expression]
SELECT UPPER(c) AS u, COUNT(*) AS n FROM T GROUP BY 1;

--#[group-by-position-out-of-range]
SELECT c FROM T GROUP BY 1, 2;

--#[group-by-position-aggregate]
SELECT COUNT(*) AS n, c FROM T GROUP BY 1;

--#[order-by-position-aggregate]
SELECT c, COUNT(*) AS n FROM T GROUP BY c ORDER BY 2 DESC;

-- Select-list aliases in GROUP BY and HAVING.
--#[group-by-alias]
SELECT b + 1 AS q, COUNT(*) AS n FROM T GROUP BY q;

--#[group-by-alias-order-by]
SELECT c AS k, COUNT(*) AS n FROM T GROUP BY k ORDER BY k;

--#[group-by-alias-of-the-same-column]
SELECT c AS c, COUNT(*) AS n FROM T GROUP BY c;

--#[group-by-alias-and-column]
SELECT UPPER(c) AS c, COUNT(*) AS n FROM T GROUP BY c;

--#[having-alias]
SELECT c, SUM(b) AS pp FROM T GROUP BY c HAVING pp > 10;

--#[having-without-group-by]
SELECT COUNT(*) AS n FROM T HAVING COUNT(*) > 1;

--#[group-by-all]
SELECT c, z, COUNT(*) AS n FROM T GROUP BY ALL;

--#[group-by-all-expression]
SELECT c, b + 1 AS x, SUM(b) AS s FROM T GROUP BY ALL;

--#[group-by-all-constant]
SELECT 'k' AS k, COUNT(*) AS n FROM T GROUP BY ALL;

-- SELECT * with GROUP BY lists the columns of the table in table order; Redshift rejects columns that are not grouped.
--#[select-star-group-by]
SELECT * FROM SIMPLE_T GROUP BY b, a, c;

--#[group-by-rollup]
SELECT c, SUM(b) AS s FROM T GROUP BY ROLLUP(c);

--#[group-by-cube]
SELECT c, z, SUM(b) AS s FROM T GROUP BY CUBE(c, z);

--#[group-by-grouping-sets]
SELECT c, z, SUM(b) AS s FROM T GROUP BY GROUPING SETS ((c), (z));
