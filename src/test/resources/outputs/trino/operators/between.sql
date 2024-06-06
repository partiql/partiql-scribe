--#[between-00]
-- between(decimal, int32, int32)
SELECT "T_DECIMALS"."da" AS "da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN -1 AND 1;

--#[between-01]
-- between(decimal, int64, int64)
SELECT "T_DECIMALS"."da" AS "da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN -2147483649 AND 2147483648;

--#[between-02]
-- between(decimal, decimal, decimal)
SELECT "T_DECIMALS"."da" AS "da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN -CAST('9223372036854775809' AS DECIMAL(38,0)) AND CAST('9223372036854775808' AS DECIMAL(38,0));

--#[between-04]
-- between(decimal(p,s), int32, int32)
SELECT "T_DECIMALS"."de" AS "de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."de" BETWEEN -1 AND 1;

--#[between-05]
-- between(decimal(p,s), int64, int64)
SELECT "T_DECIMALS"."de" AS "de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."de" BETWEEN -2147483649 AND 2147483648;

--#[between-06]
-- between(decimal(p,s), decimal, decimal)
SELECT "T_DECIMALS"."de" AS "de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."de" BETWEEN -CAST('9223372036854775809' AS DECIMAL(38,0)) AND CAST('9223372036854775808' AS DECIMAL(38,0));
