--#[review-cte-column-00]
WITH x AS (SELECT t.b * 2 AS n FROM T AS t) SELECT LEN(x.n) AS l FROM x;

--#[review-cte-column-01]
SELECT LEFT(s.d, 7) AS ym FROM (SELECT TRUNC(t.timestamp_1) AS d FROM T AS t) AS s;

--#[review-cte-column-02]
SELECT CONCAT(s.y, '-Q') AS l FROM (SELECT EXTRACT(YEAR FROM t.timestamp_1) AS y FROM T AS t) AS s;

--#[review-array-subscript-00]
SELECT ARRAY(t.col_list[5], t.col_list[0]) AS a FROM T_ALL_TYPES AS t;

--#[review-array-subscript-01]
SELECT t.col_list[5] + 1 AS a FROM T_ALL_TYPES AS t;

--#[review-real-aggregate-00]
SELECT COALESCE(MAX(t.col_float32), 0) * 3 AS x FROM T_ALL_TYPES AS t;

--#[review-real-aggregate-01]
SELECT COALESCE(MAX(t.col_float32), 0) = 16777217 AS x FROM T_ALL_TYPES AS t;
