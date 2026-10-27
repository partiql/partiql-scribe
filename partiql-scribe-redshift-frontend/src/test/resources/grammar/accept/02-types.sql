-- Data types, as CAST targets: the numeric, character, datetime, and boolean types with their Redshift aliases.
-- https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html

--#[type-smallint]
SELECT CAST(a AS SMALLINT), CAST(a AS INT2) FROM t;

--#[type-integer]
SELECT CAST(a AS INTEGER), CAST(a AS INT), CAST(a AS INT4) FROM t;

--#[type-bigint]
SELECT CAST(a AS BIGINT), CAST(a AS INT8) FROM t;

--#[type-decimal]
SELECT CAST(a AS DECIMAL), CAST(a AS DECIMAL(10)), CAST(a AS DECIMAL(10, 2)) FROM t;

--#[type-numeric]
SELECT CAST(a AS NUMERIC), CAST(a AS NUMERIC(18, 0)) FROM t;

--#[type-real]
SELECT CAST(a AS REAL), CAST(a AS FLOAT4) FROM t;

--#[type-double]
SELECT CAST(a AS DOUBLE PRECISION), CAST(a AS FLOAT8), CAST(a AS FLOAT) FROM t;

--#[type-char]
SELECT CAST(a AS CHAR), CAST(a AS CHAR(10)), CAST(a AS CHARACTER(10)), CAST(a AS NCHAR(10)), CAST(a AS BPCHAR) FROM t;

--#[type-varchar]
SELECT CAST(a AS VARCHAR), CAST(a AS VARCHAR(256)), CAST(a AS VARCHAR(MAX)) FROM t;

--#[type-character-varying]
SELECT CAST(a AS CHARACTER VARYING(10)), CAST(a AS CHAR VARYING(10)), CAST(a AS NVARCHAR(10)), CAST(a AS TEXT) FROM t;

--#[type-date]
SELECT CAST(a AS DATE) FROM t;

--#[type-timestamp]
SELECT CAST(a AS TIMESTAMP), CAST(a AS TIMESTAMP WITHOUT TIME ZONE) FROM t;

--#[type-boolean]
SELECT CAST(a AS BOOLEAN), CAST(a AS BOOL) FROM t;

--#[type-case]
SELECT CAST(a AS varchar(10)), a::Int, CONVERT(Double Precision, a) FROM t;

--#[type-shorthand]
SELECT a::SMALLINT, a::DECIMAL(10, 2), a::VARCHAR(MAX), a::TIMESTAMP, a::BOOL FROM t;

--#[type-convert]
SELECT CONVERT(INTEGER, a), CONVERT(VARCHAR(10), a), CONVERT(DATE, a) FROM t;
