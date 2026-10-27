--#[row-number]
SELECT c, ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) AS rn FROM T;

-- Redshift RANK returns INTEGER, PartiQL RANK returns BIGINT; the translation adds a note.
--#[rank]
SELECT b, RANK() OVER (ORDER BY b DESC) AS r FROM T;

--#[rank-without-order-by]
SELECT b, RANK() OVER () AS r FROM T;

--#[dense-rank]
SELECT b, DENSE_RANK() OVER (PARTITION BY c ORDER BY b) AS r FROM T;

--#[lag-lead]
SELECT b, LAG(b) OVER (ORDER BY timestamp_1) AS prev, LEAD(b, 2) OVER (PARTITION BY c ORDER BY timestamp_1 DESC) AS nxt FROM T;

--#[lag-offset-zero-respect-nulls]
SELECT b, LAG(b, 0) RESPECT NULLS OVER (ORDER BY b) AS cur FROM T;

--#[lag-offset-expression]
SELECT LAG(b, b) OVER (ORDER BY b) AS v FROM T;

--#[lag-default]
SELECT LAG(b, 1, 0) OVER (ORDER BY b) AS v FROM T;

--#[lag-without-order-by]
SELECT LAG(b) OVER (PARTITION BY c) AS v FROM T;

--#[ignore-nulls]
SELECT b, LAG(b) IGNORE NULLS OVER (ORDER BY timestamp_1) AS prev FROM T;

--#[partition-by-super-path]
SELECT ROW_NUMBER() OVER (PARTITION BY d.e ORDER BY b) AS r FROM T;

--#[partition-by-columns]
SELECT b, LEAD(b) OVER (PARTITION BY c, z ORDER BY timestamp_1, b DESC) AS nxt FROM T;

--#[partition-by-expression]
SELECT ROW_NUMBER() OVER (PARTITION BY UPPER(c) ORDER BY b) AS r FROM T;

--#[order-by-nulls]
SELECT b, ROW_NUMBER() OVER (ORDER BY b DESC NULLS LAST) AS r1, RANK() OVER (ORDER BY c) AS r2 FROM T;

--#[order-by-constant]
SELECT ROW_NUMBER() OVER (ORDER BY 1) AS r FROM T;

-- A running COUNT(*) is ROW_NUMBER.
--#[running-count]
SELECT c, COUNT(*) OVER (PARTITION BY c ORDER BY b ROWS UNBOUNDED PRECEDING) AS running_n FROM T;

--#[running-sum-of-one]
SELECT c, SUM(1) OVER (PARTITION BY c ORDER BY b ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_n FROM T;

--#[running-sum]
SELECT b, SUM(b) OVER (ORDER BY b ROWS UNBOUNDED PRECEDING) AS running FROM T;

--#[aggregate-window]
SELECT c, b, SUM(b) OVER (PARTITION BY c) AS total FROM T;

--#[count-star-window]
SELECT b, COUNT(*) OVER () AS n FROM T;

--#[percent-rank]
SELECT b, PERCENT_RANK() OVER (ORDER BY b) AS p FROM T;

--#[ntile]
SELECT b, NTILE(4) OVER (ORDER BY b) AS q FROM T;

--#[first-value]
SELECT b, FIRST_VALUE(b) OVER (PARTITION BY c ORDER BY b ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS f FROM T;

--#[ratio-to-report]
SELECT b, RATIO_TO_REPORT(b) OVER (PARTITION BY c) AS r FROM T;

--#[median-window]
SELECT b, MEDIAN(b) OVER (PARTITION BY c) AS m FROM T;

-- A window function in ORDER BY: the query is a subquery whose window function is a column.
--#[order-by-window-function]
SELECT b FROM T ORDER BY ROW_NUMBER() OVER (ORDER BY b);

--#[order-by-window-alias]
SELECT b, ROW_NUMBER() OVER (ORDER BY b) AS rn FROM T ORDER BY rn DESC;

--#[order-by-window-alias-limit]
SELECT b, ROW_NUMBER() OVER (ORDER BY b) AS rn FROM T ORDER BY rn DESC LIMIT 2;

--#[order-by-window-alias-expression]
SELECT b, ROW_NUMBER() OVER (ORDER BY b) AS rn FROM T ORDER BY rn + 1;

--#[window-in-expression]
SELECT b, ROW_NUMBER() OVER (ORDER BY b) - 1 AS r0, CASE WHEN ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) = 1 THEN 'first' ELSE 'other' END AS f FROM T;

--#[window-over-grouped-query]
SELECT c, COUNT(*) AS n, RANK() OVER (ORDER BY COUNT(*) DESC) AS r FROM T GROUP BY c;

--#[window-over-group-key]
SELECT c, RANK() OVER (ORDER BY c) AS r FROM T GROUP BY c;

--#[window-lateral-alias]
SELECT ROW_NUMBER() OVER (ORDER BY b) AS rn, rn + 1 AS next FROM T;

--#[window-in-where]
SELECT b FROM T WHERE ROW_NUMBER() OVER (ORDER BY b) = 1;

-- A named window is not Redshift syntax.
--#[named-window]
SELECT b, ROW_NUMBER() OVER w AS r FROM T WINDOW w AS (ORDER BY b);
