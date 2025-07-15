--#[with-00]
-- SELECT *
WITH "cte1" AS (SELECT "SIMPLE_T"."a", "SIMPLE_T"."b", "SIMPLE_T"."c" FROM "default"."SIMPLE_T" AS "SIMPLE_T") SELECT "cte1"."a", "cte1"."b", "cte1"."c" FROM "cte1" AS "cte1";

--#[with-01]
-- SELECT * and alias for with list element
WITH "cte1" AS (SELECT "wle1"."a", "wle1"."b", "wle1"."c" FROM "default"."SIMPLE_T" AS "wle1") SELECT "cte1"."a", "cte1"."b", "cte1"."c" FROM "cte1" AS "cte1";

--#[with-02]
-- SELECT list and alias for with list element
WITH "cte1" AS (SELECT "wle1"."a" FROM "default"."SIMPLE_T" AS "wle1") SELECT "cte1"."a" FROM "cte1" AS "cte1";
