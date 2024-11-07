--#[in-00]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-01]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-02]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-03]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-04]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-05]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);
