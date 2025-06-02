--#[distinct-00]
SELECT DISTINCT "T"."a" FROM "default"."T" AS "T";

--#[distinct-01]
SELECT DISTINCT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T";

--#[distinct-02]
(SELECT DISTINCT "T"."a" FROM "default"."T" AS "T") UNION DISTINCT (SELECT DISTINCT "T"."a" FROM "default"."T" AS "T");

--#[distinct-03]
(SELECT DISTINCT "T"."a" FROM "default"."T" AS "T") UNION DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T");

--#[distinct-04]
(SELECT "T"."a" FROM "default"."T" AS "T") UNION DISTINCT (SELECT DISTINCT "T"."a" FROM "default"."T" AS "T");
