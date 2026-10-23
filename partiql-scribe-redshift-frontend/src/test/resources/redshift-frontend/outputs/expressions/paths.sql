-- --#[attribute-00] TODO(redshift)
SELECT COALESCE("t"['x']['y']) AS "v" FROM "default"."T" AS "t";

-- --#[attribute-01] TODO(redshift)
SELECT COALESCE("t"['x']['foo']) AS "v" FROM "default"."T" AS "t";

-- --#[attribute-02] TODO(redshift)
SELECT COALESCE("t"['x']['y']) AS "y" FROM "default"."T" AS "t";

-- --#[attribute-03] TODO(redshift)
SELECT COALESCE("t"['x']['Äb']) AS "v" FROM "default"."T" AS "t";

-- --#[attribute-04] TODO(redshift)
SELECT "t"['d']['e'] AS "v" FROM "default"."T" AS "t";

-- --#[attribute-05] TODO(redshift)
SELECT COALESCE("t"['x']['y']) AS "v1", COALESCE(COALESCE("t"['x'], "t"['x'])['y']) AS "v2" FROM "default"."T" AS "t";

-- --#[attribute-06] TODO(redshift)
SELECT COALESCE("s"['y']['z']) AS "v" FROM (SELECT "t"['x'] AS "y" FROM "default"."T" AS "t") AS "s";

-- --#[index-00] TODO(redshift)
SELECT COALESCE("t"['x'][0]['y']) AS "v1", COALESCE("t"['array']["t"['b'] + 1]) AS "v2" FROM "default"."T" AS "t";

-- --#[index-01] TODO(redshift)
[UNSUPPORTED{message="bracket navigation with a string key"}];
