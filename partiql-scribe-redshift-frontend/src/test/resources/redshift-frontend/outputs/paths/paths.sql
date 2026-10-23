-- --#[path-00] TODO(redshift)
SELECT COALESCE("t"['x']['y']) AS "v" FROM "default"."T" AS "t";

-- --#[path-01] TODO(redshift)
SELECT COALESCE("t"['x'][0]['y']) AS "v" FROM "default"."T" AS "t";

-- --#[path-02] TODO(redshift)
SELECT "t"['d']['e'] AS "v" FROM "default"."T" AS "t";

-- --#[path-03] TODO(redshift)
SELECT COALESCE("t"['array']["t"['b'] + 1]) AS "v" FROM "default"."T" AS "t";
