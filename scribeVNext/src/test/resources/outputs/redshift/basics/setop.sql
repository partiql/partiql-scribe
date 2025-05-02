-- SQL set ops

-- SQL UNION
--#[setop-00]
(SELECT "t1"."a" FROM "default"."SIMPLE_T" AS "t1") UNION DISTINCT (SELECT "t2"."a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-01]
(SELECT "t1"."a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-02]
((SELECT "t1"."a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a" FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3"."a" FROM "default"."SIMPLE_T" AS "t3");

--#[setop-03]
(SELECT "t1"."a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"."a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"."a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-04]
(SELECT "t1"."c", "t1"."b", "t1"."a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"."c", "t2"."b", "t2"."a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"."c", "t3"."b", "t3"."a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-05]
((SELECT "t1"."a", "t1"."b", "t1"."c" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"."a", "t2"."b", "t2"."c" FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3"."a", "t3"."b", "t3"."c" FROM "default"."SIMPLE_T" AS "t3");
