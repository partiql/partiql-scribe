--#[with-recursive-00]
-- Counting with UNION ALL
WITH RECURSIVE "cnt" ("n") AS ((SELECT 1 AS "n" FROM "default"."SIMPLE_T" AS "t" WHERE "t"."b" = 0) UNION ALL (SELECT "cnt"."n" + 1 AS "n" FROM "cnt" AS "cnt" WHERE "cnt"."n" < 5)) SELECT "cnt"."n" AS "n" FROM "cnt" AS "cnt";

--#[with-recursive-01]
-- Hierarchy traversal with a join
WITH RECURSIVE "reports" ("id", "name") AS ((SELECT "e"."id" AS "id", "e"."name" AS "name" FROM "default"."EMPLOYEE_T" AS "e" WHERE "e"."manager_id" IS NULL) UNION ALL (SELECT "e"."id" AS "id", "e"."name" AS "name" FROM "default"."EMPLOYEE_T" AS "e" INNER JOIN "reports" AS "r" ON "e"."manager_id" = "r"."id")) SELECT "r"."id" AS "id", "r"."name" AS "name" FROM "reports" AS "r";

--#[with-recursive-02]
-- UNION (DISTINCT) and a column list renaming the columns of each operand
WITH RECURSIVE "managers" ("x") AS ((SELECT "e"."id" AS "id" FROM "default"."EMPLOYEE_T" AS "e" WHERE "e"."id" = 5) UNION DISTINCT (SELECT "e"."manager_id" AS "manager_id" FROM "default"."EMPLOYEE_T" AS "e" INNER JOIN "managers" AS "m" ON true WHERE "e"."id" = "m"."x")) SELECT "m"."x" AS "x" FROM "managers" AS "m";

--#[with-recursive-03]
-- A recursive element seeded from a non-recursive element that is defined later
WITH RECURSIVE "roots" ("id") AS (SELECT "e"."id" AS "id" FROM "default"."EMPLOYEE_T" AS "e" WHERE "e"."manager_id" IS NULL), "tree" ("id", "depth") AS ((SELECT "b"."id" AS "id", 0 AS "_1" FROM "roots" AS "b") UNION ALL (SELECT "e"."id" AS "id", "t"."depth" + 1 AS "_1" FROM "default"."EMPLOYEE_T" AS "e" INNER JOIN "tree" AS "t" ON "e"."manager_id" = "t"."id")) SELECT "t"."id" AS "id", "t"."depth" AS "depth" FROM "tree" AS "t";

--#[with-recursive-04]
-- A recursive element referenced twice in the query
WITH RECURSIVE "cnt" ("n") AS ((SELECT 1 AS "_1" FROM "default"."SIMPLE_T" AS "t" WHERE "t"."b" = 0) UNION ALL (SELECT "cnt"."n" + 1 AS "_1" FROM "cnt" AS "cnt" WHERE "cnt"."n" < 3)) SELECT "c1"."n" AS "n1", "c2"."n" AS "n2" FROM "cnt" AS "c1" INNER JOIN "cnt" AS "c2" ON "c1"."n" = "c2"."n";
