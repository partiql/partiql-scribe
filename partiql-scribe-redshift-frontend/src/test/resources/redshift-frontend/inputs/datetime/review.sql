--#[review-aggregate-00]
SELECT MIN(t.col_date) + 1 AS v FROM T_ALL_TYPES AS t;

--#[review-aggregate-01]
SELECT DATEADD(day, 1, MAX(t.col_date)) AS v FROM T_ALL_TYPES AS t;

--#[review-subquery-00]
SELECT DATEDIFF(day, (SELECT MAX(t2.col_date) FROM T_ALL_TYPES AS t2), DATE '2020-01-01') AS v FROM T_ALL_TYPES AS t;

--#[review-subquery-01]
SELECT EXTRACT(year FROM (SELECT MAX(t2.col_date) FROM T_ALL_TYPES AS t2)) AS v FROM T_ALL_TYPES AS t;

--#[review-interval-times-00]
SELECT t.col_timestamp + t.col_d2s * 2 AS v FROM T_INTERVALS AS t;

--#[review-interval-times-01]
SELECT t.col_d2s * 2.5 AS v FROM T_INTERVALS AS t;

--#[review-interval-times-02]
SELECT t.col_d2s / 2 AS v FROM T_INTERVALS AS t;

--#[review-interval-negate-00]
SELECT t.col_timestamp + -INTERVAL '1' DAY AS v FROM T_INTERVALS AS t;

--#[review-interval-negate-01]
SELECT t.col_timestamp + -t.col_d2s AS v FROM T_INTERVALS AS t;

--#[review-interval-negate-02]
SELECT t.col_timestamp + +t.col_d2s AS v FROM T_INTERVALS AS t;

--#[review-interval-nested-00]
SELECT t.col_date + INTERVAL '1 month' * 2 AS v FROM T_ALL_TYPES AS t;

--#[review-interval-nested-01]
SELECT t.col_date + (INTERVAL '1 month' + INTERVAL '1 month') AS v FROM T_ALL_TYPES AS t;

--#[review-timetz-case-00]
SELECT CASE t.col_timez WHEN TIMETZ '12:30:45+08' THEN 1 ELSE 0 END AS v FROM T_ALL_TYPES AS t;

--#[review-timetz-nullif-00]
SELECT NULLIF(t.col_timez, TIMETZ '12:30:45+08') AS v FROM T_ALL_TYPES AS t;

--#[review-order-by-00]
SELECT t.col_date AS v FROM T_ALL_TYPES AS t ORDER BY t.col_date + 1;
