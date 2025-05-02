
--#[trim-00]
SELECT TRIM(BOTH FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-01]
SELECT TRIM(BOTH FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-02]
SELECT TRIM(LEADING FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-03]
SELECT TRIM(TRAILING FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";
