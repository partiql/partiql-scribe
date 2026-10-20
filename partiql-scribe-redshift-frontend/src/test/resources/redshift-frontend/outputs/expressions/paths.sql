--#[attribute-00]
SELECT "t"['x']['y'] AS "v" FROM "default"."T" AS "t";

--#[attribute-01]
SELECT "t"['x']['foo'] AS "v" FROM "default"."T" AS "t";

--#[attribute-02]
SELECT "t"['x']['y'] AS "y" FROM "default"."T" AS "t";

--#[attribute-03]
SELECT "t"['x']['Äb'] AS "v" FROM "default"."T" AS "t";

--#[attribute-04]
SELECT "t"['d']['e'] AS "v" FROM "default"."T" AS "t";

--#[attribute-05]
SELECT "t"['x']['y'] AS "v1", COALESCE("t"['x'], "t"['x'])['y'] AS "v2" FROM "default"."T" AS "t";

--#[attribute-06]
SELECT "s"['y']['z'] AS "v" FROM (SELECT "t"['x'] AS "y" FROM "default"."T" AS "t") AS "s";

--#[index-00]
SELECT "t"['x'][0]['y'] AS "v1", "t"['array']["t"['b'] + 1] AS "v2" FROM "default"."T" AS "t";

--#[index-01]
[UNSUPPORTED{message="bracket navigation with a string key"}];
