-- Basic window functions with OVER clause
--#[window-01]
SELECT ROW_NUMBER() OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-02]
SELECT RANK() OVER (ORDER BY "T"."a" DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

--#[window-03]
SELECT DENSE_RANK() OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window functions with PARTITION BY
--#[window-04]
SELECT "T"."a", ROW_NUMBER() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-05]
SELECT "T"."a", RANK() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

--#[window-06]
SELECT "T"."a", DENSE_RANK() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- LAG and LEAD functions
--#[window-07]
SELECT "T"."a", LAG("T"."a", 1) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-08]
SELECT "T"."a", LEAD("T"."a", 1) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-09]
SELECT "T"."a", LAG("T"."a", 2) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-10]
SELECT "T"."a", LEAD("T"."a", 2) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window functions with NULLS handling
-- #[window-11]
-- PError{code=FEATURE_NOT_SUPPORTED, severity=ERROR, kind=SEMANTIC, location=null, properties={FEATURE_NAME=IGNORE NULLS}}
-- SELECT "T"."a", LAG("T"."a", 1, NULL) IGNORE NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-12]
SELECT "T"."a", LAG("T"."a", 1) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- #[window-13]
-- PError{code=FEATURE_NOT_SUPPORTED, severity=ERROR, kind=SEMANTIC, location=null, properties={FEATURE_NAME=IGNORE NULLS}}
-- SELECT "T"."a", LEAD("T"."a", 1) IGNORE NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-14]
SELECT "T"."a", LEAD("T"."a", 1) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window clause definitions (inline window references for Redshift)
--#[window-15]
SELECT "T"."a", ROW_NUMBER() OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-16]
SELECT "T"."a", RANK() OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1", DENSE_RANK() OVER (ORDER BY "T"."b" DESC NULLS FIRST) AS "_2" FROM "default"."T" AS "T";

--#[window-17]
SELECT "T"."a", ROW_NUMBER() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Multiple window functions with inline window references
--#[window-18]
SELECT "T"."a", "T"."b", ROW_NUMBER() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "rn1", RANK() OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "rank1", LAG("T"."a", 1) RESPECT NULLS OVER (PARTITION BY "T"."b" ORDER BY "T"."a" ASC NULLS LAST) AS "lag1" FROM "default"."T" AS "T";

-- Complex window with multiple partitions and orders (inline)
--#[window-19]
SELECT "T"."a", "T"."b", "T"."c", ROW_NUMBER() OVER (PARTITION BY "T"."a" ORDER BY "T"."b" ASC NULLS LAST, "T"."c" ASC NULLS LAST) AS "rn", RANK() OVER (PARTITION BY "T"."a" ORDER BY "T"."b" DESC NULLS FIRST, "T"."c" DESC NULLS FIRST) AS "rank_desc" FROM "default"."T" AS "T";

-- Multiple ORDER BY columns
--#[window-20]
SELECT "T"."a", "T"."b", ROW_NUMBER() OVER (ORDER BY "T"."a" ASC NULLS LAST, "T"."b" ASC NULLS LAST, "T"."c" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-21]
SELECT "T"."a", "T"."b", RANK() OVER (PARTITION BY "T"."a" ORDER BY "T"."b" ASC NULLS LAST, "T"."c" DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

-- Complex comprehensive example (inline window references)
--#[window-22]
SELECT "t"."a" AS "_id", "t"."b" AS "_name", RANK() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" ASC NULLS LAST, "t"."c" ASC NULLS LAST) AS "_rank_1", RANK() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" DESC NULLS FIRST, "t"."c" DESC NULLS FIRST) AS "_rank_2", DENSE_RANK() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" ASC NULLS LAST, "t"."c" ASC NULLS LAST) AS "_dense_rank_1", DENSE_RANK() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" DESC NULLS FIRST, "t"."c" DESC NULLS FIRST) AS "_dense_rank_2", ROW_NUMBER() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" ASC NULLS LAST, "t"."c" ASC NULLS LAST) AS "_row_number_1", ROW_NUMBER() OVER (PARTITION BY "t"."a" ORDER BY "t"."b" DESC NULLS FIRST, "t"."c" DESC NULLS FIRST) AS "_row_number_2", LAG("t"."b", 1) RESPECT NULLS OVER (PARTITION BY "t"."a" ORDER BY "t"."b" ASC NULLS LAST, "t"."c" ASC NULLS LAST) AS "_lag_1", LAG("t"."b", 1) RESPECT NULLS OVER (PARTITION BY "t"."a" ORDER BY "t"."b" DESC NULLS FIRST, "t"."c" DESC NULLS FIRST) AS "_lag_2", LEAD("t"."b", 1) RESPECT NULLS OVER (PARTITION BY "t"."a" ORDER BY "t"."b" ASC NULLS LAST, "t"."c" ASC NULLS LAST) AS "_lead_1", LEAD("t"."b", 1) RESPECT NULLS OVER (PARTITION BY "t"."a" ORDER BY "t"."b" DESC NULLS FIRST, "t"."c" DESC NULLS FIRST) AS "_lead_2" FROM "default"."T" AS "t";

--#[window-34]
SELECT "T"."a", count(1) AS "_1", ROW_NUMBER() OVER (ORDER BY count(1) ASC NULLS LAST) AS "_2" FROM "default"."T" AS "T" GROUP BY "T"."a";

-- SUM over a partition
--#[window-23]
SELECT "T"."a", SUM("T"."b") OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- AVG over a partition with ORDER BY. Not supported: Redshift needs a ROWS frame with ORDER BY, which cannot express the default frame
--#[window-24]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Unsupported aggregate window function AVG: Redshift requires a ROWS frame with ORDER BY, which cannot express the default frame. Remove ORDER BY from the OVER clause or use another target"}];

-- COUNT(*) over a partition
--#[window-25]
SELECT "T"."a", COUNT(1) OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- MAX with PARTITION BY and ORDER BY. Not supported: Redshift needs a ROWS frame with ORDER BY, which cannot express the default frame
--#[window-35]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Unsupported aggregate window function MAX: Redshift requires a ROWS frame with ORDER BY, which cannot express the default frame. Remove ORDER BY from the OVER clause or use another target"}];

-- MIN over a named window with ORDER BY. Not supported: Redshift needs a ROWS frame with ORDER BY, which cannot express the default frame
--#[window-36]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Unsupported aggregate window function MIN: Redshift requires a ROWS frame with ORDER BY, which cannot express the default frame. Remove ORDER BY from the OVER clause or use another target"}];

-- COUNT(DISTINCT) over a partition. Not supported: Redshift does not support DISTINCT in aggregate window functions
--#[window-37]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Unsupported aggregate window function COUNT: Redshift does not support DISTINCT in aggregate window functions. Remove DISTINCT or use another target"}];

-- SUM over the whole input (empty OVER)
--#[window-38]
SELECT "T"."a", SUM("T"."b") OVER () AS "_1" FROM "default"."T" AS "T";

-- Two aggregate window functions in one query. Not supported: Redshift needs a ROWS frame with ORDER BY, which cannot express the default frame
--#[window-39]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Unsupported aggregate window function MAX: Redshift requires a ROWS frame with ORDER BY, which cannot express the default frame. Remove ORDER BY from the OVER clause or use another target"}];

-- Aggregate window function over a GROUP BY aggregate
--#[window-40]
SELECT "T"."a", SUM(count(1)) OVER () AS "_1" FROM "default"."T" AS "T" GROUP BY "T"."a";

-- Window in a correlated derived table under a scalar subquery. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-42]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Window in an uncorrelated scalar subquery
--#[window-43]
SELECT "o"."a", (SELECT MAX("t"."b") OVER () AS "m" FROM "default"."T" AS "t" LIMIT 1) AS "m" FROM "default"."T" AS "o";

-- Window in a correlated IN subquery. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-44]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Window in a correlated EXISTS subquery. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-45]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Window in a correlated lateral derived table. Not supported: Redshift does not support lateral correlated subqueries
--#[window-46]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Redshift does not support lateral correlated subqueries"}];

-- ROW_NUMBER in a correlated subquery. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-47]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Window in a subquery over a CTE of the outer query (not a correlation)
--#[window-48]
WITH "w" AS (SELECT "t"."b", "t"."c" FROM "default"."T" AS "t") SELECT "o"."a", (SELECT MAX("x"."b") OVER () AS "m" FROM "w" AS "x" LIMIT 1) AS "m" FROM "default"."T" AS "o";

-- Correlation inside a CASE in a subquery with a window. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-49]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Correlation in a subquery body with its own WITH clause. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-50]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Aggregate over a named window without ORDER BY
--#[window-51]
SELECT "T"."a", MIN("T"."b") OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- Subquery over an outer CTE that is also correlated. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-52]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Window in a correlated NOT EXISTS subquery. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-53]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Correlated EXISTS and correlated scalar subqueries with windows in one query. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-54]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Correlation in the select list, outside the window function. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-55]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Correlation in a subquery nested in the subquery with the window. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-56]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];

-- Correlated subquery whose window is only in an uncorrelated derived table
--#[window-57]
SELECT "o"."a", (SELECT max("s"."m") AS "m" FROM (SELECT MAX("t"."b") OVER () AS "m" FROM "default"."T" AS "t") AS "s" WHERE "s"."m" > "o"."b") AS "m" FROM "default"."T" AS "o";

-- Correlation in a path index. Not supported: Redshift does not support a correlated subquery with a window function
--#[window-58]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="Redshift does not support correlation references from within a subquery that contains a window function"}];
