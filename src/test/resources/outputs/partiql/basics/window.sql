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

--#[window-34]
SELECT "T"['a'] AS "a", count(1) AS "_1", ROW_NUMBER() OVER (ORDER BY count(1) ASC NULLS LAST) AS "_2" FROM "default"."T" AS "T" GROUP BY "T"['a'];

-- SUM over a partition
--#[window-23]
SELECT "T"['a'] AS "a", SUM("T"['b']) OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- AVG over a partition with ORDER BY
--#[window-24]
SELECT "T"['a'] AS "a", AVG("T"['b']) OVER (PARTITION BY "T"."a" ORDER BY "T"['c'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- COUNT(*) over a partition
--#[window-25]
SELECT "T"['a'] AS "a", COUNT(1) OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- MAX with PARTITION BY and ORDER BY
--#[window-35]
SELECT "T"['a'] AS "a", MAX("T"['b']) OVER (PARTITION BY "T"."a" ORDER BY "T"['c'] ASC NULLS LAST) AS "_1" FROM "default"."T" AS "T";

-- MIN over a named window with ORDER BY
--#[window-36]
SELECT "T"['a'] AS "a", MIN("T"['b']) OVER "w" AS "_1" FROM "default"."T" AS "T" WINDOW "w" AS (PARTITION BY "T"."a" ORDER BY "T"['c'] ASC NULLS LAST);

-- COUNT(DISTINCT) over a partition
--#[window-37]
SELECT "T"['a'] AS "a", COUNT(DISTINCT "T"['b']) OVER (PARTITION BY "T"."a") AS "_1" FROM "default"."T" AS "T";

-- SUM over the whole input (empty OVER)
--#[window-38]
SELECT "T"['a'] AS "a", SUM("T"['b']) OVER () AS "_1" FROM "default"."T" AS "T";

-- Two aggregate window functions in one query
--#[window-39]
SELECT "T"['a'] AS "a", COUNT("T"['b']) OVER (ORDER BY "T"['c'] ASC NULLS LAST) AS "_1", MAX("T"['b']) OVER (ORDER BY "T"['c'] ASC NULLS LAST) AS "_2" FROM "default"."T" AS "T";

-- Aggregate window function over a GROUP BY aggregate
--#[window-40]
SELECT "T"['a'] AS "a", SUM(count(1)) OVER () AS "_1" FROM "default"."T" AS "T" GROUP BY "T"['a'];

-- Window in a correlated derived table under a scalar subquery
--#[window-42]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']) AS "s") AS "m" FROM "default"."T" AS "o";

-- Window in an uncorrelated scalar subquery
--#[window-43]
SELECT "o"['a'] AS "a", (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" LIMIT 1) AS "m" FROM "default"."T" AS "o";

-- Window in a correlated IN subquery
--#[window-44]
SELECT "o"['a'] AS "a" FROM "default"."T" AS "o" WHERE "o"['b'] IN (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']);

-- Window in a correlated EXISTS subquery
--#[window-45]
SELECT "o"['a'] AS "a" FROM "default"."T" AS "o" WHERE EXISTS (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']);

-- Window in a correlated lateral derived table
--#[window-46]
SELECT "o"['a'] AS "a", "s"['m'] AS "m" FROM "default"."T" AS "o" INNER JOIN (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']) AS "s" ON true;

-- ROW_NUMBER in a correlated subquery
--#[window-47]
SELECT "o"['a'] AS "a", (SELECT max("s"['n']) AS "n" FROM (SELECT ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "n" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']) AS "s") AS "n" FROM "default"."T" AS "o";

-- Window in a subquery over a CTE of the outer query (not a correlation)
--#[window-48]
WITH "w" AS (SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t") SELECT "o"['a'] AS "a", (SELECT MAX("x"['b']) OVER () AS "m" FROM "w" AS "x" LIMIT 1) AS "m" FROM "default"."T" AS "o";

-- Correlation inside a CASE in a subquery with a window
--#[window-49]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE CASE WHEN "t"['c'] = "o"['c'] THEN true ELSE false END) AS "s") AS "m" FROM "default"."T" AS "o";

-- Correlation in a subquery body with its own WITH clause
--#[window-50]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (WITH "w" AS (SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t") SELECT MAX("x"['b']) OVER () AS "m" FROM "w" AS "x" WHERE "x"['c'] = "o"['c']) AS "s") AS "m" FROM "default"."T" AS "o";

-- Aggregate over a named window without ORDER BY
--#[window-51]
SELECT "T"['a'] AS "a", MIN("T"['b']) OVER "w" AS "_1" FROM "default"."T" AS "T" WINDOW "w" AS (PARTITION BY "T"."a");

-- Subquery over an outer CTE that is also correlated
--#[window-52]
WITH "w" AS (SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t") SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("x"['b']) OVER () AS "m" FROM "w" AS "x" WHERE "x"['c'] = "o"['c']) AS "s") AS "m" FROM "default"."T" AS "o";

-- Window in a correlated NOT EXISTS subquery
--#[window-53]
SELECT "o"['a'] AS "a" FROM "default"."T" AS "o" WHERE NOT (EXISTS (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']));

-- Correlated EXISTS and correlated scalar subqueries with windows in one query
--#[window-54]
SELECT "o"['a'] AS "a" FROM "default"."T" AS "o" WHERE EXISTS (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['c'] = "o"['c']) AND ("o"['b'] = (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("u"['b']) OVER () AS "m" FROM "default"."T" AS "u" WHERE "u"['c'] = "o"['c']) AS "s"));

-- Correlation in the select list, outside the window function
--#[window-55]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () + "o"['b'] AS "m" FROM "default"."T" AS "t") AS "s") AS "m" FROM "default"."T" AS "o";

-- Correlation in a subquery nested in the subquery with the window
--#[window-56]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE EXISTS (SELECT "u"['a'] AS "a" FROM "default"."T" AS "u" WHERE "u"['c'] = "o"['c'])) AS "s") AS "m" FROM "default"."T" AS "o";

-- Correlated subquery whose window is only in an uncorrelated derived table
--#[window-57]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t") AS "s" WHERE "s"['m'] > "o"['b']) AS "m" FROM "default"."T" AS "o";

-- Correlation in a path index
--#[window-58]
SELECT "o"['a'] AS "a", (SELECT max("s"['m']) AS "m" FROM (SELECT MAX("t"['b']) OVER () AS "m" FROM "default"."T" AS "t" WHERE "t"['array']["o"['b']] = 1) AS "s") AS "m" FROM "default"."T" AS "o";
