--#[limit-offset-00]
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" LIMIT 1;

--#[limit-offset-01]
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 1;

--#[limit-offset-02]
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 1 LIMIT 1;

--#[limit-offset-03]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-04]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-05]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-06]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-07]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-08]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-09]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-10]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-11]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

--#[limit-offset-12]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-13]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-14]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-15]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-16]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-17]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-18]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-19]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

--#[limit-offset-20]
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;
