-- ABS of DECIMAL(p, s) keeps the type, which PLK widens to DECIMAL(39, 19).
--#[math-abs-00]
SELECT ABS(t.foo.keep) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[math-abs-01]
SELECT ABS(b) AS v FROM T;

-- Redshift returns NULL for a SUPER value that is not a number, and PartiQL returns MISSING.
--#[math-abs-02]
SELECT ABS(x) AS v FROM T;

--#[math-mod-00]
SELECT MOD(b, 3) AS v FROM T;

--#[math-mod-01]
SELECT MOD(t.foo.keep, 2) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[math-mod-02]
SELECT MOD(t.col_float64, 2) AS v FROM T_ALL_TYPES t;

-- ROUND rounds half away from zero, as a CAST to DECIMAL(38, n) does.
--#[math-round-00]
SELECT ROUND(t.foo.keep, 1) AS v1, ROUND(t.foo.keep) AS v2, ROUND(t.foo.keep, 2) AS v3 FROM datatypes.T_DECIMAL_5_2 t;

--#[math-round-01]
SELECT ROUND(t.foo.keep, -1) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[math-round-02]
SELECT ROUND(b) AS v FROM T;

-- Redshift does not document how it rounds ties of floating-point values.
--#[math-round-03]
SELECT ROUND(t.col_float64, 2) AS v FROM T_ALL_TYPES t;

-- Redshift does not document the conversion of a BIGINT argument; a conversion to DOUBLE PRECISION changes large values.
--#[math-round-04]
SELECT ROUND(col_int64) AS v FROM T_ALL_TYPES;

--#[math-round-05]
SELECT ROUND(t.foo.keep, r.b) AS v FROM datatypes.T_DECIMAL_5_2 t, T AS r;

--#[math-floor-00]
SELECT FLOOR(t.foo.keep) AS v FROM datatypes.T_DECIMAL_5_2 t;

-- The first test returns infinite values unchanged.
--#[math-floor-01]
SELECT FLOOR(t.col_float64) AS v FROM T_ALL_TYPES t;

--#[math-floor-02]
SELECT FLOOR(b) AS v FROM T;

--#[math-floor-03]
SELECT FLOOR(x) AS v FROM T;

--#[math-ceil-00]
SELECT CEILING(t.foo.keep) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[math-ceil-01]
SELECT CEIL(t.col_float64) AS v FROM T_ALL_TYPES t;

--#[math-trunc-00]
SELECT TRUNC(t.foo.keep, 1) AS v1, TRUNC(t.foo.keep) AS v2, TRUNC(t.foo.keep, -1) AS v3 FROM datatypes.T_DECIMAL_5_2 t;

--#[math-trunc-01]
SELECT TRUNC(t.col_float64) AS v FROM T_ALL_TYPES t;

--#[math-trunc-02]
SELECT TRUNC(b) AS v1, TRUNC(b, -1) AS v2 FROM T;

--#[math-trunc-03]
SELECT TRUNC(col_int64) AS v FROM T_ALL_TYPES;

--#[math-trunc-04]
SELECT TRUNC(t.col_float64, 1) AS v FROM T_ALL_TYPES t;

-- TRUNC of a timestamp returns its date.
--#[math-trunc-05]
SELECT TRUNC(timestamp_1) AS v FROM T;

-- TRUNC of a TIMESTAMPTZ value returns its date in UTC, the Redshift session time zone.
--#[math-trunc-06]
SELECT TRUNC(t.col_timestampz) AS v FROM T_ALL_TYPES AS t;

--#[math-sign-00]
SELECT SIGN(r.b) AS v1, SIGN(t.foo.keep) AS v2 FROM T AS r, datatypes.T_DECIMAL_5_2 t;

--#[math-sign-01]
SELECT SIGN(t.col_float64) AS v FROM T_ALL_TYPES t;

--#[math-pi-00]
SELECT PI() AS v FROM T;

-- POWER of literals whose exact result is a DOUBLE PRECISION value.
--#[math-power-00]
SELECT POWER(2, 10) AS v1, POW(2, -1) AS v2, POWER(2.5, 2) AS v3 FROM T;

--#[math-power-01]
SELECT POWER(b, 2) AS v FROM T;

-- 0.1 is not a DOUBLE PRECISION value, so the result of the power function is not exact.
--#[math-power-02]
SELECT POWER(0.1, 2) AS v FROM T;

--#[math-power-03]
SELECT POWER(3, -1) AS v FROM T;

--#[math-unsupported-00]
SELECT SQRT(b) AS v FROM T;

--#[math-unsupported-01]
SELECT EXP(b) AS v FROM T;

--#[math-unsupported-02]
SELECT LN(b) AS v FROM T;

--#[math-unsupported-03]
SELECT LOG(2, b) AS v FROM T;

--#[math-unsupported-04]
SELECT ATAN2(b, 1) AS v FROM T;

--#[math-unsupported-05]
SELECT RANDOM() AS v FROM T;

--#[math-unsupported-06]
SELECT DEGREES(b) AS v FROM T;
