--#[limit-offset-00]
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" LIMIT 1;

--#[limit-offset-01]
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 1;

-- --#[limit-offset-02] TODO(duckdb)
SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 1 LIMIT 1;

-- --#[limit-offset-03] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-04] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-05] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-06] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-07] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-08] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-09] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-10] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-11] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3);

-- --#[limit-offset-12] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-13] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-14] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-15] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-16] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-17] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT DISTINCT (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-18] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) UNION ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-19] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) EXCEPT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;

-- --#[limit-offset-20] TODO(duckdb)
(SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 2 LIMIT 1) INTERSECT ALL (SELECT "T"."a" AS "a" FROM "default"."T" AS "T" OFFSET 4 LIMIT 3) OFFSET 6 LIMIT 5;
