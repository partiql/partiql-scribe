--#[cast-00]
SELECT CAST('1' AS INT) AS "_1" FROM "default"."T" AS "T";

--#[cast-01]
SELECT CAST('1' AS INT) AS "_1" FROM "default"."T" AS "T";

--#[cast-02]
SELECT CAST('1' AS BIGINT) AS "_1" FROM "default"."T" AS "T";

--#[cast-03]
SELECT CAST('1' AS BIGINT) AS "_1" FROM "default"."T" AS "T";

-- #[cast-04]
-- SELECT CAST(1 AS REAL) AS "_1" FROM "default"."T" AS "T";

--#[cast-05]
SELECT CAST(1 AS DOUBLE PRECISION) AS "_1" FROM "default"."T" AS "T";
