-- SQL set ops

-- SQL UNION
--#[setop-00]
SELECT a FROM SIMPLE_T AS t1 UNION SELECT a FROM SIMPLE_T AS t2;

--#[setop-01]
SELECT a FROM SIMPLE_T AS t1 UNION ALL SELECT a FROM SIMPLE_T AS t2;

--#[setop-02]
SELECT a FROM SIMPLE_T AS t1 UNION ALL SELECT a FROM SIMPLE_T AS t2 UNION SELECT a FROM SIMPLE_T AS t3;

--#[setop-03]
SELECT a FROM SIMPLE_T AS t1 UNION ALL (SELECT a FROM SIMPLE_T AS t2 UNION SELECT a FROM SIMPLE_T AS t3);

--#[setop-04]
SELECT c, b, a FROM SIMPLE_T AS t1 UNION ALL (SELECT c, b, a FROM SIMPLE_T AS t2 UNION SELECT c, b, a FROM SIMPLE_T AS t3);

--#[setop-05]
SELECT * FROM SIMPLE_T AS t1 UNION ALL SELECT * FROM SIMPLE_T AS t2 UNION SELECT * FROM SIMPLE_T AS t3;

-- Set operations on columns of compatible but distinct types, requiring type coercion

-- No column aliases
--#[setop-06]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 UNION SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

-- Explicit column aliases
--#[setop-07]
SELECT col_int32 AS a, col_timestamp AS b, col_float64 AS c FROM T_ALL_TYPES AS t1 UNION SELECT col_int64 AS a, col_date AS b, col_decimal AS c FROM T_ALL_TYPES AS t2;

--#[setop-08]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 UNION ALL SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

--#[setop-09]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 EXCEPT SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

--#[setop-10]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 EXCEPT ALL SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

--#[setop-11]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 INTERSECT SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

--#[setop-12]
SELECT col_int32, col_timestamp, col_float64 FROM T_ALL_TYPES AS t1 INTERSECT ALL SELECT col_int64, col_date, col_decimal FROM T_ALL_TYPES AS t2;

-- Different kinds of expressions with relaxed type matching
--#[setop-13]
SELECT
    CASE WHEN col_int32 = 0 THEN 0. ELSE 1. / col_int32 END,
    DATE '2026-04-15',
    (col_float64 * 3) - 10
FROM T_ALL_TYPES AS t1

INTERSECT ALL SELECT
    CHAR_LENGTH(col_string),
    col_timestamp,
    (SELECT 123.45 FROM T_ALL_TYPES)
FROM T_ALL_TYPES AS t2;

