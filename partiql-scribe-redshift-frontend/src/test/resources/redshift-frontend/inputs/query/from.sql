--#[comma-join]
SELECT t.b, s.a FROM T t, SIMPLE_T s WHERE t.b = s.b;

--#[full-outer-join]
SELECT t1.b, t2.c FROM T t1 FULL OUTER JOIN SIMPLE_T t2 ON t1.b = t2.b;

--#[cross-join]
SELECT t1.b, t2.c FROM T t1 CROSS JOIN SIMPLE_T t2;

--#[right-join]
SELECT t1.b, t2.c FROM T t1 RIGHT JOIN SIMPLE_T t2 ON t1.b = t2.b;

--#[join-without-condition]
SELECT t1.b FROM T t1 JOIN SIMPLE_T t2;

-- A parenthesized join on the right of a join.
--#[parenthesized-join]
SELECT t.b, u.col_int32 FROM T t LEFT JOIN (SIMPLE_T s JOIN T_ALL_TYPES u ON s.b = u.col_int32) ON t.b = s.b;

-- JOIN USING is JOIN ON; a reference to a join column is the column of the left table, the right table for a RIGHT
-- join, and COALESCE of both for a FULL join.
--#[join-using]
SELECT b, t.c, simple_t.a FROM T JOIN SIMPLE_T USING (b);

--#[join-using-left]
SELECT b, c FROM T LEFT JOIN SIMPLE_T s USING (b, c);

--#[join-using-right]
SELECT b FROM T RIGHT JOIN SIMPLE_T USING (b);

--#[join-using-full]
SELECT b FROM T FULL JOIN SIMPLE_T USING (b);

--#[join-using-unknown-column]
SELECT b FROM T JOIN SIMPLE_T USING (z);

--#[join-using-star]
SELECT * FROM T JOIN SIMPLE_T USING (b);

--#[natural-join]
SELECT b FROM T NATURAL JOIN SIMPLE_T;

-- A subquery in FROM: the alias is optional, and column aliases rename the select items.
--#[subquery-without-alias]
SELECT b FROM (SELECT b FROM T);

--#[subquery-column-aliases]
SELECT s.x FROM (SELECT b, c FROM T) AS s(x, y);

--#[subquery-fewer-column-aliases]
SELECT x, c FROM (SELECT b, c FROM T) AS s(x);

--#[subquery-column-aliases-set-operation]
SELECT x FROM (SELECT b FROM T UNION SELECT b FROM SIMPLE_T) AS s(x);

--#[subquery-not-lateral]
SELECT s.v FROM T AS t, (SELECT b AS v FROM SIMPLE_T) AS s;

--#[table-column-aliases]
SELECT p FROM SIMPLE_T AS s(p, q, r);

--#[table-names]
SELECT t.b FROM "default".t;

--#[table-star]
SELECT b FROM T *;

--#[temporary-table]
SELECT * FROM #tmp;

--#[select-into]
SELECT b INTO newt FROM T;

--#[connect-by]
SELECT b FROM T START WITH b = 1 CONNECT BY PRIOR b = b;

--#[pivot]
SELECT * FROM (SELECT c, b FROM T) PIVOT (SUM(b) FOR c IN ('x', 'y'));

--#[unpivot]
SELECT * FROM (SELECT col_int32, col_int64 FROM T_ALL_TYPES) UNPIVOT (v FOR n IN (col_int32, col_int64));

-- SUPER arrays: a FROM item over an array of an earlier FROM item unnests the array. Redshift unnests only arrays, so
-- the source is CASE WHEN x IS LIST THEN x ELSE [] END, and the root of the source is @t.
--#[unnest]
SELECT t.b, e FROM T t, t.x AS e;

--#[unnest-path-and-navigation]
SELECT t.b, e.a AS a FROM T t, t.x.items e;

--#[unnest-nested]
SELECT el, f FROM T t, t.x AS el, el AS f;

-- T has a column `a`, so the name `a` is either the column or the unnested element.
--#[unnest-nested-ambiguous]
SELECT a, f FROM T t, t.x AS a, a AS f;

-- The alias t1 is also the name of a catalog table.
--#[unnest-alias-of-table-name]
SELECT t1.b, e FROM T t1, t1.x AS e;

--#[unnest-left-join]
SELECT t.b, e FROM T t LEFT JOIN t.x e ON TRUE;

--#[unnest-join-without-condition]
SELECT t.b, e FROM T t JOIN t.x e;

--#[unnest-in-subquery]
SELECT t.b FROM T t WHERE t.b IN (SELECT e FROM t.x AS e);

--#[unnest-function]
SELECT o.b, up.product FROM T o, UNNEST(o.x) AS up(product);

--#[unnest-function-with-offset]
SELECT up.product, up.idx FROM T o, UNNEST(o.x) WITH OFFSET AS up(product, idx);

--#[unnest-at-index]
SELECT e, i FROM T t, t.x AS e AT i;

--#[unnest-star]
SELECT * FROM T t, t.x AS e;

--#[object-unpivot]
SELECT k, v FROM T t, UNPIVOT t.x AS v AT k;
