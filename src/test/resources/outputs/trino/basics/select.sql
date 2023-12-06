
--#[select-10]
SELECT "T".c || current_user AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT current_user AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT current_date AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[select-13]
SELECT date_diff('day', current_date, current_date) AS "_1" FROM "default"."T" AS "T";

--#[select-14]
SELECT date_add('day', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[select-15]
SELECT date_add('day', -5, current_date) AS "_1" FROM "default"."T" AS "T";
