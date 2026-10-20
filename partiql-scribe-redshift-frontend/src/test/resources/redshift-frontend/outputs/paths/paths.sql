--#[path-00]
SELECT "t"['x']['y'] AS "v" FROM "default"."T" AS "t";

--#[path-01]
SELECT "t"['x'][0]['y'] AS "v" FROM "default"."T" AS "t";

--#[path-02]
SELECT "t"['d']['e'] AS "v" FROM "default"."T" AS "t";

--#[path-03]
SELECT "t"['array']["t"['b'] + 1] AS "v" FROM "default"."T" AS "t";
