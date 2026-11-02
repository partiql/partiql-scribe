-- Constructs with no counterpart in the target: Redshift-only functions, regular
-- expressions, ordered-set aggregates, grouping extensions, recursive and hierarchical queries, intervals, collation.

--#[interval-standalone-qualifier] 1:21
SELECT INTERVAL '1' DAY FROM t;

--#[interval-qualifier-range] 1:31
SELECT ts + INTERVAL '1 2:03' DAY TO MINUTE FROM t;

--#[interval-type] 1:18
SELECT CAST(a AS INTERVAL DAY TO SECOND) FROM t;

--#[collate] 1:25
SELECT a FROM t WHERE a COLLATE case_insensitive = 'x';

--#[row-value-in] 1:25
SELECT a FROM t WHERE (a, b) IN ((1, 2), (3, 4));

--#[row-value-comparison] 1:25
SELECT a FROM t WHERE (a, b) = (1, 2);

--#[at-time-zone] 1:14
SELECT ts AT TIME ZONE 'UTC' FROM t;

--#[quantified-any] 1:27
SELECT a FROM t WHERE a = ANY (SELECT b FROM u);

--#[quantified-some] 1:27
SELECT a FROM t WHERE a > SOME (SELECT b FROM u);

--#[quantified-all] 1:28
SELECT a FROM t WHERE a <> ALL (SELECT b FROM u);

--#[similar-to] 1:25
SELECT a FROM t WHERE a SIMILAR TO '(x|y)%';

--#[posix-regex] 1:25
SELECT a FROM t WHERE a ~ '^x' AND b !~ 'y$';

--#[posix-regex-case-insensitive] 1:25
SELECT a FROM t WHERE a ~* '^x' OR b !~* 'y$';

--#[like-operator] 1:25
SELECT a FROM t WHERE a ~~ 'x%';

--#[with-recursive] 1:16
WITH RECURSIVE c (n) AS (SELECT 1 FROM t UNION ALL SELECT n + 1 FROM c WHERE n < 10) SELECT n FROM c;

--#[oracle-outer-join] 1:38
SELECT a FROM t, u WHERE t.id = u.id (+);

--#[rollup] 1:46
SELECT a, b, COUNT(*) FROM t GROUP BY ROLLUP (a, b);

--#[cube] 1:44
SELECT a, b, COUNT(*) FROM t GROUP BY CUBE (a, b);

--#[grouping-sets] 1:48
SELECT a, b, COUNT(*) FROM t GROUP BY GROUPING SETS ((a), (b), ());

--#[group-by-all] 1:36
SELECT a, COUNT(*) FROM t GROUP BY ALL;

--#[grouping-function] 1:19
SELECT a, GROUPING(a) FROM t GROUP BY a;

--#[connect-by] 1:24
SELECT id FROM t START WITH parent IS NULL CONNECT BY PRIOR id = parent;

--#[within-group-listagg] 1:15
SELECT LISTAGG(a, ',') WITHIN GROUP (ORDER BY a) FROM t;

--#[within-group-percentile] 1:23
SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY a) FROM t;

--#[listagg] 1:15
SELECT LISTAGG(a) FROM t;

--#[median] 1:14
SELECT MEDIAN(a) FROM t;

--#[any-value] 1:17
SELECT ANY_VALUE(a) FROM t;

--#[approximate-count] 1:25
SELECT APPROXIMATE COUNT(DISTINCT a) FROM t;

--#[approximate-percentile] 1:35
SELECT APPROXIMATE PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY a) FROM t;

--#[bit-aggregates] 1:15
SELECT BIT_AND(a), BIT_OR(a) FROM t;

--#[array-functions] 1:8
SELECT ARRAY(1, 2), ARRAY_CONCAT(a, b), GET_ARRAY_LENGTH(a) FROM t;

--#[to-char] 1:15
SELECT TO_CHAR(d, 'YYYY-MM-DD') FROM t;

--#[to-date] 1:15
SELECT TO_DATE(s, 'YYYY-MM-DD') FROM t;

--#[to-number] 1:17
SELECT TO_NUMBER(s, '999.99') FROM t;

--#[to-timestamp] 1:20
SELECT TO_TIMESTAMP(s, 'YYYY-MM-DD HH24:MI:SS') FROM t;

--#[text-to-int-alt] 1:23
SELECT TEXT_TO_INT_ALT(s), TEXT_TO_NUMERIC_ALT(s) FROM t;

--#[try-cast] 1:16
SELECT TRY_CAST(a AS INT) FROM t;

--#[add-months] 1:18
SELECT ADD_MONTHS(d, 1) FROM t;

--#[convert-timezone] 1:24
SELECT CONVERT_TIMEZONE('UTC', 'PST', ts) FROM t;

--#[date-compare] 1:16
SELECT DATE_CMP(d1, d2), TIMESTAMP_CMP(ts1, ts2) FROM t;

--#[date-part-year] 1:22
SELECT DATE_PART_YEAR(d) FROM t;

--#[last-day-next-day] 1:16
SELECT LAST_DAY(d), NEXT_DAY(d, 'Monday') FROM t;

--#[months-between] 1:22
SELECT MONTHS_BETWEEN(d1, d2) FROM t;

--#[trunc] 1:13
SELECT TRUNC(ts), TRUNC(a, 2) FROM t;

--#[timeofday-timezone] 1:17
SELECT TIMEOFDAY(), TIMEZONE('UTC', ts) FROM t;

--#[current-timestamp] 1:8
SELECT CURRENT_TIMESTAMP FROM t;

--#[current-time] 1:8
SELECT CURRENT_TIME FROM t;

--#[localtime] 1:8
SELECT LOCALTIME, LOCALTIMESTAMP FROM t;

--#[hash-functions] 1:11
SELECT MD5(a), SHA1(a), SHA2(a, 256), FNV_HASH(a), CHECKSUM(a) FROM t;

--#[json-parse] 1:18
SELECT JSON_PARSE(s) FROM t;

--#[json-extract-path-text] 1:30
SELECT JSON_EXTRACT_PATH_TEXT(s, 'a', 'b') FROM t;

--#[json-extract-array-element-text] 1:39
SELECT JSON_EXTRACT_ARRAY_ELEMENT_TEXT(s, 0) FROM t;

--#[json-other] 1:21
SELECT IS_VALID_JSON(s), JSON_ARRAY_LENGTH(s), JSON_SERIALIZE(x) FROM t;

--#[trigonometry] 1:11
SELECT SIN(a), COS(a), TAN(a), ACOS(a), ATAN2(a, b) FROM t;

--#[math-other] 1:12
SELECT CBRT(a), LOG(a), PI(), RANDOM(), SIGN(a), DEGREES(a), RADIANS(a) FROM t;

--#[round] 1:13
SELECT ROUND(a, 2) FROM t;

--#[object-functions] 1:14
SELECT OBJECT('a', 1), LOWER_ATTRIBUTE_NAMES(o) FROM t;

--#[object-transform] 1:24
SELECT OBJECT_TRANSFORM(o KEEP 'a' SET 'b', 1) FROM t;

--#[string-functions] 1:13
SELECT ASCII(a), CHR(65), INITCAP(a), LPAD(a, 5), RPAD(a, 5, 'x'), REPEAT(a, 2), REPLACE(a, 'x', 'y'), REVERSE(a), TRANSLATE(a, 'x', 'y') FROM t;

--#[regexp-functions] 1:20
SELECT REGEXP_COUNT(a, 'x'), REGEXP_INSTR(a, 'x'), REGEXP_REPLACE(a, 'x', 'y'), REGEXP_SUBSTR(a, 'x') FROM t;

--#[quote-functions] 1:19
SELECT QUOTE_IDENT(a), QUOTE_LITERAL(a) FROM t;

--#[soundex-difference] 1:15
SELECT SOUNDEX(a), DIFFERENCE(a, b) FROM t;

--#[strtol-crc32] 1:14
SELECT STRTOL('ff', 16), CRC32(a) FROM t;

--#[super-type-information] 1:16
SELECT IS_ARRAY(s), IS_OBJECT(s), IS_INTEGER(s), JSON_TYPEOF(s), DECIMAL_PRECISION(s), SIZE(s) FROM t;

--#[current-user] 1:8
SELECT CURRENT_USER FROM t;

--#[user] 1:8
SELECT USER FROM t;

--#[session-user] 1:8
SELECT SESSION_USER, CURRENT_USER_ID FROM t;

--#[udf-call] 1:16
SELECT f_my_udf(a) FROM t;

--#[udf-call-qualified] 1:25
SELECT myschema.f_my_udf(a, b) FROM t;

--#[unknown-function-no-arguments] 1:19
SELECT my_function() FROM t;
