--#[path-00]
SELECT COALESCE("t"['x']['y']) AS "v" FROM "default"."T" AS "t";

--#[path-01]
SELECT COALESCE("t"['x'][0]['y']) AS "v" FROM "default"."T" AS "t";

--#[path-02]
SELECT "t"['d']['e'] AS "v" FROM "default"."T" AS "t";

--#[path-03]
SELECT COALESCE("t"['array']["t"['b'] + 1]) AS "v" FROM "default"."T" AS "t";
