-- Redshift converts a character string literal to the type of the other operand.
--#[string-literal-date-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date = '2020-01-01';

--#[string-literal-date-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date <> ' 2020-01-01 ';

-- A time of day other than midnight: the Redshift result is not documented.
--#[string-literal-date-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date = '2008-01-04 10:00:00';

--#[string-literal-date-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE '2020-01-01' < col_date;

-- A date without a time is midnight.
--#[string-literal-timestamp-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_timestamp >= '2020-01-01';

--#[string-literal-timestamp-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_timestamp < '2020-01-01 10:20:30.5';

--#[string-literal-timestamp-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_timestamp = 'Jun 1,2008 09:59:59';

--#[string-literal-boolean-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool = 't';

--#[string-literal-boolean-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool <> 'No';

--#[string-literal-boolean-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool = 'maybe';

-- 1 and 0 are TRUE and FALSE next to a BOOLEAN value.
--#[integer-literal-boolean-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool = 1 AND col_bool <> 0;

--#[integer-literal-boolean-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool = 2;

-- An integer compared with a character string: Redshift converts the integer to a string, so only the canonical text
-- of an integer gives the same result as an integer comparison.
--#[string-literal-integer-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 = '5';

--#[string-literal-integer-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 = '-0';

--#[string-literal-integer-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 = '05';

--#[string-literal-integer-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 > '10';

-- Redshift converts the string to DECIMAL.
--#[string-literal-decimal-00]
SELECT t.foo.bar FROM datatypes.T_DECIMAL_5_2 t WHERE t.foo.keep >= '1.5';

--#[string-literal-decimal-01]
SELECT t.foo.bar FROM datatypes.T_DECIMAL_5_2 t WHERE t.foo.keep = '1.505';

--#[string-literal-float-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 = '1.5';

-- A non-literal number and a character string: unsupported.
--#[number-string-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_string = col_int32;

--#[number-string-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_string = 1;

--#[cross-family-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date = col_string;

--#[cross-family-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_bool = col_int32;

-- Redshift ignores the padding of a CHAR(n) value in a comparison.
--#[char-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_char = 'abc  ';

--#[char-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_char = col_string;

-- VARCHAR values are compared as they are; see the README.
--#[varchar-00]
SELECT a FROM T WHERE c = 'abc  ' AND c <> z;

-- Two literals compare exactly.
--#[literals-00]
SELECT a FROM T WHERE 'a' = 'a ';

-- Redshift compares DOUBLE PRECISION with DECIMAL, and REAL with integers, in DOUBLE PRECISION.
--#[float-decimal-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 <= 0.1;

--#[float-decimal-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 = col_decimal;

--#[float-decimal-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float32 > col_int32;

--#[float-decimal-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 > col_int64 AND col_float32 < col_float64;

-- A literal that the floating-point type holds exactly needs no conversion.
--#[float-decimal-04]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 < 100.0 AND col_float32 >= 0.5 AND col_float32 < 0.1;

--#[datetime-00]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date < col_timestampz AND col_timestamp >= col_date;

--#[interval-00]
SELECT t.col_date FROM T_INTERVALS t WHERE t.col_y2mon = t.col_y2mon AND t.col_d2s <> t.col_d2s;

--#[interval-01]
SELECT t.col_date FROM T_INTERVALS t WHERE t.col_d2s > t.col_d2s;

--#[interval-02]
SELECT t.col_date FROM T_INTERVALS t WHERE t.col_y2mon = t.col_d2s;

-- SUPER values are compared as they are.
--#[super-00]
SELECT b FROM T t WHERE t.x.k = 5 AND t.x.s <> 'a';
