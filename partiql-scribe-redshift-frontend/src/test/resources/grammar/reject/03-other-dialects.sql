-- Syntax that is not Redshift (PostgreSQL, MySQL, SQL Server, Spark forms that users paste) or that Redshift has but the
-- grammar does not cover. Each is a syntax error with a location, so the unsupported form is named precisely.

--#[distinct-on] 1:17
SELECT DISTINCT ON (a) a, b FROM t;

--#[window-clause] 1:20
SELECT SUM(a) OVER w FROM t WINDOW w AS (PARTITION BY b);

--#[fetch-first] 1:28
SELECT a FROM t ORDER BY a FETCH FIRST 10 ROWS ONLY;

--#[for-update] 1:17
SELECT a FROM t FOR UPDATE;

--#[lateral] 1:27
SELECT a FROM t, LATERAL (SELECT b FROM u WHERE u.id = t.id) AS s;

--#[tablesample] 1:29
SELECT a FROM t TABLESAMPLE BERNOULLI (10);

--#[with-ordinality] 1:39
SELECT a FROM t, UNNEST(t.items) WITH ORDINALITY AS i;

--#[values-in-from] 1:29
SELECT * FROM (VALUES (1, 2), (3, 4)) AS v (a, b);

--#[positional-parameter] 1:27
SELECT a FROM t WHERE b = $1;

--#[named-parameter] 1:27
SELECT a FROM t WHERE b = :name;

--#[question-mark-parameter] 1:27
SELECT a FROM t WHERE b = ?;

--#[dollar-quoted-string] 1:8
SELECT $$text$$ FROM t;

--#[escape-string] 1:9
SELECT E'a\nb' FROM t;

--#[bit-string] 1:9
SELECT B'1010' FROM t;

--#[hex-string] 1:9
SELECT X'FF' FROM t;

--#[array-constructor] 1:15
SELECT ARRAY[1, 2, 3] FROM t;

--#[struct-literal] 1:8
SELECT {'a': 1} FROM t;

--#[typed-literal-other] 1:16
SELECT NUMERIC '1.5' FROM t;

--#[limit-percent] 1:26
SELECT a FROM t LIMIT 10 PERCENT;

--#[top-parenthesized] 1:12
SELECT TOP (10) a FROM t;

--#[group-by-with-rollup] 1:38
SELECT a, COUNT(*) FROM t GROUP BY a WITH ROLLUP;

--#[order-by-collate] 1:28
SELECT a FROM t ORDER BY a COLLATE "C";

--#[order-by-using] 1:28
SELECT a FROM t ORDER BY a USING <;

--#[join-apply] 1:23
SELECT a FROM t CROSS APPLY (SELECT b FROM u) AS s;

--#[join-semi] 1:22
SELECT a FROM t LEFT SEMI JOIN u ON t.id = u.id;

--#[natural-cross-join] 1:25
SELECT a FROM t NATURAL CROSS JOIN u;

--#[filter-clause] 1:24
SELECT COUNT(a) FILTER (WHERE b > 0) FROM t;

--#[aggregate-order-by] 1:21
SELECT STRING_AGG(a ORDER BY b) FROM t;

--#[frame-groups] 1:32
SELECT SUM(a) OVER (ORDER BY b GROUPS 1 PRECEDING) FROM t;

--#[frame-exclude] 1:73
SELECT SUM(a) OVER (ORDER BY b ROWS BETWEEN 1 PRECEDING AND CURRENT ROW EXCLUDE CURRENT ROW) FROM t;

--#[frame-expression-bound] 1:37
SELECT SUM(a) OVER (ORDER BY b ROWS n PRECEDING) FROM t;

--#[frame-interval-bound] 1:38
SELECT SUM(a) OVER (ORDER BY d RANGE INTERVAL '1 day' PRECEDING) FROM t;

--#[cast-postgres-array-type] 1:14
SELECT a::INT[] FROM t;

--#[cast-type-with-schema] 1:21
SELECT a::pg_catalog.int4 FROM t;

--#[type-array] 1:23
SELECT CAST(a AS ARRAY<INT>) FROM t;

--#[is-json] 1:28
SELECT a FROM t WHERE b IS JSON;

--#[is-of-type] 1:28
SELECT a FROM t WHERE b IS OF (INT);

--#[between-in-bounds-list] 1:35
SELECT a FROM t WHERE a BETWEEN (1, 2);

--#[like-any] 1:30
SELECT a FROM t WHERE a LIKE ANY ('a%', 'b%');

--#[overlaps] 1:32
SELECT a FROM t WHERE (d1, d2) OVERLAPS (d3, d4);

--#[match-recognize] 1:34
SELECT a FROM t MATCH_RECOGNIZE (PARTITION BY a ORDER BY d PATTERN (x) DEFINE x AS TRUE);

--#[select-into-variable] 1:15
SELECT a INTO :v FROM t;

--#[sql-server-bracket-identifier] 1:8
SELECT [a] FROM t;

--#[mysql-backtick-identifier] 1:8
SELECT `a` FROM `t`;

--#[hash-comment] 1:17
SELECT a FROM t # not a comment in Redshift
;

--#[sql-server-top-ties] 1:15
SELECT TOP 10 WITH TIES a FROM t ORDER BY a;

--#[qualify-before-having] 1:81
SELECT a, COUNT(*) FROM t GROUP BY a QUALIFY ROW_NUMBER() OVER (ORDER BY a) = 1 HAVING COUNT(*) > 1;

--#[clause-out-of-order] 1:28
SELECT a FROM t ORDER BY a WHERE a = 1;

--#[limit-before-order] 1:26
SELECT a FROM t LIMIT 10 ORDER BY a;
