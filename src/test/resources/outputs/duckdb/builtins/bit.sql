--#[bitwise-and-00]
SELECT "T"."b" & "T"."b" AS "_1" FROM "default"."T" AS "T";

--#[bitwise-and-01]
SELECT "T"."b" & 3 AS "_1" FROM "default"."T" AS "T";
