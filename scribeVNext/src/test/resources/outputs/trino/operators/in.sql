--#[in-00]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-01]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-02]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-03]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-04]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-05]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c", "T"."d" AS "d", "T"."x" AS "x", "T"."array" AS "array", "T"."z" AS "z", "T"."v" AS "v", "T"."timestamp_1" AS "timestamp_1", "T"."timestamp_2" AS "timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);
