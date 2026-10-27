-- The select list: items, aliases, stars, quantifiers, TOP, EXCLUDE, and INTO.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_list.html

--#[select-items]
SELECT a, b, c FROM t;

--#[select-alias-as]
SELECT a AS x, b AS "Quoted Alias" FROM t;

--#[select-alias-bare]
SELECT a x, t.b y FROM t;

--#[select-star]
SELECT * FROM t;

--#[select-qualified-star]
SELECT t.*, u.* FROM t, u;

--#[select-star-and-items]
SELECT *, a + 1 AS a1 FROM t;

--#[select-distinct]
SELECT DISTINCT a, b FROM t;

--#[select-all]
SELECT ALL a FROM t;

--#[select-top]
SELECT TOP 10 a FROM t ORDER BY a;

--#[select-top-distinct]
SELECT TOP 5 DISTINCT a FROM t;

--#[select-no-from]
SELECT 1, 'a', TRUE;

--#[select-expressions]
SELECT a + b, (a - b) * 2, CASE WHEN a > 0 THEN 'p' ELSE 'n' END, COUNT(*) FROM t GROUP BY 1, 2, 3;

--#[select-exclude]
SELECT * EXCLUDE a FROM t;

--#[select-exclude-list]
SELECT * EXCLUDE (a, b) FROM t;

--#[select-exclude-qualified-star]
SELECT t.* EXCLUDE a FROM t, u;

--#[select-into]
SELECT a, b INTO new_table FROM t;

--#[select-into-temp]
SELECT a INTO TEMP TABLE #tmp FROM t;

--#[select-keyword-columns]
SELECT date, time, year, month, day, count, position, "user" FROM t;
