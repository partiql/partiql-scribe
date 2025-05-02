-- SQL set ops

-- SQL UNION
--#[setop-00]
(SELECT "t1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION DISTINCT (SELECT "t2"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-01]
(SELECT "t1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t2");

--#[setop-02]
((SELECT "t1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t3");

--#[setop-03]
(SELECT "t1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-04]
(SELECT "t1"['c'] AS "c", "t1"['b'] AS "b", "t1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t1") UNION ALL ((SELECT "t2"['c'] AS "c", "t2"['b'] AS "b", "t2"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t2") UNION DISTINCT (SELECT "t3"['c'] AS "c", "t3"['b'] AS "b", "t3"['a'] AS "a" FROM "default"."SIMPLE_T" AS "t3"));

--#[setop-05]
((SELECT "t1".* FROM "default"."SIMPLE_T" AS "t1") UNION ALL (SELECT "t2".* FROM "default"."SIMPLE_T" AS "t2")) UNION DISTINCT (SELECT "t3".* FROM "default"."SIMPLE_T" AS "t3");
