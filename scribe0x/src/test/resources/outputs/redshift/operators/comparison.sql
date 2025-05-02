--#[eq-00]
SELECT "T"."a" = true AS "_1" FROM "default"."T" AS "T";

--#[eq-01]
SELECT "T"."b" = 1 AS "_1" FROM "default"."T" AS "T";

--#[eq-02]
SELECT "T"."c" = 'a' AS "_1" FROM "default"."T" AS "T";

--#[eq-03]
SELECT "T"."c" = "T"."z" AS "_1" FROM "default"."T" AS "T";

--#[eq-04]
SELECT "T"."c" = "T"."d"."e" AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[eq-05]
SELECT "T"."c" = "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[neq-00]
SELECT "T"."a" <> true AS "_1" FROM "default"."T" AS "T";

--#[neq-01]
SELECT "T"."b" <> 1 AS "_1" FROM "default"."T" AS "T";

--#[neq-02]
SELECT "T"."c" <> 'a' AS "_1" FROM "default"."T" AS "T";

--#[neq-03]
SELECT "T"."c" <> "T"."z" AS "_1" FROM "default"."T" AS "T";

--#[neq-04]
SELECT "T"."c" <> "T"."d"."e" AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[neq-05]
SELECT "T"."c" <> "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[lt-00]
SELECT "T"."b" < 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[lt-01]
SELECT "T"."b" < "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[lte-00]
SELECT "T"."b" <= 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[lte-01]
SELECT "T"."b" <= "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[gt-00]
SELECT "T"."b" > 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[gt-01]
SELECT "T"."b" > "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[gte-00]
SELECT "T"."b" >= 1 AS "_1" FROM "default"."T" AS "T";

-- DISABLED !! "T.".x" is type ANY which is not supported by Redshift
-- #[gte-01]
-- SELECT "T"."b" >= "T"."x" AS "_1" FROM "default"."T" AS "T";

--#[not-00]
SELECT NOT ("T"."a") AS "_1" FROM "default"."T" AS "T";

--#[not-01]
SELECT NOT (NOT ("T"."a")) AS "_1" FROM "default"."T" AS "T";
