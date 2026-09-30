--#[bitwise-and-00]
SELECT "bitwise_and"("T"."b", "T"."b") AS "_1" FROM "default"."T" AS "T";

--#[bitwise-and-01]
SELECT "bitwise_and"("T"."b", 3) AS "_1" FROM "default"."T" AS "T";
