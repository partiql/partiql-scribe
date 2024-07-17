
--#[trim-00]
SELECT TRIM(BOTH "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-01]
SELECT TRIM(BOTH "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-02]
SELECT TRIM(LEADING "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-03]
SELECT TRIM(TRAILING "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM "T"['c']) AS "_1" FROM "default"."T" AS "T";
