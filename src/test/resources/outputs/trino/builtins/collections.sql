--#[collections-01]
SELECT cardinality("t"."array") AS "v" FROM "default"."T" AS "t";

--#[collections-02]
SELECT cardinality("t"."array") AS "v" FROM "default"."T" AS "t";

--#[collections-03]
SELECT cardinality("t"."array") > 0 AS "v" FROM "default"."T" AS "t";
