--#[collections-01]
SELECT "size"("t"['array']) AS "v" FROM "default"."T" AS "t";

--#[collections-02]
SELECT "cardinality"("t"['array']) AS "v" FROM "default"."T" AS "t";

--#[collections-03]
SELECT "exists"("t"['array']) AS "v" FROM "default"."T" AS "t";
