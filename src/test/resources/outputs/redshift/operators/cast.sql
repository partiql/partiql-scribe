--#[cast-00]
CAST('1' AS INT);

--#[cast-01]
CAST('1' AS INT);

--#[cast-02]
CAST('1' AS BIGINT);

--#[cast-03]
CAST('1' AS BIGINT);

--#[cast-07]
SELECT CAST(1 AS DECIMAL) AS "_1" FROM "default"."T" AS "T";

--#[cast-08]
SELECT CAST(1 AS DECIMAL) AS "_1" FROM "default"."T" AS "T";

--#[cast-09]
SELECT CAST(9223372036854775807 AS DECIMAL(38,0)) AS "_1" FROM "default"."T" AS "T";

--#[cast-10]
SELECT CAST(9223372036854775808 AS DECIMAL(38,0)) AS "_1" FROM "default"."T" AS "T";
