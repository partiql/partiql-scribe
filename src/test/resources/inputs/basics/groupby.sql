--#[groupby-00]
SELECT T.a FROM T GROUP BY T.a as a_alias;

--#[groupby-01]
SELECT T.a, MAX(b) AS agg1 FROM T GROUP BY T.a as a_alias;

--#[groupby-02]
SELECT T.a, MAX(b) AS agg1, COUNT(c) AS agg2 FROM T GROUP BY T.a as a_alias;

--#[groupby-03]
SELECT T.a, T.b, MAX(b) AS agg1, COUNT(c) AS agg2 FROM T GROUP BY T.a as a_alias, T.b AS b_alias;

--#[groupby-04]
SELECT MAX(b) AS agg1, COUNT(c) AS agg2, T.a, T.b  FROM T GROUP BY T.a as a_alias, T.b AS b_alias;

--#[groupby-05]
SELECT MAX(b) AS agg1, COUNT(c) AS agg2, T.b, T.a FROM T GROUP BY T.a as a_alias, T.b AS b_alias;
