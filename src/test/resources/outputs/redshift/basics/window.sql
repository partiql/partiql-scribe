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
-- ERROR: Redshift doesn't support IGNORE NULLS
-- SELECT "T"."a", LAG("T"."a", 1, NULL) IGNORE NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-12]
SELECT "T"."a", LAG("T"."a", 1) RESPECT NULLS OVER (ORDER BY "T"."a" ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- #[window-13]
-- ERROR: Redshift doesn't support IGNORE NULLS
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
