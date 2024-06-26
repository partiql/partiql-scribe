-- tests translations of type names across dialects.

-- exact numeric

--#[cast-00]
SELECT CAST('1' AS INT) FROM T;

--#[cast-01]
SELECT CAST('1' AS INT4) FROM T;

--#[cast-02]
SELECT CAST('1' AS INT8) FROM T;

--#[cast-03]
SELECT CAST('1' AS BIGINT) FROM T;

-- approximate numeric

-- #[cast-04]
-- SELECT CAST(1 AS REAL) FROM T;

--#[cast-05]
SELECT CAST(1 AS DOUBLE PRECISION) FROM T;
