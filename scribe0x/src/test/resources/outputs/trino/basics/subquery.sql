-- IN collection subquery
--#[subquery-02]
SELECT "upper"("T"."v") AS "_1" FROM "default"."T" AS "T"
WHERE "T"."b" IN (SELECT "T"."b" AS "b" FROM "default"."T" AS "T" WHERE "T"."a");
