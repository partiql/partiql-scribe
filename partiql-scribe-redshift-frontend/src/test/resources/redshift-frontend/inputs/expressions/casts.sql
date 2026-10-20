--#[numeric-00]
SELECT CAST(b AS DECIMAL(10,2)) AS v1, b::bigint AS v2, b::double precision AS v3, CAST(b AS BOOLEAN) AS v4, CAST(a AS INT) AS v5 FROM T;

-- Redshift truncates a DECIMAL value from a table when a cast reduces its scale; PartiQL rounds.
--#[decimal-truncation-00]
SELECT CAST(t.foo.keep AS INT) AS v1, CAST(t.foo.keep AS DECIMAL(5,1)) AS v2, t.foo.keep::numeric(5) AS v3 FROM datatypes.T_DECIMAL_5_2 t;

--#[decimal-truncation-01]
SELECT CAST(t.foo.keep AS DECIMAL(10,4)) AS v FROM datatypes.T_DECIMAL_5_2 t;

-- Redshift rounds a constant.
--#[decimal-constant-00]
SELECT CAST(162.00 AS INT) AS v1, 1.200::decimal(3,1) AS v2 FROM T;

--#[decimal-constant-01]
SELECT CAST(2.5 AS INT) AS v FROM T;

--#[decimal-constant-02]
SELECT 1.249::decimal(3,1) AS v FROM T;

--#[cast-float-00]
SELECT CAST(t.col_float32 AS DOUBLE PRECISION) AS v1, CAST(t.col_decimal AS REAL) AS v2 FROM T_ALL_TYPES t;

--#[cast-float-01]
SELECT CAST(t.col_float64 AS INT) AS v FROM T_ALL_TYPES t;

--#[float-02]
SELECT CAST(t.col_float64 AS DECIMAL(10,2)) AS v FROM T_ALL_TYPES t;

--#[float-03]
SELECT CAST(t.col_float64 AS REAL) AS v FROM T_ALL_TYPES t;

-- Redshift ignores blanks around a number in a character string; PartiQL fails on some, such as '0 ' and ' 012'.
--#[string-number-00]
SELECT CAST(t.col_string AS INT) AS v1, CAST(t.col_char AS BIGINT) AS v2 FROM T_ALL_TYPES t;

--#[string-number-01]
SELECT CAST(' 12 ' AS INT) AS v1, '+7'::smallint AS v2, CAST('12.50' AS DECIMAL(10,2)) AS v3, CAST('0.5' AS DOUBLE PRECISION) AS v4 FROM T;

--#[string-number-02]
SELECT CAST('2147483648' AS INT) AS v FROM T;

--#[string-number-03]
SELECT CAST('12.505' AS DECIMAL(10,2)) AS v FROM T;

-- PartiQL fails on strings such as '0.5'.
--#[string-number-04]
SELECT CAST(c AS DECIMAL(10,2)) AS v FROM T;

--#[string-number-05]
SELECT CAST(c AS DOUBLE PRECISION) AS v FROM T;

--#[string-boolean-00]
SELECT CAST('yes' AS BOOLEAN) AS v1, 'f'::bool AS v2 FROM T;

--#[string-boolean-01]
SELECT c::boolean AS v FROM T;

-- PartiQL cannot convert a character string to a datetime value; a literal is converted at translation time.
--#[string-datetime-00]
SELECT CAST('2026-10-19' AS DATE) AS v1, '2026-10-19 12:30:00'::timestamp AS v2, CAST('2026-10-19' AS TIMESTAMP) AS v3 FROM T;

-- The time of day is dropped.
--#[string-datetime-01]
SELECT CAST('2008-02-18 02:36:48' AS DATE) AS v FROM T;

--#[string-datetime-02]
SELECT CAST(c AS DATE) AS v FROM T;

--#[string-datetime-03]
SELECT c::timestamp AS v FROM T;

--#[cast-varchar-00]
SELECT CAST(b AS VARCHAR) AS v1, b::varchar(30) AS v2, b::text AS v3, b::varchar(max) AS v4 FROM T;

-- Redshift VARCHAR(n) holds n bytes; PartiQL VARCHAR(n) n characters.
--#[cast-varchar-01]
SELECT CAST(t.foo.keep AS VARCHAR(20)) AS v1, CAST(t.foo.keep AS VARCHAR) AS v2 FROM datatypes.T_STRING_16 t;

--#[cast-varchar-02]
SELECT CAST(t.foo.keep AS VARCHAR(10)) AS v FROM datatypes.T_STRING_16 t;

--#[cast-varchar-03]
SELECT c::varchar(10) AS v FROM T;

-- Whether Redshift truncates a longer value in CAST(x AS VARCHAR) is not documented.
--#[varchar-04]
SELECT c::varchar AS v FROM T;

--#[varchar-05]
SELECT c::varchar(max) AS v1, CAST('abc' AS VARCHAR) AS v2, CAST('abc' AS VARCHAR(2)) AS v3 FROM T;

--#[varchar-06]
SELECT CAST('éé' AS VARCHAR(3)) AS v FROM T;

-- A CHAR(n) value loses its padding when Redshift converts it to VARCHAR.
--#[varchar-07]
SELECT CAST(t.col_char AS VARCHAR) AS v1, CAST(t.col_char AS VARCHAR(5)) AS v2 FROM T_ALL_TYPES t;

--#[number-string-00]
SELECT CAST(t.foo.keep AS VARCHAR) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[number-string-01]
SELECT CAST(t.foo.keep * t.foo.keep AS VARCHAR(20)) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[number-string-02]
SELECT CAST(t.foo.keep / 4 AS VARCHAR) AS v FROM datatypes.T_DECIMAL_5_2 t;

--#[number-string-03]
SELECT CAST(t.col_float64 AS VARCHAR) AS v FROM T_ALL_TYPES t;

--#[datetime-string-00]
SELECT CAST(t.col_date AS VARCHAR(10)) AS v1, CAST(t.col_date AS VARCHAR) AS v2 FROM T_ALL_TYPES t;

--#[datetime-string-01]
SELECT t.col_timestamp::varchar AS v FROM T_ALL_TYPES t;

--#[boolean-string-00]
SELECT t.col_bool::varchar AS v FROM T_ALL_TYPES t;

-- CHAR(n) values take part in comparisons and functions, where Redshift ignores their padding.
--#[cast-char-00]
SELECT CAST(b AS CHAR(5)) AS v1, CAST('abc' AS CHAR(5)) AS v2 FROM T;

--#[cast-char-01]
SELECT a FROM T WHERE CAST(b AS CHAR(5)) = '1';

--#[cast-datetime-00]
SELECT CAST(t.col_date AS TIMESTAMP) AS v1, CAST(t.col_timestamp AS DATE) AS v2, CAST(t.col_timestamp AS TIME) AS v3, CAST(t.col_timestampz AS DATE) AS v4 FROM T_ALL_TYPES t;

-- TIMESTAMPTZ values are UTC instants.
--#[datetime-01]
SELECT CAST(t.col_timestampz AS TIMETZ) AS v FROM T_ALL_TYPES t;

--#[datetime-02]
SELECT CAST(t.col_time AS TIMESTAMP) AS v FROM T_ALL_TYPES t;

--#[datetime-03]
SELECT CAST(t.col_timestamp AS TIMETZ) AS v FROM T_ALL_TYPES t;

--#[datetime-04]
SELECT CAST(TIMETZ '12:30:45+08:00' AS TIME) AS v FROM T;

--#[null-00]
SELECT CAST(NULL AS DATE) AS v1, NULL::varchar AS v2, NULL::int AS v3 FROM T;

-- Redshift casts SUPER values laxly: a value that does not convert is NULL.
--#[super-00]
SELECT t.x.y::int AS v FROM T t;

--#[super-01]
SELECT CAST(t.x AS VARCHAR) AS v FROM T t;

--#[convert-00]
SELECT CONVERT(integer, b) AS v1, CONVERT(varchar(30), b) AS v2 FROM T;

--#[try-cast-00]
SELECT TRY_CAST(c AS INT) AS v FROM T;
