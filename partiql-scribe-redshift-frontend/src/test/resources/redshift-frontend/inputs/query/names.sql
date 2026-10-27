-- A lateral column alias is the expression of the earlier item, unless a FROM item has a column of that name.
--#[lateral-alias]
SELECT b + 1 AS q, q * 2 AS y FROM T;

--#[lateral-alias-column-wins]
SELECT b + 1 AS c, c AS y FROM T;

--#[lateral-alias-aggregate]
SELECT SUM(b) AS s, s * 2 AS d FROM T;

-- Select-list aliases in WHERE; a name that is also a column is unsupported, because Redshift does not document which
-- one it uses.
--#[where-alias]
SELECT b + 1 AS q FROM T WHERE q > 2;

--#[where-alias-and-column]
SELECT b + 1 AS b FROM T WHERE b > 2;

--#[where-alias-aggregate]
SELECT COUNT(*) AS n FROM T WHERE n > 1;

-- ORDER BY: an output column name first, then an input column; a position is a select item.
--#[order-by-alias]
SELECT b + 1 AS c FROM T ORDER BY c;

--#[order-by-alias-expression]
SELECT b + 1 AS q FROM T ORDER BY q * 2;

--#[order-by-position]
SELECT b, c FROM T ORDER BY 2, 1 DESC;

--#[order-by-position-expression]
SELECT b + 1 AS x FROM T ORDER BY 1 DESC;

--#[order-by-position-star]
SELECT * FROM SIMPLE_T ORDER BY 3;

--#[order-by-position-out-of-range]
SELECT b FROM T ORDER BY 2;

--#[order-by-not-selected]
SELECT b FROM T ORDER BY c DESC NULLS LAST;

--#[order-by-ambiguous-name]
SELECT b AS x, c AS x FROM T ORDER BY x;

-- A column name equal to a FROM alias or a table name is qualified, because PartiQL reads the name as the FROM item.
--#[column-named-like-alias]
SELECT b FROM T AS b;

--#[column-named-like-outer-alias]
SELECT b.c FROM T AS b WHERE b.b IN (SELECT b FROM SIMPLE_T);

--#[column-named-like-earlier-from-item]
SELECT q.v FROM T AS b, (SELECT b AS v FROM SIMPLE_T) AS q;

-- A correlated reference to an outer FROM item whose alias an inner FROM item repeats: the outer alias is renamed.
--#[correlated-reference-repeated-alias]
SELECT t.b, (SELECT MAX(t.b) FROM SIMPLE_T t WHERE t.c = z) AS m FROM T t;

--#[unknown-qualified-column]
SELECT t.nope FROM T t;

--#[ambiguous-column]
SELECT b FROM T, SIMPLE_T;

--#[from-alias-as-value]
SELECT s FROM SIMPLE_T AS s;

--#[schema-qualified-column]
SELECT "default".t.b FROM "default".t;

-- * with other items, EXCLUDE, and SELECT * over a subquery with repeated column names.
--#[star-and-item]
SELECT *, b AS b2 FROM SIMPLE_T;

--#[item-and-star]
SELECT b AS b2, * FROM SIMPLE_T;

--#[star-join]
SELECT * FROM SIMPLE_T s1 JOIN SIMPLE_T s2 ON s1.b = s2.b;

--#[qualified-star]
SELECT s.*, t.b FROM SIMPLE_T s, T t;

--#[exclude]
SELECT * EXCLUDE c, d FROM T;

--#[exclude-parenthesized]
SELECT * EXCLUDE (a, c) FROM SIMPLE_T;

--#[exclude-unknown-column]
SELECT * EXCLUDE nope FROM SIMPLE_T;

--#[exclude-select-list]
SELECT a, b EXCLUDE a FROM SIMPLE_T;

--#[star-repeated-names]
SELECT * FROM (SELECT b AS x, c AS x FROM T) AS s;

--#[star-of-super-value]
SELECT t.x.* FROM T t;
