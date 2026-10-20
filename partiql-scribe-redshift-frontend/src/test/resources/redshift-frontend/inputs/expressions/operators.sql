--#[concat-00]
SELECT c || z AS v FROM T;

-- Redshift removes the padding of a CHAR(n) value when it converts it to VARCHAR.
--#[concat-01]
SELECT col_char || col_string AS v FROM T_ALL_TYPES;

--#[concat-02]
SELECT col_char || col_char AS v FROM T_ALL_TYPES;

-- Redshift converts an integer, DECIMAL, or DATE operand to a character string.
--#[concat-03]
SELECT c || ' seats ' || b AS v FROM T;

--#[concat-04]
SELECT t.foo.bar || t.foo.keep AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[concat-05]
SELECT col_string || col_date AS v FROM T_ALL_TYPES;

--#[concat-06]
SELECT col_string || col_timestamp AS v FROM T_ALL_TYPES;

--#[concat-07]
SELECT col_string || col_float64 AS v FROM T_ALL_TYPES;

-- PartiQL writes DECIMAL values with an exponent when the scale is 7 or more.
--#[concat-08]
SELECT col_string || CAST(col_int32 AS DECIMAL(18,8)) AS v FROM T_ALL_TYPES;

--#[concat-09]
SELECT b || b AS v FROM T;

--#[arithmetic-00]
SELECT b * 2 + 1 AS v1, b / 2 AS v2, b % -3 AS v3, -7 / 2 AS v4 FROM T;

--#[arithmetic-01]
SELECT t.foo.keep * 2 + 1 AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[arithmetic-02]
SELECT t.col_float64 * t.col_decimal AS v1, t.col_float64 / t.col_int32 AS v2 FROM T_ALL_TYPES t;

-- Redshift computes REAL with another number in DOUBLE PRECISION.
--#[arithmetic-03]
SELECT t.col_float32 + 1 AS v1, t.col_float32 * t.col_decimal AS v2, t.col_float32 - t.col_float64 AS v3 FROM T_ALL_TYPES t;

--#[arithmetic-04]
SELECT t.col_float32 * t.col_float32 AS v FROM T_ALL_TYPES t;

--#[arithmetic-05]
SELECT t.col_float64 % 2 AS v FROM T_ALL_TYPES t;

--#[arithmetic-06]
SELECT 0.12345678901234567890 * 0.12345678901234567890 AS v FROM T;

--#[arithmetic-07]
SELECT x + 1 AS v FROM T;

--#[arithmetic-08]
SELECT c + 1 AS v FROM T;

-- A DECIMAL quotient is exact when the reciprocal of a literal divisor has few fraction digits.
--#[decimal-division-00]
SELECT b / 100.0 AS v FROM T;

--#[decimal-division-05]
SELECT t.foo.keep / 8 AS v1, t.foo.keep / 0.04 AS v2 FROM datatypes.T_DECIMAL_5_2 t;

--#[decimal-division-01]
SELECT 2.0 / 3.0 AS v FROM T;

--#[decimal-division-02]
SELECT t.foo.keep / 3 AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[decimal-division-03]
SELECT t.col_decimal / 100 AS v FROM T_ALL_TYPES t;

--#[decimal-division-04]
SELECT 5. / 2. AS v FROM T;

-- PartiQL types -x of a DECIMAL(p,s) value as DECIMAL(38,19).
--#[unary-00]
SELECT -t.col_decimal + 1 AS v1, +t.col_decimal AS v2, -t.col_int32 AS v3 FROM T_ALL_TYPES t;

--#[unary-01]
SELECT -x AS v FROM T;

--#[abs-operator-00]
SELECT @ -5.0 AS v1, @ b AS v2, @ -5 + 1 AS v3 FROM T;

--#[abs-operator-01]
SELECT @ x AS v FROM T;

-- PartiQL does not plan an aggregate call in the argument of a function such as ABS.
--#[abs-operator-02]
SELECT @ MAX(b) AS v FROM T;

--#[bitwise-and-00]
SELECT b & 3 AS v1, b + 1 & 3 AS v2 FROM T;

--#[bitwise-and-01]
SELECT a FROM T WHERE b & 1 = 1;

--#[bitwise-and-02]
SELECT t.col_decimal & 1 AS v FROM T_ALL_TYPES t;

--#[operator-00]
SELECT b | 1 AS v FROM T;

--#[operator-01]
SELECT 1 << 4 AS v FROM T;

--#[operator-02]
SELECT b >> 2 AS v FROM T;

--#[operator-03]
SELECT 2 ^ 3 AS v FROM T;

--#[operator-04]
SELECT |/ 25.0 AS v FROM T;

--#[operator-05]
SELECT ||/ 27.0 AS v FROM T;

--#[operator-06]
SELECT ~ b AS v FROM T;

-- `||/*` is a concatenation followed by a comment.
--#[operator-07]
SELECT c ||/* comment */ z AS v FROM T;
