-- Function calls: scalar, aggregate, window, ordered-set, the functions with their own syntax, CASE, and the niladic
-- functions. The grammar accepts any function name; which functions translate is decided later.
-- https://docs.aws.amazon.com/redshift/latest/dg/c_SQL_functions.html

--#[scalar]
SELECT UPPER(a), LOWER(a), LENGTH(a), ABS(n), ROUND(n, 2), COALESCE(a, b, 'x'), NVL(a, b), NULLIF(a, b), GREATEST(a, b, c) FROM t;

--#[scalar-no-arguments]
SELECT GETDATE(), RANDOM(), PI(), CURRENT_SCHEMA() FROM t;

--#[scalar-nested]
SELECT ROUND(ABS(a - b) / NULLIF(c, 0), 2), UPPER(TRIM(COALESCE(a, ''))) FROM t;

--#[scalar-qualified-arguments]
SELECT DATE_TRUNC('month', t.d), SPLIT_PART(t.s, ',', 2), REGEXP_REPLACE(t.s, '[0-9]+', '') FROM t;

--#[left-right]
SELECT LEFT(a, 3), RIGHT(a, 3) FROM t;

--#[aggregates]
SELECT COUNT(a), SUM(n), AVG(n), MIN(n), MAX(n), STDDEV(n), VARIANCE(n), MEDIAN(n), BOOL_AND(b), BOOL_OR(b), ANY_VALUE(a) FROM t;

--#[count-star]
SELECT COUNT(*) FROM t;

--#[aggregates-distinct]
SELECT COUNT(DISTINCT a), SUM(DISTINCT n), AVG(DISTINCT n), COUNT(ALL a) FROM t;

--#[aggregates-expression]
SELECT SUM(n * 2), COUNT(CASE WHEN a > 0 THEN 1 END), MAX(LENGTH(s)), SUM(n) / COUNT(*) FROM t;

--#[approximate]
SELECT APPROXIMATE COUNT(DISTINCT a), APPROXIMATE PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY n) FROM t;

--#[listagg]
SELECT LISTAGG(a, ', '), LISTAGG(DISTINCT a, ','), LISTAGG(a) WITHIN GROUP (ORDER BY a) FROM t;

--#[ordered-set]
SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY n), PERCENTILE_DISC(0.9) WITHIN GROUP (ORDER BY n DESC) FROM t;

--#[window-ranking]
SELECT ROW_NUMBER() OVER (ORDER BY a), RANK() OVER (PARTITION BY b ORDER BY a DESC), DENSE_RANK() OVER (PARTITION BY b, c ORDER BY a, d), NTILE(4) OVER (ORDER BY a) FROM t;

--#[window-empty]
SELECT ROW_NUMBER() OVER (), SUM(n) OVER () FROM t;

--#[window-partition-only]
SELECT SUM(n) OVER (PARTITION BY a), COUNT(*) OVER (PARTITION BY a, b) FROM t;

--#[window-aggregates]
SELECT SUM(n) OVER (PARTITION BY a ORDER BY d), AVG(n) OVER (ORDER BY d), COUNT(*) OVER (PARTITION BY a) FROM t;

--#[window-order-nulls]
SELECT ROW_NUMBER() OVER (ORDER BY a NULLS FIRST, b DESC NULLS LAST) FROM t;

--#[window-frame-rows]
SELECT SUM(n) OVER (ORDER BY d ROWS UNBOUNDED PRECEDING), SUM(n) OVER (ORDER BY d ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), SUM(n) OVER (ORDER BY d ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING) FROM t;

--#[window-frame-range]
SELECT SUM(n) OVER (ORDER BY d RANGE UNBOUNDED PRECEDING), SUM(n) OVER (ORDER BY d RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING), SUM(n) OVER (PARTITION BY a ORDER BY d RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING) FROM t;

--#[window-frame-current-row]
SELECT SUM(n) OVER (ORDER BY d ROWS CURRENT ROW), FIRST_VALUE(a) OVER (ORDER BY d ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM t;

--#[window-value-functions]
SELECT LAG(a) OVER (ORDER BY d), LAG(a, 2) OVER (ORDER BY d), LEAD(a, 1, 0) OVER (PARTITION BY b ORDER BY d), FIRST_VALUE(a) OVER (ORDER BY d), LAST_VALUE(a) OVER (ORDER BY d), NTH_VALUE(a, 2) OVER (ORDER BY d) FROM t;

--#[window-null-treatment]
SELECT FIRST_VALUE(a) IGNORE NULLS OVER (ORDER BY d), LAST_VALUE(a) IGNORE NULLS OVER (ORDER BY d), LAG(a) RESPECT NULLS OVER (ORDER BY d) FROM t;

--#[window-ratio-and-distribution]
SELECT PERCENT_RANK() OVER (ORDER BY n), CUME_DIST() OVER (ORDER BY n), RATIO_TO_REPORT(n) OVER (PARTITION BY a) FROM t;

--#[window-listagg]
SELECT LISTAGG(a, ',') WITHIN GROUP (ORDER BY a) OVER (PARTITION BY b) FROM t;

--#[dateadd-datediff]
SELECT DATEADD(day, 1, d), DATEADD(month, -3, d), DATEDIFF(day, d1, d2), DATEDIFF(year, d, CURRENT_DATE) FROM t;

--#[dateadd-datediff-string-part]
SELECT DATEADD('day', 1, d), DATEDIFF('hour', ts1, ts2), DATEADD('month', n, d) FROM t;

--#[dateadd-datediff-abbreviations]
SELECT DATEADD(d, 1, d), DATEADD(mon, 1, d), DATEADD(yr, 1, d), DATEDIFF(qtr, d1, d2), DATEDIFF(w, d1, d2), DATEDIFF(ms, ts1, ts2) FROM t;

--#[extract]
SELECT EXTRACT(YEAR FROM d), EXTRACT(month FROM d), EXTRACT(EPOCH FROM ts), EXTRACT('dow' FROM d), EXTRACT(hour FROM ts AT TIME ZONE 'UTC') FROM t;

--#[position]
SELECT POSITION('a' IN s), POSITION(sub IN s), POSITION('x' IN LOWER(s)) FROM t;

--#[substring-from-for]
SELECT SUBSTRING(s FROM 2), SUBSTRING(s FROM 2 FOR 3), SUBSTRING(s FOR 3), SUBSTRING(s FROM n + 1 FOR LENGTH(s) - n) FROM t;

--#[substring-arguments]
SELECT SUBSTRING(s, 2), SUBSTRING(s, 2, 3), SUBSTR(s, 1, 2) FROM t;

--#[trim]
SELECT TRIM(s), TRIM(' ' FROM s), TRIM(BOTH FROM s), TRIM(BOTH 'x' FROM s), TRIM(LEADING 'x' FROM s), TRIM(TRAILING FROM s), TRIM(LEADING FROM s) FROM t;

--#[trim-arguments]
SELECT TRIM(s, 'x'), LTRIM(s), RTRIM(s, ' '), BTRIM(s) FROM t;

--#[case-searched]
SELECT CASE WHEN a > 0 THEN 'positive' WHEN a < 0 THEN 'negative' ELSE 'zero' END FROM t;

--#[case-searched-no-else]
SELECT CASE WHEN a THEN 1 END FROM t;

--#[case-simple]
SELECT CASE a WHEN 1 THEN 'one' WHEN 2 THEN 'two' ELSE 'other' END FROM t;

--#[case-nested]
SELECT CASE WHEN a THEN CASE WHEN b THEN 1 ELSE 2 END ELSE CASE c WHEN 'x' THEN 3 END END FROM t;

--#[case-expressions]
SELECT CASE WHEN a IN (1, 2) AND b IS NOT NULL THEN SUM(n) * 2 ELSE (SELECT MAX(n) FROM u) END FROM t GROUP BY a, b;

--#[niladic-datetime]
SELECT SYSDATE, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIME, LOCALTIMESTAMP, LOCALTIME;

--#[niladic-user]
SELECT CURRENT_USER, CURRENT_USER_ID, SESSION_USER, USER;

--#[niladic-in-expression]
SELECT * FROM t WHERE d > CURRENT_DATE - 7 AND ts < SYSDATE AND created_by = CURRENT_USER;

--#[scalar-subquery]
SELECT (SELECT MAX(n) FROM u), (SELECT COUNT(*) FROM u WHERE u.id = t.id) AS c FROM t;

--#[object-transform]
SELECT OBJECT_TRANSFORM(s KEEP '"a"', '"b"' SET '"c"', 1) FROM t;

--#[super-functions]
SELECT JSON_PARSE(s), JSON_SERIALIZE(j), IS_ARRAY(j), IS_OBJECT(j), GET_ARRAY_LENGTH(j), ARRAY(1, 2, 3), ARRAY_CONCAT(a1, a2) FROM t;

--#[json-functions]
SELECT JSON_EXTRACT_PATH_TEXT(s, 'a', 'b'), JSON_EXTRACT_ARRAY_ELEMENT_TEXT(s, 0), IS_VALID_JSON(s) FROM t;

--#[conversion-functions]
SELECT TO_CHAR(d, 'YYYY-MM-DD'), TO_DATE(s, 'YYYY-MM-DD'), TO_TIMESTAMP(s, 'YYYY-MM-DD HH24:MI:SS'), TO_NUMBER(s, '999.99'), CONVERT_TIMEZONE('UTC', 'PST', ts) FROM t;

--#[function-in-every-clause]
SELECT UPPER(a) FROM t WHERE LENGTH(a) > 1 GROUP BY UPPER(a) HAVING COUNT(*) > 1 ORDER BY LOWER(a) LIMIT 1;
