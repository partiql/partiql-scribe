--#[literal-00]
SELECT 0 AS "_1" FROM "default"."T" AS "T";

--#[literal-01]
SELECT 1 AS "_1" FROM "default"."T" AS "T";

--#[literal-02]
SELECT -1 AS "_1" FROM "default"."T" AS "T";

--#[literal-03]
SELECT 9223372036854775807 AS "_1" FROM "default"."T" AS "T";

--#[literal-04]
SELECT CAST('9223372036854775808' AS DECIMAL(38,0)) AS "_1" FROM "default"."T" AS "T";

--#[literal-05]
SELECT -CAST('9223372036854775808' AS DECIMAL(38,0)) AS "_1" FROM "default"."T" AS "T";

--#[literal-06]
SELECT -CAST('9223372036854775809' AS DECIMAL(38,0)) AS "_1" FROM "default"."T" AS "T";
