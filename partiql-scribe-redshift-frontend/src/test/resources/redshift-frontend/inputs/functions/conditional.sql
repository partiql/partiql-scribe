-- PLK 1.6.1 types COALESCE with the common type of the arguments but does not convert them, so the translation casts
-- an argument of another numeric or datetime type to the common type.
--#[cond-coalesce-00]
SELECT NVL(col_decimal, 0) + 1 AS v FROM T_ALL_TYPES;

--#[cond-coalesce-01]
SELECT SUM(NVL(col_decimal, 0)) AS v FROM T_ALL_TYPES;

--#[cond-coalesce-02]
SELECT COALESCE(col_int64, 0) AS v1, COALESCE(col_int16, col_int32) AS v2, COALESCE(col_float32, col_float64) AS v3 FROM T_ALL_TYPES;

--#[cond-coalesce-03]
SELECT COALESCE(col_date, col_timestamp) AS v FROM T_ALL_TYPES;

--#[cond-coalesce-04]
SELECT NVL(c, z, 'none') AS v1, NVL(b, 1, 2) AS v2 FROM T;

-- The common type DECIMAL(38, 30) cannot hold every INTEGER value.
--#[cond-coalesce-05]
SELECT COALESCE(CAST(b AS DECIMAL(38, 30)), b) AS v FROM T;

-- A string literal converts to the type of the other arguments, as in Redshift.
--#[cond-coalesce-06]
SELECT COALESCE(col_int32, '0') AS v1, COALESCE(col_date, '2020-01-01') AS v2, COALESCE(col_bool, 't') AS v3 FROM T_ALL_TYPES;

--#[cond-coalesce-07]
SELECT COALESCE(col_int32, 'abc') AS v FROM T_ALL_TYPES;

--#[cond-coalesce-08]
SELECT COALESCE(col_int32, col_string) AS v FROM T_ALL_TYPES;

-- A CHAR(n) value unified with other character strings loses its padding.
--#[cond-coalesce-09]
SELECT COALESCE(col_char, 'x') AS v FROM T_ALL_TYPES;

--#[cond-coalesce-10]
SELECT COALESCE(x, 0) AS v FROM T;

--#[cond-nullif-00]
SELECT NULLIF(b, 0) AS v1, NULLIF(c, '') AS v2 FROM T;

-- NULLIF disregards the padding of a CHAR(n) argument.
--#[cond-nullif-01]
SELECT NULLIF(col_char, '') AS v1, NULLIF(col_char, 'ab  ') AS v2 FROM T_ALL_TYPES;

--#[cond-nullif-02]
SELECT NULLIF(col_int32, '0') AS v FROM T_ALL_TYPES;

-- Redshift compares a number with a character string as text, so '-0' is not 0.
--#[cond-nullif-03]
SELECT NULLIF(col_int32, '-0') AS v FROM T_ALL_TYPES;

--#[cond-nullif-04]
SELECT NULLIF(col_int32, col_string) AS v FROM T_ALL_TYPES;

--#[cond-decode-00]
SELECT DECODE(b, 1, 'one', 2, 'two', 'other') AS v FROM T;

-- A NULL search value matches a NULL expression.
--#[cond-decode-01]
SELECT DECODE(b, NULL, 0, 1) AS v FROM T;

--#[cond-decode-02]
SELECT DECODE(timestamp_1, timestamp_2, 'same') AS v FROM T;

--#[cond-decode-03]
SELECT DECODE(b, 1, 2, 2.5) AS v1, DECODE(b, '1', 'one') AS v2 FROM T;

--#[cond-decode-04]
SELECT DECODE(col_char, 'ab', 1, 0) AS v FROM T_ALL_TYPES;

--#[cond-decode-05]
SELECT DECODE(b, 1) AS v FROM T;

--#[cond-greatest-00]
SELECT GREATEST(b, 0) AS v1, LEAST(b, 10) AS v2, GREATEST(b) AS v3 FROM T;

--#[cond-greatest-01]
SELECT GREATEST(timestamp_1, timestamp_2) AS v FROM T;

--#[cond-greatest-02]
SELECT GREATEST(timestamp_1, timestamp_2, TIMESTAMP '2020-01-01 00:00:00') AS v FROM T;

--#[cond-greatest-03]
SELECT GREATEST(b, 2.5) AS v FROM T;

--#[cond-greatest-04]
SELECT LEAST(col_char, 'b') AS v FROM T_ALL_TYPES;

--#[cond-greatest-05]
SELECT GREATEST(x, 1) AS v FROM T;

--#[cond-greatest-06]
SELECT GREATEST(b, c) AS v FROM T;

--#[cond-nvl2-00]
SELECT NVL2(c, 'present', 'absent') AS v1, NVL2(b, b, 0) AS v2, NVL2(c, b, 2.5) AS v3 FROM T;

--#[cond-nvl2-01]
SELECT NVL2(col_int32, 5, '7') AS v1, NVL2(col_int32, col_date, col_timestamp) AS v2 FROM T_ALL_TYPES;

-- Redshift converts the second value to the type of the first.
--#[cond-nvl2-02]
SELECT NVL2(c, 'x', b) AS v FROM T;

--#[cond-nvl2-03]
SELECT NVL2(col_int32, t.foo.keep, col_string) AS v FROM T_ALL_TYPES, datatypes.T_STRING_16 t;

-- The Scribe PartiQL target prints a subquery argument of COALESCE without parentheses.
--#[cond-subquery-00]
SELECT NVL((SELECT MAX(b) FROM T), 0) AS v FROM T;

--#[cond-subquery-01]
SELECT GREATEST((SELECT MAX(b) FROM T), 0) AS v FROM T;

-- A type-dependent rule over an aggregate probes the aggregate with a NULL of its type.
--#[cond-nested-00]
SELECT NVL(SUM(b), 0) AS v FROM T;

--#[cond-nested-01]
SELECT DECODE(COUNT(*), 0, 'none', 'some') AS v FROM T;

--#[cond-nested-02]
SELECT c FROM T GROUP BY c HAVING NVL(SUM(b), 0) > 10;
