-- Constructs the target view engine does not support yet. The grammar has no rule for them, so they are syntax
-- errors; they can be added when the target supports them.

--#[select-without-from] 1:9
SELECT 1;

--#[select-without-from-expression] 1:30
SELECT 1 + 1 AS two, 'a' AS s;

--#[select-without-from-in-subquery] 1:36
SELECT a FROM t WHERE a = (SELECT 1);

--#[exists] 1:30
SELECT a FROM t WHERE EXISTS (SELECT 1 FROM u WHERE u.id = t.id);

--#[not-exists] 1:34
SELECT a FROM t WHERE NOT EXISTS (SELECT b FROM u WHERE u.id = t.id);

--#[intersect] 1:17
SELECT a FROM t INTERSECT SELECT a FROM u;

--#[except] 1:17
SELECT a FROM t EXCEPT SELECT a FROM u;

--#[minus] 1:17
SELECT a FROM t MINUS SELECT a FROM u;

--#[join-using] 1:24
SELECT a FROM t JOIN u USING (id);

--#[natural-join] 1:17
SELECT a FROM t NATURAL JOIN u;

--#[pivot] 1:36
SELECT * FROM (SELECT p, q FROM t) PIVOT (SUM(q) FOR p IN ('a', 'b'));

--#[unpivot] 1:17
SELECT * FROM t UNPIVOT (v FOR k IN (a, b));

--#[unnest] 1:18
SELECT x FROM t, UNNEST(t.arr) AS x;

--#[qualify] 1:25
SELECT a FROM t QUALIFY ROW_NUMBER() OVER (PARTITION BY b ORDER BY c) = 1;

--#[window-aggregate] 1:20
SELECT SUM(a) OVER (PARTITION BY b) FROM t;

--#[window-row-number] 1:18
SELECT ROW_NUMBER() OVER (ORDER BY a) FROM t;

--#[window-rank] 1:12
SELECT RANK() OVER (PARTITION BY b ORDER BY a DESC) FROM t;

--#[window-lag] 1:11
SELECT LAG(a, 1, 0) OVER (ORDER BY d) FROM t;

--#[window-lead] 1:12
SELECT LEAD(a) OVER (ORDER BY d) FROM t;

--#[window-frame] 1:20
SELECT SUM(a) OVER (ORDER BY d ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) FROM t;

--#[window-ignore-nulls] 1:19
SELECT FIRST_VALUE(a) IGNORE NULLS OVER (ORDER BY d) FROM t;

--#[super-navigation-field] 1:14
SELECT s.a.b.c.d FROM t;

--#[super-navigation-element] 1:9
SELECT s[0] FROM t;

--#[super-navigation-element-field] 1:15
SELECT s.items[0].name FROM t;

--#[super-navigation-quoted-field] 1:48
SELECT s."Key" FROM t WHERE s."Key" = 1 AND s.x[1] > 0;

--#[super-unnest-from-index] 1:40
SELECT o, i FROM t AS c, c.orders AS o AT i;

--#[super-object-unpivot] 1:26
SELECT k, v FROM t AS c, UNPIVOT c.obj AS v AT k;

--#[super-type] 1:18
SELECT CAST(a AS SUPER), JSON_PARSE(s) FROM t;

--#[array-contains] 1:37
SELECT a FROM t WHERE ARRAY_CONTAINS(arr, 1);

--#[stddev] 1:19
SELECT STDDEV_SAMP(a), STDDEV_POP(a) FROM t;

--#[variance] 1:16
SELECT VAR_SAMP(a), VAR_POP(a) FROM t;

--#[bool-and-or] 1:16
SELECT BOOL_AND(a), BOOL_OR(b) FROM t;

--#[date-part] 1:17
SELECT DATE_PART(year, d) FROM t;

--#[date-trunc] 1:18
SELECT DATE_TRUNC('month', d) FROM t;

--#[extract-non-epoch] 1:16
SELECT EXTRACT(YEAR FROM d) FROM t;

--#[extract-month] 1:16
SELECT EXTRACT(month FROM d) FROM t;

--#[abs] 1:11
SELECT ABS(a) FROM t;

--#[ceiling-floor] 1:15
SELECT CEILING(a), CEIL(a), FLOOR(a) FROM t;

--#[exp-ln] 1:11
SELECT EXP(a), LN(a) FROM t;

--#[mod] 1:11
SELECT MOD(a, 2) FROM t;

--#[sqrt] 1:12
SELECT SQRT(a) FROM t;

--#[trim] 1:12
SELECT TRIM(a) FROM t;

--#[trim-both] 1:12
SELECT TRIM(BOTH 'x' FROM a) FROM t;

--#[btrim-ltrim-rtrim] 1:13
SELECT BTRIM(a), LTRIM(a), RTRIM(a, 'x') FROM t;

--#[length] 1:11
SELECT LEN(a), LENGTH(a), CHAR_LENGTH(a), CHARACTER_LENGTH(a), TEXTLEN(a), OCTET_LENGTH(a) FROM t;

--#[position] 1:16
SELECT POSITION('x' IN a) FROM t;

--#[strpos-charindex] 1:14
SELECT STRPOS(a, 'x'), CHARINDEX('x', a) FROM t;

--#[left-right] 1:8
SELECT LEFT(a, 2), RIGHT(a, 2) FROM t;

--#[substring-from] 1:17
SELECT SUBSTRING(a FROM 1 FOR 2) FROM t;

--#[substring-comma] 1:17
SELECT SUBSTRING(a, 1, 2) FROM t;

--#[split-part] 1:18
SELECT SPLIT_PART(a, ',', 1) FROM t;
