--#[numeric-00]
SELECT CAST("t"['b'] AS DECIMAL(10,2)) AS "v1", CAST("t"['b'] AS BIGINT) AS "v2", CAST("t"['b'] AS DOUBLE PRECISION) AS "v3", CAST("t"['b'] AS BOOL) AS "v4", CAST("t"['a'] AS INT) AS "v5" FROM "default"."T" AS "t";

--#[decimal-truncation-00]
SELECT CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS INT) AS "v1", CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(0.1 AS DECIMAL(38,1))) AS DECIMAL(5,1)) AS "v2", CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(5,0)) AS "v3" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[decimal-truncation-01]
SELECT CAST("t"['foo']['keep'] AS DECIMAL(10,4)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[decimal-constant-00]
SELECT CAST(162.00 AS INT) AS "v1", CAST(1.200 AS DECIMAL(3,1)) AS "v2" FROM "default"."T" AS "t";

--#[decimal-constant-01]
[UNSUPPORTED{message="CAST of a DECIMAL constant that needs rounding"}];

--#[decimal-constant-02]
[UNSUPPORTED{message="CAST of a DECIMAL constant that needs rounding"}];

--#[cast-float-00]
SELECT CAST("t"['col_float32'] AS DOUBLE PRECISION) AS "v1", CAST("t"['col_decimal'] AS REAL) AS "v2" FROM "default"."T_ALL_TYPES" AS "t";

--#[cast-float-01]
[UNSUPPORTED{message="CAST of DOUBLE PRECISION to an integer type"}];

--#[float-02]
[UNSUPPORTED{message="CAST of REAL or DOUBLE PRECISION to DECIMAL"}];

--#[float-03]
[UNSUPPORTED{message="CAST of DOUBLE PRECISION to REAL"}];

--#[string-number-00]
SELECT CAST(TRIM(BOTH ' 	

' FROM "t"['col_string']) AS INT) AS "v1", CAST(TRIM(BOTH ' 	

' FROM CAST(TRIM(TRAILING FROM "t"['col_char']) AS STRING)) AS BIGINT) AS "v2" FROM "default"."T_ALL_TYPES" AS "t";

--#[string-number-01]
SELECT CAST(12 AS INT) AS "v1", CAST(7 AS SMALLINT) AS "v2", CAST(12.50 AS DECIMAL(10,2)) AS "v3", CAST(0.5 AS DOUBLE PRECISION) AS "v4" FROM "default"."T" AS "t";

--#[string-number-02]
[UNSUPPORTED{message="CAST of the character string '2147483648' to INTEGER"}];

--#[string-number-03]
[UNSUPPORTED{message="CAST of the character string '12.505' to DECIMAL(10,2)"}];

--#[string-number-04]
[UNSUPPORTED{message="CAST of a character string to DECIMAL (PartiQL rejects strings such as '0.5')"}];

--#[string-number-05]
[UNSUPPORTED{message="CAST of a character string to DOUBLE PRECISION (PartiQL rejects strings such as '0.5')"}];

--#[string-boolean-00]
SELECT true AS "v1", false AS "v2" FROM "default"."T" AS "t";

--#[string-boolean-01]
[UNSUPPORTED{message="CAST of a character string to BOOLEAN"}];

--#[string-datetime-00]
SELECT DATE '2026-10-19' AS "v1", TIMESTAMP '2026-10-19 12:30:00' AS "v2", TIMESTAMP '2026-10-19 00:00:00' AS "v3" FROM "default"."T" AS "t";

--#[string-datetime-01]
SELECT DATE '2008-02-18' AS "v" FROM "default"."T" AS "t";

--#[string-datetime-02]
[UNSUPPORTED{message="CAST of a character string to DATE (PartiQL cannot convert a string value to DATE)"}];

--#[string-datetime-03]
[UNSUPPORTED{message="CAST of a character string to TIMESTAMP (PartiQL cannot convert a string value to TIMESTAMP)"}];

--#[cast-varchar-00]
SELECT CAST("t"['b'] AS STRING) AS "v1", CAST("t"['b'] AS VARCHAR(30)) AS "v2", CAST("t"['b'] AS STRING) AS "v3", CAST("t"['b'] AS STRING) AS "v4" FROM "default"."T" AS "t";

--#[cast-varchar-01]
SELECT CAST(CAST("t"['foo']['keep'] AS STRING) AS VARCHAR(20)) AS "v1", CAST("t"['foo']['keep'] AS STRING) AS "v2" FROM "default"."datatypes"."T_STRING_16" AS "t";

--#[cast-varchar-02]
[UNSUPPORTED{message="CAST to VARCHAR(10) that can truncate a character string"}];

--#[cast-varchar-03]
[UNSUPPORTED{message="CAST to VARCHAR(10) that can truncate a character string"}];

--#[varchar-04]
[UNSUPPORTED{message="CAST to VARCHAR without a length of a string that can exceed 256 bytes"}];

--#[varchar-05]
SELECT "t"['c'] AS "v1", CAST('abc' AS STRING) AS "v2", CAST('abc' AS VARCHAR(2)) AS "v3" FROM "default"."T" AS "t";

--#[varchar-06]
[UNSUPPORTED{message="CAST to VARCHAR(3) that can truncate a character string"}];

--#[varchar-07]
SELECT CAST(TRIM(TRAILING FROM "t"['col_char']) AS STRING) AS "v1", CAST(CAST(TRIM(TRAILING FROM "t"['col_char']) AS STRING) AS VARCHAR(5)) AS "v2" FROM "default"."T_ALL_TYPES" AS "t";

--#[number-string-00]
SELECT CAST("t"['foo']['keep'] AS STRING) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[number-string-01]
SELECT CAST("t"['foo']['keep'] * "t"['foo']['keep'] AS VARCHAR(20)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[number-string-02]
[UNSUPPORTED{message="CAST of a DECIMAL value to a character string"}];

--#[number-string-03]
[UNSUPPORTED{message="CAST of DOUBLE PRECISION to a character string"}];

--#[datetime-string-00]
SELECT CAST(CAST("t"['col_date'] AS STRING) AS VARCHAR(10)) AS "v1", CAST("t"['col_date'] AS STRING) AS "v2" FROM "default"."T_ALL_TYPES" AS "t";

--#[datetime-string-01]
[UNSUPPORTED{message="CAST of TIMESTAMP to a character string"}];

--#[boolean-string-00]
[UNSUPPORTED{message="CAST of BOOLEAN to VARCHAR"}];

--#[cast-char-00]
SELECT CAST("t"['b'] AS CHAR(5)) AS "v1", CAST('abc' AS CHAR(5)) AS "v2" FROM "default"."T" AS "t";

--#[cast-char-01]
[UNSUPPORTED{message="CAST to CHAR(n) outside the SELECT list"}];

--#[cast-datetime-00]
SELECT CAST("t"['col_date'] AS TIMESTAMP) AS "v1", CAST("t"['col_timestamp'] AS DATE) AS "v2", CAST("t"['col_timestamp'] AS TIME (6)) AS "v3", CAST("t"['col_timestampz'] AS DATE) AS "v4" FROM "default"."T_ALL_TYPES" AS "t";

--#[datetime-01]
SELECT CAST(CAST("t"['col_timestampz'] AS TIME (6)) AS TIME (6) WITH TIME ZONE) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

--#[datetime-02]
[UNSUPPORTED{message="CAST of TIME to TIMESTAMP"}];

--#[datetime-03]
[UNSUPPORTED{message="CAST of TIMESTAMP to TIMETZ"}];

--#[datetime-04]
[UNSUPPORTED{message="CAST of a TIMETZ literal with an offset to TIME"}];

--#[null-00]
SELECT CAST(NULL AS DATE) AS "v1", CAST(NULL AS STRING) AS "v2", CAST(NULL AS INT) AS "v3" FROM "default"."T" AS "t";

--#[super-00]
[UNSUPPORTED{message="CAST of a SUPER value"}];

--#[super-01]
[UNSUPPORTED{message="CAST of a SUPER value"}];

--#[convert-00]
SELECT CAST("t"['b'] AS INT) AS "v1", CAST("t"['b'] AS VARCHAR(30)) AS "v2" FROM "default"."T" AS "t";

--#[try-cast-00]
[UNSUPPORTED{message="TRY_CAST"}];
