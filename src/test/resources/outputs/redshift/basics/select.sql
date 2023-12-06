
--#[select-10]
SELECT "T".c || CURRENT_USER AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT CURRENT_USER AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[select-13]
SELECT DATEDIFF(DAY, CURRENT_DATE, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[select-14]
SELECT DATEADD(DAY, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[select-15]
SELECT DATEADD(DAY, -5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";
