-- Basic window functions with OVER clause
--#[window-01]
SELECT ROW_NUMBER() OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-02]
SELECT RANK() OVER (ORDER BY "T"['a'] DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

--#[window-03]
SELECT DENSE_RANK() OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window functions with PARTITION BY
--#[window-04]
SELECT "T"['a'] AS "a", ROW_NUMBER() OVER (PARTITION BY "T"."b" ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-05]
SELECT "T"['a'] AS "a", RANK() OVER (PARTITION BY "T"."b" ORDER BY "T"['a'] DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

--#[window-06]
SELECT "T"['a'] AS "a", DENSE_RANK() OVER (PARTITION BY "T"."b" ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- LAG and LEAD functions
--#[window-07]
SELECT "T"['a'] AS "a", LAG("T"['a'], 1, NULL) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-08]
SELECT "T"['a'] AS "a", LEAD("T"['a'], 1, NULL) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-09]
SELECT "T"['a'] AS "a", LAG("T"['a'], 2, NULL) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-10]
SELECT "T"['a'] AS "a", LEAD("T"['a'], 2, false) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window functions with NULLS handling
-- #[window-11]
-- PError{code=FEATURE_NOT_SUPPORTED, severity=ERROR, kind=SEMANTIC, location=null, properties={FEATURE_NAME=IGNORE NULLS}}
-- SELECT "T"['a'] AS "a", LAG("T"['a'], 1, NULL) IGNORE NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-12]
SELECT "T"['a'] AS "a", LAG("T"['a'], 1, NULL) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- #[window-13]
-- PError{code=FEATURE_NOT_SUPPORTED, severity=ERROR, kind=SEMANTIC, location=null, properties={FEATURE_NAME=IGNORE NULLS}}
-- SELECT "T"['a'] AS "a", LEAD("T"['a'], 1, NULL) IGNORE NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-14]
SELECT "T"['a'] AS "a", LEAD("T"['a'], 1, NULL) RESPECT NULLS OVER (ORDER BY "T"['a'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- Window clause definitions
--#[window-15]
SELECT "T"['a'] AS "a", ROW_NUMBER() OVER "w" AS "_1" FROM "default"."T" AS "T" WINDOW "w" AS (ORDER BY "T"['a'] ASC NULLS LAST);

--#[window-16]
SELECT "T"['a'] AS "a", RANK() OVER "w1" AS "_1", DENSE_RANK() OVER "w2" AS "_2" FROM "default"."T" AS "T" WINDOW "w1" AS (ORDER BY "T"['a'] ASC NULLS LAST), "w2" AS (ORDER BY "T"['b'] DESC NULLS FIRST);

--#[window-17]
SELECT "T"['a'] AS "a", ROW_NUMBER() OVER "w" AS "_1" FROM "default"."T" AS "T" WINDOW "w" AS (PARTITION BY "T"."b" ORDER BY "T"['a'] ASC NULLS LAST);

-- Multiple window functions with named windows
--#[window-18]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", ROW_NUMBER() OVER "w1" AS "rn1", RANK() OVER "w1" AS "rank1", LAG("T"['a'], 1, NULL) RESPECT NULLS OVER "w1" AS "lag1" FROM "default"."T" AS "T" WINDOW "w1" AS (PARTITION BY "T"."b" ORDER BY "T"['a'] ASC NULLS LAST);

-- Complex window with multiple partitions and orders
--#[window-19]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", "T"['c'] AS "c", ROW_NUMBER() OVER "w1" AS "rn", RANK() OVER "w2" AS "rank_desc" FROM "default"."T" AS "T" WINDOW "w1" AS (PARTITION BY "T"."a" ORDER BY "T"['b'] ASC NULLS LAST, "T"['c'] ASC NULLS LAST), "w2" AS (PARTITION BY "T"."a" ORDER BY "T"['b'] DESC NULLS FIRST, "T"['c'] DESC NULLS FIRST);

-- Multiple ORDER BY columns
--#[window-20]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "T"['a'] ASC NULLS LAST, "T"['b'] ASC NULLS LAST, "T"['c'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

--#[window-21]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", RANK() OVER (PARTITION BY "T"."a" ORDER BY "T"['b'] ASC NULLS LAST, "T"['c'] DESC NULLS FIRST) AS "_1" FROM "default"."T" AS "T";

-- Complex comprehensive example
--#[window-22]
SELECT "t"['a'] AS "_id", "t"['b'] AS "_name", RANK() OVER "_w1" AS "_rank_1", RANK() OVER "_w2" AS "_rank_2", DENSE_RANK() OVER "_w1" AS "_dense_rank_1", DENSE_RANK() OVER "_w2" AS "_dense_rank_2", ROW_NUMBER() OVER "_w1" AS "_row_number_1", ROW_NUMBER() OVER "_w2" AS "_row_number_2", LAG("t"['b'], 1, -1) RESPECT NULLS OVER "_w1" AS "_lag_1", LAG("t"['b'], 1, -1) RESPECT NULLS OVER "_w2" AS "_lag_2", LEAD("t"['b'], 1, -1) RESPECT NULLS OVER "_w1" AS "_lead_1", LEAD("t"['b'], 1, -1) RESPECT NULLS OVER "_w2" AS "_lead_2" FROM "default"."T" AS "t" WINDOW "_w1" AS (PARTITION BY "t"."a" ORDER BY "t"['b'] ASC NULLS LAST, "t"['c'] ASC NULLS LAST), "_w2" AS (PARTITION BY "t"."a" ORDER BY "t"['b'] DESC NULLS FIRST, "t"['c'] DESC NULLS FIRST);
