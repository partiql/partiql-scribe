-- The conditional functions: NVL, COALESCE, NULLIF, NVL2, DECODE, GREATEST, and LEAST.

--#[nvl]
SELECT NVL(c, 'none') FROM SIMPLE_T;

--#[nvl-several-arguments]
SELECT NVL(c, z, 'none') FROM T;

--#[coalesce]
SELECT COALESCE(c, 'none') FROM SIMPLE_T;

--#[coalesce-of-aggregate]
SELECT COALESCE(MAX(b), 0), SUM(NVL(b, 0)) FROM SIMPLE_T;

--#[nullif]
SELECT NULLIF(b, 0), NULLIF(c, '') FROM SIMPLE_T;

--#[nvl2]
SELECT NVL2(c, 'set', 'unset'), NVL2(b, b + 1, 0) FROM SIMPLE_T;

--#[decode]
SELECT DECODE(b, 1, 'one', 2, 'two', 'other') FROM SIMPLE_T;

--#[decode-without-default]
SELECT DECODE(b, 1, 'one') FROM SIMPLE_T;

-- A NULL search value matches a NULL expression.
--#[decode-null-search-value]
SELECT DECODE(c, NULL, 'none', 'some') FROM SIMPLE_T;

--#[decode-column-search-value]
SELECT DECODE(t.b, u.b, 'same', 'different') FROM SIMPLE_T AS t, T AS u;

--#[greatest-least]
SELECT GREATEST(b, 1), LEAST(b, 1) FROM SIMPLE_T;

--#[greatest-three-arguments]
SELECT GREATEST(b, b + 1, 10) FROM SIMPLE_T;

--#[greatest-columns]
SELECT GREATEST(t.b, u.b) FROM SIMPLE_T AS t, T AS u;

--#[greatest-one-argument]
SELECT GREATEST(b), LEAST(c) FROM SIMPLE_T;

-- NULL arguments are left out; all NULL is NULL.
--#[greatest-null-arguments]
SELECT GREATEST(b, NULL), GREATEST(NULL, NULL) FROM SIMPLE_T;

--#[greatest-timestamps]
SELECT GREATEST(timestamp_1, timestamp_2) FROM T;

--#[least-string]
SELECT LEAST(c, 'm') FROM SIMPLE_T;

--#[conditional-in-where]
SELECT a FROM SIMPLE_T WHERE NVL(b, 0) > 0 AND NULLIF(c, '') IS NOT NULL;

--#[conditional-nested]
SELECT NVL(NULLIF(c, ''), 'empty'), DECODE(NVL2(c, 1, 0), 1, 'set', 'unset') FROM SIMPLE_T;

--#[conditional-order-by-alias]
SELECT NVL(b, 0) AS n FROM SIMPLE_T ORDER BY n;

--#[conditional-distinct-limit]
SELECT DISTINCT NVL(b, 0) AS n FROM SIMPLE_T ORDER BY n LIMIT 5;

--#[decode-group-by]
SELECT DECODE(b, 1, 'one', 'other') AS k, COUNT(*) FROM SIMPLE_T GROUP BY DECODE(b, 1, 'one', 'other');

-- When a side of a DECODE match is a literal, the both-NULL clause is dead and left out; a signed number is a literal.
--#[decode-signed-search-value]
SELECT DECODE(b, -1, 'neg', 'other') FROM SIMPLE_T;

--#[decode-null-expression]
SELECT DECODE(NULL, b, 'match', 'no') FROM SIMPLE_T;

--#[decode-literal-expression]
SELECT DECODE(1, b, 'match', 'no') FROM SIMPLE_T;

--#[decode-boolean]
SELECT DECODE(a, TRUE, 'yes', FALSE, 'no', 'unknown') FROM SIMPLE_T;

--#[greatest-signed-literal]
SELECT GREATEST(b, -1) FROM SIMPLE_T;

--#[conditional-in-having-and-join]
SELECT t.c, COUNT(*) FROM SIMPLE_T AS t JOIN T AS u ON NVL(t.b, 0) = NVL(u.b, 0) GROUP BY t.c HAVING NVL(MAX(t.b), 0) > 1;

--#[conditional-in-between-and-like]
SELECT a FROM SIMPLE_T WHERE b BETWEEN LEAST(1, 2) AND GREATEST(1, 2) AND NVL(c, '') LIKE 'a%';

--#[conditional-mixed-case-names]
SELECT Nvl(c, 'x'), dEcOdE(b, 1, 'a'), Greatest(b, 1) FROM SIMPLE_T;

-- Redshift's DECODE example compares a date to a string, which it converts; PartiQL does not (a known difference).
--#[decode-date-string-search-value]
SELECT DECODE(timestamp_1, '2008-06-01 00:00:00', 'June 1st, 2008') FROM T;

-- One value left keeps the item's own name, not the column's.
--#[greatest-single-value-name]
SELECT s.b FROM (SELECT GREATEST(b, NULL), a AS b FROM SIMPLE_T) AS s;
