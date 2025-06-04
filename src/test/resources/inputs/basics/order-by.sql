--#[order-by-00]
SELECT a FROM T ORDER BY T.a;

--#[order-by-01]
SELECT a FROM T ORDER BY T.a, T.b;

--#[order-by-02]
SELECT a FROM T ORDER BY a, b;

--#[order-by-03]
SELECT T.flds FROM EXCLUDE_T AS T ORDER BY T.flds.c.field_x;

--#[order-by-04]
SELECT a FROM T ORDER BY T.a ASC, T.b DESC;

--#[order-by-05]
SELECT a FROM T ORDER BY T.a ASC NULLS FIRST, T.b DESC NULLS LAST;

--#[order-by-06]
SELECT a FROM T ORDER BY T.a ASC NULLS LAST, T.b DESC NULLS FIRST;

--#[order-by-07]
(SELECT a FROM T ORDER BY T.a, T.b) UNION (SELECT a FROM T ORDER BY T.a, T.b);

--#[order-by-08]
(SELECT a, b FROM T ORDER BY T.a, T.b) UNION (SELECT a, b FROM T ORDER BY T.a, T.b) ORDER BY a;

--#[order-by-09]
(SELECT a, b FROM T ORDER BY T.a, T.b LIMIT 1 OFFSET 2) UNION (SELECT a, b FROM T ORDER BY T.a, T.b LIMIT 3 OFFSET 4) ORDER BY a LIMIT 5 OFFSET 6;

--#[order-by-10]
(SELECT T.flds FROM EXCLUDE_T AS T) UNION (SELECT T.flds FROM EXCLUDE_T AS T) ORDER BY flds.c.field_x;
