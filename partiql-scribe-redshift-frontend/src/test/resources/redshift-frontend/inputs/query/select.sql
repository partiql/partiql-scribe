-- The select list, DISTINCT, TOP, and EXCLUDE. Tables: T(a bool, b int, c string, ...), SIMPLE_T(a bool, b int, c string).

--#[select-columns]
SELECT a, b, c FROM T;

--#[select-star]
SELECT * FROM SIMPLE_T;

--#[select-qualified-star]
SELECT t.* FROM SIMPLE_T AS t;

--#[select-alias-as]
SELECT a AS x, b AS "Y" FROM SIMPLE_T;

--#[select-alias-bare]
SELECT a x, b y FROM SIMPLE_T;

--#[select-alias-reserved-after-as]
SELECT a AS from, b AS select FROM SIMPLE_T;

--#[select-qualified-columns]
SELECT t.a, t.b FROM SIMPLE_T AS t;

--#[select-table-qualified-columns]
SELECT SIMPLE_T.a, SIMPLE_T.b FROM SIMPLE_T;

--#[select-case-folding]
SELECT A, Simple_T.B, "C" FROM Simple_T;

--#[select-distinct]
SELECT DISTINCT a FROM SIMPLE_T;

--#[select-all]
SELECT ALL a FROM SIMPLE_T;

--#[select-top]
SELECT TOP 5 a FROM SIMPLE_T ORDER BY a;

--#[select-exclude-single-table]
SELECT * EXCLUDE a FROM SIMPLE_T;

--#[select-exclude-list]
SELECT * EXCLUDE (a, b) FROM SIMPLE_T;

--#[select-exclude-qualified]
SELECT * EXCLUDE (t.a) FROM SIMPLE_T AS t, T AS u;

--#[select-exclude-qualified-star]
SELECT t.* EXCLUDE (a) FROM SIMPLE_T AS t, T AS u;

--#[select-exclude-ambiguous]
SELECT * EXCLUDE a FROM SIMPLE_T, T;

--#[select-exclude-with-items]
SELECT a, b EXCLUDE a FROM SIMPLE_T;

--#[select-star-and-items]
SELECT *, a FROM SIMPLE_T;

-- An EXCLUDE qualifier must be a FROM item, and the relation behind a `t.*`.
--#[select-exclude-unknown-qualifier]
SELECT * EXCLUDE t.c FROM SIMPLE_T AS x;

--#[select-exclude-other-relation-than-star]
SELECT x.* EXCLUDE y.c FROM SIMPLE_T AS x JOIN T AS y ON x.b = y.b;

--#[select-exclude-own-relation-of-star]
SELECT x.* EXCLUDE x.c FROM SIMPLE_T AS x JOIN T AS y ON x.b = y.b;

--#[select-exclude-three-part-name]
SELECT * EXCLUDE default.SIMPLE_T.c FROM default.SIMPLE_T;
