--#[with-00]
-- SELECT *
WITH "cte1" AS (SELECT "SIMPLE_T"['a'] AS "a", "SIMPLE_T"['b'] AS "b", "SIMPLE_T"['c'] AS "c" FROM "default"."SIMPLE_T" AS "SIMPLE_T") SELECT "cte1"['a'] AS "a", "cte1"['b'] AS "b", "cte1"['c'] AS "c" FROM "cte1" AS "cte1";

--#[with-01]
-- SELECT * and alias for with list element
WITH "cte1" AS (SELECT "wle1"['a'] AS "a", "wle1"['b'] AS "b", "wle1"['c'] AS "c" FROM "default"."SIMPLE_T" AS "wle1") SELECT "cte1"['a'] AS "a", "cte1"['b'] AS "b", "cte1"['c'] AS "c" FROM "cte1" AS "cte1";

--#[with-02]
-- SELECT list and alias for with list element
WITH "cte1" AS (SELECT "wle1"['a'] AS "a" FROM "default"."SIMPLE_T" AS "wle1") SELECT "cte1"['a'] AS "a" FROM "cte1" AS "cte1";
