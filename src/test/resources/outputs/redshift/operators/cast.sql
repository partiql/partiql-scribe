--#[cast-00]
CAST('1' AS INT4);

--#[cast-01]
SELECT CAST('foo' AS VARCHAR) AS "s" FROM "default"."T" AS "T";
