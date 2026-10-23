-- --#[with] TODO(redshift)
SELECT "x"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "x";

-- --#[with-shadows-table] TODO(redshift)
SELECT "simple_t"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] > 1) AS "simple_t";

-- --#[with-subquery-reference] TODO(redshift)
SELECT "x"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "x" WHERE "x"['b'] = (SELECT max("x"['b']) AS "max" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "x");

-- --#[with-sibling-and-join] TODO(redshift)
SELECT "y"['b'] AS "b" FROM (SELECT "x"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "x" WHERE "x"['b'] > 1) AS "y" INNER JOIN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "x" ON "y"['b'] = "x"['b'];

-- --#[with-alias] TODO(redshift)
SELECT "q"['c'] AS "c" FROM (SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t") AS "q" WHERE "q"['b'] > 1;

-- --#[with-column-list] TODO(redshift)
SELECT "x"['q'] AS "q" FROM (SELECT "t"['b'] AS "q", "t"['c'] AS "c" FROM "default"."T" AS "t") AS "x";

-- --#[with-column-list-star] TODO(redshift)
SELECT "x"['q'] AS "q", "x"['r'] AS "r", "x"['c'] AS "c" FROM (SELECT "simple_t"['a'] AS "q", "simple_t"['b'] AS "r", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") AS "x";

-- --#[with-column-list-too-long] TODO(redshift)
[INVALID_INPUT{message="WITH column list has 2 names for 1 columns"}];

-- --#[with-set-operation] TODO(redshift)
SELECT "x"['b'] AS "b" FROM ((SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t")) AS "x" ORDER BY "x"['b'] ASC NULLS LAST;

-- --#[with-in-subquery] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "x"['b'] AS "b" FROM (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") AS "x");

-- --#[with-recursive] TODO(redshift)
[UNSUPPORTED{message="WITH RECURSIVE"}];
