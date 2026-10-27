-- The FROM clause: tables, aliases, subqueries, joins, UNNEST, SUPER navigation, PIVOT and UNPIVOT.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause30.html

--#[from-table]
SELECT a FROM t;

--#[from-qualified]
SELECT a FROM schema1.t;

--#[from-three-part]
SELECT a FROM db.schema1.t;

--#[from-quoted]
SELECT a FROM "My Schema"."My Table";

--#[from-alias]
SELECT t1.a FROM t AS t1;

--#[from-alias-bare]
SELECT t1.a FROM t t1;

--#[from-alias-columns]
SELECT s.x, s.y FROM t AS s (x, y);

--#[from-list]
SELECT a, b FROM t, u, v AS w;

--#[from-subquery]
SELECT s.a FROM (SELECT a FROM t) AS s;

--#[from-subquery-bare-alias]
SELECT s.a FROM (SELECT a FROM t) s;

--#[from-subquery-no-alias]
SELECT a FROM (SELECT a FROM t);

--#[from-subquery-column-aliases]
SELECT s.x FROM (SELECT a FROM t) AS s (x);

--#[from-subquery-with]
SELECT s.a FROM (WITH c AS (SELECT a FROM t) SELECT a FROM c) AS s;

--#[from-subquery-set-op]
SELECT s.a FROM (SELECT a FROM t UNION SELECT a FROM u) AS s;

--#[from-temp-table]
SELECT a FROM #tmp;

--#[from-temp-table-alias]
SELECT x.a FROM #tmp AS x;

--#[join-inner]
SELECT t.a, u.b FROM t INNER JOIN u ON t.id = u.id;

--#[join-bare]
SELECT t.a, u.b FROM t JOIN u ON t.id = u.id;

--#[join-left]
SELECT t.a, u.b FROM t LEFT JOIN u ON t.id = u.id;

--#[join-left-outer]
SELECT t.a, u.b FROM t LEFT OUTER JOIN u ON t.id = u.id;

--#[join-right]
SELECT t.a, u.b FROM t RIGHT OUTER JOIN u ON t.id = u.id;

--#[join-full]
SELECT t.a, u.b FROM t FULL JOIN u ON t.id = u.id;

--#[join-full-outer]
SELECT t.a, u.b FROM t FULL OUTER JOIN u ON t.id = u.id;

--#[join-cross]
SELECT t.a, u.b FROM t CROSS JOIN u;

--#[join-natural]
SELECT a FROM t NATURAL JOIN u;

--#[join-natural-left]
SELECT a FROM t NATURAL LEFT OUTER JOIN u;

--#[join-using]
SELECT a FROM t JOIN u USING (id);

--#[join-using-list]
SELECT a FROM t LEFT JOIN u USING (id, region);

--#[join-chain]
SELECT * FROM a JOIN b ON a.x = b.x LEFT JOIN c ON b.y = c.y FULL JOIN d ON c.z = d.z CROSS JOIN e;

--#[join-condition-complex]
SELECT * FROM t JOIN u ON t.id = u.id AND t.d BETWEEN u.lo AND u.hi OR u.flag IS NULL;

--#[join-subquery]
SELECT t.a FROM t JOIN (SELECT id FROM u WHERE x > 1) AS s ON t.id = s.id;

--#[join-parenthesized]
SELECT * FROM a JOIN (b JOIN c ON b.y = c.y) ON a.x = b.x;

--#[join-parenthesized-alias]
SELECT * FROM (a JOIN b ON a.x = b.x) AS ab;

--#[join-comma-and-join]
SELECT * FROM t, u JOIN v ON u.id = v.id;

--#[join-no-condition]
SELECT * FROM t JOIN u;

--#[unnest]
SELECT i FROM t, UNNEST(t.items) AS i;

--#[unnest-with-offset]
SELECT i, o FROM t, UNNEST(t.items) WITH OFFSET AS i (i, o);

--#[super-navigation]
SELECT i.name FROM t, t.items AS i;

--#[super-navigation-bare-alias]
SELECT i.name FROM t, t.items i;

--#[super-navigation-at-index]
SELECT i, idx FROM t, t.items AS i AT idx;

--#[super-navigation-nested]
SELECT x FROM t, t.a AS a, a.b AS b, b.c AS x;

--#[super-navigation-element]
SELECT x FROM t, t.a[0].b AS x;

--#[super-unpivot]
SELECT k, v FROM t, UNPIVOT t.attrs AS v AT k;

--#[pivot]
SELECT * FROM (SELECT partname, price FROM part) PIVOT (AVG(price) FOR partname IN ('P1', 'P2')) AS p;

--#[pivot-aliases]
SELECT * FROM part PIVOT (COUNT(*) FOR quality IN (1 AS high, 2 AS low));

--#[unpivot-table]
SELECT * FROM (SELECT red, green FROM count_by_color) UNPIVOT (cnt FOR color IN (red, green)) AS u;

--#[unpivot-include-nulls]
SELECT * FROM count_by_color UNPIVOT INCLUDE NULLS (cnt FOR color IN (red, green, blue));

--#[unpivot-exclude-nulls]
SELECT * FROM count_by_color UNPIVOT EXCLUDE NULLS (cnt FOR color IN (red, green));

--#[unpivot-aliases]
SELECT * FROM count_by_color UNPIVOT (cnt FOR color IN (red AS r, green AS g));
