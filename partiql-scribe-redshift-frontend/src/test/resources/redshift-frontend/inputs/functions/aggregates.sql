-- COUNT is the same in both languages.
--#[agg-count-00]
SELECT c, COUNT(*) AS n, COUNT(z) AS nz, COUNT(DISTINCT z) AS dz FROM T GROUP BY c;

-- SUM by argument type: BIGINT through DECIMAL(38, 0) (PLK 1.6.1 fails for BIGINT), REAL through DOUBLE PRECISION.
--#[agg-sum-00]
SELECT SUM(col_int16) AS s16, SUM(col_int32) AS s32, SUM(col_int64) AS s64, SUM(col_float32) AS sf32, SUM(col_float64) AS sf64 FROM T_ALL_TYPES;

-- SUM of DECIMAL(p, s) has the type DECIMAL(38, s).
--#[agg-sum-01]
SELECT SUM(de) AS s FROM T_DECIMALS;

--#[agg-sum-02]
SELECT SUM(DISTINCT b) AS s FROM T;

--#[agg-sum-03]
SELECT SUM(t.foo.keep * 2) AS s FROM datatypes.T_DECIMAL_5_2 t;

-- The scale of a DECIMAL quotient differs between the languages.
--#[agg-sum-04]
SELECT SUM(de / 2) AS s FROM T_DECIMALS;

--#[agg-sum-05]
SELECT SUM(x) AS s FROM T;

-- AVG of an integer is BIGINT without the fraction in Redshift.
--#[agg-avg-00]
SELECT AVG(b) AS v FROM T;

--#[agg-avg-01]
SELECT c, AVG(DISTINCT b) AS v FROM T GROUP BY c;

--#[agg-avg-02]
SELECT AVG(col_int16) AS v16, AVG(col_int64) AS v64 FROM T_ALL_TYPES;

--#[agg-avg-03]
SELECT AVG(col_float32) AS v1, AVG(col_float64) AS v2 FROM T_ALL_TYPES;

-- Redshift AVG of DECIMAL(p, s) returns DECIMAL(38, s); how it shortens the fraction is not documented.
--#[agg-avg-04]
SELECT AVG(col_decimal) AS v FROM T_ALL_TYPES;

--#[agg-avg-05]
SELECT AVG(x) AS v FROM T;

--#[agg-min-max-00]
SELECT MIN(b) AS lo, MAX(timestamp_1) AS hi, MAX(DISTINCT c) AS mc FROM T;

-- MIN and MAX of DECIMAL(p, s) keep the type, which PLK widens to DECIMAL(38, 19).
--#[agg-min-max-01]
SELECT MIN(t.foo.keep) AS lo, MAX(t.foo.keep) AS hi FROM datatypes.T_DECIMAL_5_2 t;

-- Redshift disregards the padding of CHAR(n) values when it compares them.
--#[agg-min-max-02]
SELECT MAX(col_char) AS m FROM T_ALL_TYPES;

--#[agg-min-max-03]
SELECT MAX(a) AS v FROM T;

--#[agg-min-max-04]
SELECT MIN(x) AS v FROM T;

--#[agg-any-value-00]
SELECT c, ANY_VALUE(z) AS z FROM T GROUP BY c;

--#[agg-bool-00]
SELECT c, BOOL_AND(a) AS all_a, BOOL_OR(a) AS any_a FROM T GROUP BY c;

--#[agg-bool-01]
SELECT c, BOOL_OR(b > 5) AS big FROM T GROUP BY c;

--#[agg-bool-02]
SELECT BOOL_AND(b) AS v FROM T;

--#[agg-having-00]
SELECT c FROM T GROUP BY c HAVING SUM(b) > 10;

-- The PLK 1.6.1 planner does not find an aggregate in the argument of a function call.
--#[agg-nested-02]
SELECT ABS(SUM(b)) AS v FROM T;

-- The rule for + probes its operands, which hides an aggregate from the planner in the probe passes.
--#[agg-nested-03]
SELECT c, SUM(b) + 1 AS v1, AVG(b) - 1 AS v2 FROM T GROUP BY c;

--#[agg-nested-04]
SELECT MAX(b) - MIN(b) AS v FROM T;

--#[agg-unsupported-00]
SELECT LISTAGG(c, ',') AS l FROM T;

--#[agg-unsupported-01]
SELECT MEDIAN(b) AS m FROM T;

--#[agg-unsupported-02]
SELECT STDDEV_SAMP(b) AS s FROM T;

--#[agg-unsupported-03]
SELECT VAR_POP(b) AS v FROM T;

--#[agg-unsupported-04]
SELECT BIT_AND(b) AS v FROM T;

--#[agg-unsupported-05]
SELECT HLL(c) AS n FROM T;
