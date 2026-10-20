-- Redshift folds SUPER attribute names to lower case and matches them exactly.
--#[attribute-00]
SELECT t.x.y AS v FROM T t;

--#[attribute-01]
SELECT t.x."Foo" AS v FROM T t;

-- A bare SUPER column.
--#[attribute-02]
SELECT x.y FROM T;

-- Only ASCII letters are folded.
--#[attribute-03]
SELECT t.x."ÄB" AS v FROM T t;

--#[attribute-04]
SELECT t.d.e AS v FROM T t;

-- A path rooted at an expression.
--#[attribute-05]
SELECT (t.x).y AS v1, COALESCE(t.x, t.x).y AS v2 FROM T t;

--#[attribute-06]
SELECT s.y.z AS v FROM (SELECT t.x AS y FROM T t) s;

--#[index-00]
SELECT t.x[0].y AS v1, t."array"[t.b + 1] AS v2 FROM T t;

-- Redshift documents [] for array indexes only.
--#[index-01]
SELECT t.x['a'] AS v FROM T t;
