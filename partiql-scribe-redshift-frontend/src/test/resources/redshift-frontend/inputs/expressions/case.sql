-- PartiQL types a CASE with the common type of its results, but does not convert them.
--#[case-types-00]
SELECT CASE WHEN a THEN b ELSE 2.5 END AS v FROM T;

--#[case-types-01]
SELECT CASE WHEN col_bool THEN col_int64 ELSE col_int16 END AS v FROM T_ALL_TYPES;

--#[case-types-02]
SELECT CASE WHEN col_bool THEN col_date ELSE col_timestamp END AS v FROM T_ALL_TYPES;

--#[case-types-03]
SELECT CASE WHEN col_bool THEN col_float64 ELSE 2.5 END AS v FROM T_ALL_TYPES;

-- The common type of DECIMAL(38,0) and DECIMAL(2,1) cannot hold every value.
--#[case-types-04]
SELECT CASE WHEN col_bool THEN col_decimal ELSE 2.5 END AS v FROM T_ALL_TYPES;

--#[case-types-05]
SELECT CASE WHEN col_bool THEN col_float32 ELSE 2.5 END AS v FROM T_ALL_TYPES;

--#[case-types-06]
SELECT CASE WHEN a THEN b ELSE c END AS v FROM T;

-- A character string literal result takes the type of the other results.
--#[case-types-07]
SELECT CASE WHEN a THEN b ELSE '0' END AS v FROM T;

--#[case-types-08]
SELECT CASE WHEN a THEN c ELSE 'none' END AS v1, CASE WHEN a THEN x ELSE b END AS v2 FROM T;

-- A WHEN NULL branch is never taken.
--#[case-null-00]
SELECT CASE WHEN NULL THEN 1 ELSE 2 END AS v FROM T;

--#[case-null-01]
SELECT CASE WHEN NULL THEN 2.5 ELSE 1 END AS v FROM T;

--#[case-null-02]
SELECT CASE WHEN a THEN NULL WHEN NULL THEN 3 ELSE b END AS v FROM T;

--#[case-null-03]
SELECT CASE WHEN NULL THEN 1 END AS v FROM T;

-- A simple CASE compares with `=`, with its conversions.
--#[case-simple-00]
SELECT CASE b WHEN 1 THEN 'one' WHEN 2 THEN 'two' END AS v FROM T;

--#[case-simple-01]
SELECT CASE col_date WHEN '2020-01-01' THEN 1 ELSE 0 END AS v FROM T_ALL_TYPES;

--#[case-simple-02]
SELECT CASE col_char WHEN 'abc' THEN 1 ELSE 0 END AS v FROM T_ALL_TYPES;

--#[case-condition-00]
SELECT CASE WHEN b > 0 THEN 'x' WHEN b THEN 'int' ELSE 'z' END AS v FROM T;
