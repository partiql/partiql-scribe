--#[between-00]
-- between(decimal, int32, int32)
SELECT "T_DECIMALS"."da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN CAST(-1 AS DECIMAL) AND CAST(1 AS DECIMAL);

--#[between-01]
-- between(decimal, int64, int64)
SELECT "T_DECIMALS"."da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN CAST(-2147483649 AS DECIMAL) AND CAST(2147483648 AS DECIMAL);

--#[between-02]
-- between(decimal, decimal, decimal)
SELECT "T_DECIMALS"."da" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE "T_DECIMALS"."da" BETWEEN CAST(-9223372036854775809 AS DECIMAL) AND CAST(9223372036854775808 AS DECIMAL);

--#[between-04]
-- between(decimal(p,s), int32, int32)
SELECT "T_DECIMALS"."de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE CAST("T_DECIMALS"."de" AS DECIMAL) BETWEEN CAST(-1 AS DECIMAL) AND CAST(1 AS DECIMAL);

--#[between-05]
-- between(decimal(p,s), int64, int64)
SELECT "T_DECIMALS"."de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE CAST("T_DECIMALS"."de" AS DECIMAL) BETWEEN CAST(-2147483649 AS DECIMAL) AND CAST(2147483648 AS DECIMAL);

--#[between-06]
-- between(decimal(p,s), decimal, decimal)
SELECT "T_DECIMALS"."de" FROM "default"."T_DECIMALS" AS "T_DECIMALS" WHERE CAST("T_DECIMALS"."de" AS DECIMAL) BETWEEN CAST(-9223372036854775809 AS DECIMAL) AND CAST(9223372036854775808 AS DECIMAL);
