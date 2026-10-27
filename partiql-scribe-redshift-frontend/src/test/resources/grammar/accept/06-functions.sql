-- Functions: the aggregates, conditional, formatting, date and time, math, and string functions with a counterpart
-- in the target. https://docs.aws.amazon.com/redshift/latest/dg/c_SQL_functions.html

--#[avg]
SELECT AVG(a), AVG(DISTINCT a), AVG(ALL a), AVG(a + b) FROM t;

--#[count]
SELECT COUNT(a), COUNT(DISTINCT a), COUNT(ALL a) FROM t;

--#[count-star]
SELECT COUNT(*) FROM t;

--#[max-min]
SELECT MAX(a), MIN(a), MAX(DISTINCT a), MIN(d) FROM t;

--#[sum]
SELECT SUM(a), SUM(DISTINCT a), SUM(a * b) FROM t;

--#[aggregates-in-expressions]
SELECT SUM(a) / COUNT(*), MAX(a) - MIN(a), CAST(AVG(a) AS INT) FROM t;

--#[aggregates-in-having-order-by]
SELECT a FROM t GROUP BY a HAVING SUM(b) > 0 ORDER BY COUNT(*) DESC;

--#[aggregates-case]
SELECT Count(a), sum(b), Max(c) FROM t;

--#[decode]
SELECT DECODE(a, 1, 'one', 2, 'two', 'other'), DECODE(a, NULL, 'none', 'some') FROM t;

--#[greatest-least]
SELECT GREATEST(a, b, c), LEAST(a, 0), GREATEST(a, b) FROM t;

--#[nvl-coalesce]
SELECT NVL(a, 0), NVL(a, b, c), COALESCE(a, b), COALESCE(a, b, 'none') FROM t;

--#[nvl2]
SELECT NVL2(a, 'set', 'unset'), NVL2(a, b + 1, 0) FROM t;

--#[nullif]
SELECT NULLIF(a, 0), NULLIF(s, '') FROM t;

--#[cast]
SELECT CAST(a AS VARCHAR(10)), CAST(s AS DATE) FROM t;

--#[convert]
SELECT CONVERT(INT, a), CONVERT(VARCHAR(10), d) FROM t;

--#[current-date]
SELECT CURRENT_DATE, CURRENT_DATE - 1 FROM t WHERE d < CURRENT_DATE;

--#[dateadd]
SELECT DATEADD(day, 1, d), DATEADD(month, -3, d), DATEADD(year, n, ts) FROM t;

--#[dateadd-parts]
SELECT DATEADD(week, 1, d), DATEADD(quarter, 1, d), DATEADD(hour, 1, ts), DATEADD(minute, 1, ts), DATEADD(second, 1, ts) FROM t;

--#[dateadd-quoted-part]
SELECT DATEADD('day', 1, d), DATEADD('month', 1, d) FROM t;

--#[dateadd-abbreviated-part]
SELECT DATEADD(d, 1, d1), DATEADD(mon, 1, d1), DATEADD(yr, 1, d1), DATEADD(h, 1, ts), DATEADD(min, 1, ts) FROM t;

--#[datediff]
SELECT DATEDIFF(day, d1, d2), DATEDIFF(month, d, CURRENT_DATE), DATEDIFF(year, DATE '2000-01-01', d) FROM t;

--#[datediff-quoted-part]
SELECT DATEDIFF('day', d1, d2) FROM t;

--#[datediff-nested]
SELECT DATEDIFF(day, DATEADD(day, -7, d), d) FROM t;

--#[getdate]
SELECT GETDATE(), GETDATE() - INTERVAL '1 day' FROM t WHERE ts < GETDATE();

--#[sysdate]
SELECT SYSDATE, SYSDATE - 1 FROM t WHERE ts < SYSDATE;

--#[extract-epoch]
SELECT EXTRACT(EPOCH FROM ts), EXTRACT(epoch FROM d) FROM t;

--#[power]
SELECT POWER(a, 2), POWER(2, a), POWER(a, b + 1) FROM t;

--#[concat]
SELECT CONCAT(a, b), CONCAT(a, ' '), CONCAT(CONCAT(a, b), c) FROM t;

--#[lower-upper]
SELECT LOWER(a), UPPER(a), UPPER(LOWER(a)), LOWER('ABC') FROM t;

--#[functions-nested]
SELECT NVL(UPPER(a), LOWER(b)), COALESCE(MAX(a), 0), SUM(NVL(a, 0)), CAST(POWER(a, 2) AS INT) FROM t;

--#[functions-in-conditions]
SELECT a FROM t WHERE UPPER(a) = 'X' AND DATEDIFF(day, d, CURRENT_DATE) < 30 AND NVL(b, 0) > 0;

--#[functions-in-group-by]
SELECT UPPER(a), COUNT(*) FROM t GROUP BY UPPER(a);
