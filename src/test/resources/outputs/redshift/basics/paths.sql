-- ----------------------------------------
--  Redshift Path Navigation (+SFW)
--  https://docs.aws.amazon.com/redshift/latest/dg/query-super.html#navigation
-- ----------------------------------------

--#[paths-sfw-00]
-- dot notation
SELECT "t"."x".y AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-01]
-- bracket notation
SELECT "t"."x"[0] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-03]
SELECT "t"."x"."y" AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-06]
SELECT "t"."x".y[0]."y" AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-24]
SELECT "t"."array"["t"."b"] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-25]
SELECT "t"."array"["t"."b" + 1] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-26]
SELECT "t"."array"[-1] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-27]
SELECT "t"."array"[9223372036854775807] AS "v" FROM "default"."T" AS "t";
