--#[limit-offset-00]
SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1;

--#[limit-offset-01]
SELECT "T"."a" FROM "default"."T" AS "T" OFFSET 1;

--#[limit-offset-02]
SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 1;

--#[limit-offset-03]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-04]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-05]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-06]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-07]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-08]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-09]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-10]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-11]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4);

--#[limit-offset-12]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-13]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-14]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-15]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-16]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-17]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT DISTINCT (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-18]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) UNION ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-19]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) EXCEPT ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;

--#[limit-offset-20]
(SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 1 OFFSET 2) INTERSECT ALL (SELECT "T"."a" FROM "default"."T" AS "T" LIMIT 3 OFFSET 4) LIMIT 5 OFFSET 6;
