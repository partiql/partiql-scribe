--#[cast-00]
SELECT CAST('true' AS BOOL);

-- NOT SUPPORTED #[cast-01]
-- SELECT CAST('1' AS TINYINT);

--#[cast-02]
SELECT CAST('1' AS SMALLINT);

--#[cast-03]
SELECT CAST('1' AS SMALLINT);

--#[cast-04]
SELECT CAST('1' AS INT);

--#[cast-05]
SELECT CAST('1' AS INT);

--#[cast-06]
SELECT CAST('1' AS INT);

--#[cast-07]
SELECT CAST('1' AS BIGINT);

--#[cast-08]
SELECT CAST('1' AS BIGINT);

--#[cast-09]
SELECT CAST('1' AS REAL);

--#[cast-10]
SELECT CAST('1' AS DOUBLE PRECISION);

--#[cast-11]
SELECT CAST('1' AS DOUBLE PRECISION);

--#[cast-12]
SELECT CAST('1' AS DECIMAL);

--#[cast-13]
SELECT CAST('1' AS DECIMAL(1));

--#[cast-14]
SELECT CAST('1' AS DECIMAL(1, 0));

--#[cast-15]
SELECT CAST('1' AS DECIMAL);

--#[cast-16]
SELECT CAST('1' AS DECIMAL(1));

--#[cast-17]
SELECT CAST('1' AS DECIMAL(1, 0));

--#[cast-18]
SELECT CAST('1' AS CHAR);

--#[cast-19]
SELECT CAST('1' AS CHARACTER);

--#[cast-20]
SELECT CAST('1' AS CHAR(1));

--#[cast-21]
SELECT CAST('1' AS CHARACTER(1));

--#[cast-22]
SELECT CAST('1' AS CHARACTER VARYING);

--#[cast-23]
SELECT CAST('1' AS VARCHAR);

--#[cast-24]
SELECT CAST('1' AS CHARACTER VARYING (1));

--#[cast-25]
SELECT CAST('1' AS VARCHAR(1));

--#[cast-26]
SELECT CAST('1' AS STRING);

--#[cast-27]
SELECT CAST('1' AS BLOB);

--#[cast-28]
SELECT CAST('1' AS BLOB(1));

--#[cast-29]
SELECT CAST('1' AS CLOB);

--#[cast-30]
SELECT CAST('1' AS CLOB(1));

-- TODO add datetime casts as its own suite.

--#[cast-31]
SELECT CAST('1969-07-16' AS DATE);

--#[cast-32]
SELECT CAST('12:00:00' AS TIME);

--#[cast-33]
SELECT CAST('1969-07-16 12:00:00' AS TIMESTAMP);

--#[cast-34]
SELECT CAST('1' AS BAG);

--#[cast-35]
SELECT CAST('1' AS LIST);

--#[cast-36]
SELECT CAST('1' AS TUPLE);

--#[cast-37]
SELECT CAST('1' AS STRUCT);
