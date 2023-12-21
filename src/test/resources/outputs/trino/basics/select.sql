
--#[select-10]
SELECT "T".c || current_user AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT current_user AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT current_date AS "CURRENT_DATE" FROM "default"."T" AS "T";
