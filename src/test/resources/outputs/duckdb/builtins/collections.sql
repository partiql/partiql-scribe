--#[collections-01]
SELECT len("t"."array") AS "v" FROM "default"."T" AS "t";

--#[collections-02]
SELECT len("t"."array") AS "v" FROM "default"."T" AS "t";

--#[collections-03]
SELECT len("t"."array") > 0 AS "v" FROM "default"."T" AS "t";
