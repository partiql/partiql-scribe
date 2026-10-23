-- --#[offset-without-limit] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] ASC NULLS LAST OFFSET 5;

-- --#[limit-all] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] ASC NULLS LAST;

-- --#[offset-before-limit] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] ASC NULLS LAST LIMIT 10 OFFSET 5;

-- --#[limit-zero] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" LIMIT 0;

-- --#[limit-column] TODO(redshift)
[INVALID_INPUT{message="LIMIT must not refer to a column"}];

-- --#[limit-negative] TODO(redshift)
[INVALID_INPUT{message="LIMIT must not be negative"}];

-- --#[limit-expression] TODO(redshift)
[UNSUPPORTED{message="LIMIT expression"}];

-- --#[limit-null] TODO(redshift)
[UNSUPPORTED{message="LIMIT expression"}];

-- --#[limit-too-large] TODO(redshift)
[INVALID_INPUT{message="LIMIT 2147483648 is greater than 2147483647"}];

-- --#[offset-column] TODO(redshift)
[INVALID_INPUT{message="OFFSET must not refer to a column"}];

-- --#[distinct-limit] TODO(redshift)
SELECT "$d"['b'] AS "b" FROM (SELECT DISTINCT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "$d" ORDER BY "$d"['b'] ASC NULLS LAST LIMIT 2;

-- --#[distinct-top] TODO(redshift)
SELECT "$d"['c'] AS "c" FROM (SELECT DISTINCT "t"['c'] AS "c" FROM "default"."T" AS "t") AS "$d" LIMIT 5;

-- --#[distinct-order-by-position-offset] TODO(redshift)
SELECT "$d"['b'] AS "b", "$d"['c'] AS "c" FROM (SELECT DISTINCT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t") AS "$d" ORDER BY "$d"['c'] DESC NULLS FIRST LIMIT 10 OFFSET 5;

-- --#[distinct-order-by-name] TODO(redshift)
SELECT "$d"['x'] AS "x" FROM (SELECT DISTINCT "t"['b'] AS "x" FROM "default"."T" AS "t") AS "$d" ORDER BY "$d"['x'] ASC NULLS LAST LIMIT 3;

-- --#[distinct-order-by-not-selected] TODO(redshift)
[INVALID_INPUT{message="for SELECT DISTINCT, ORDER BY expressions must appear in the select list"}];

-- --#[distinct-star-limit] TODO(redshift)
SELECT "$d"['a'] AS "a", "$d"['b'] AS "b", "$d"['c'] AS "c" FROM (SELECT DISTINCT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") AS "$d" LIMIT 5;

-- --#[distinct-repeated-names-limit] TODO(redshift)
SELECT "$d"['$c1'] AS "b", "$d"['$c2'] AS "b" FROM (SELECT DISTINCT "t1"['b'] AS "$c1", "t2"['b'] AS "$c2" FROM "default"."T" AS "t1" INNER JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['c'] = "t2"['c']) AS "$d" ORDER BY "$d"['$c1'] ASC NULLS LAST LIMIT 3;

-- --#[distinct-without-limit] TODO(redshift)
SELECT DISTINCT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['b'] ASC NULLS LAST;

-- --#[top] TODO(redshift)
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] ASC NULLS LAST LIMIT 2;

-- --#[select-without-from] TODO(redshift)
SELECT 1 AS "v" FROM <<1>> AS "$no_from";

-- --#[select-without-from-subquery] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] = (SELECT 1 AS "one" FROM <<1>> AS "$no_from");

-- --#[select-star-without-from] TODO(redshift)
[INVALID_INPUT{message="SELECT * with no tables specified is not valid"}];

-- --#[trailing-semicolons] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";

-- --#[multiple-statements] TODO(redshift)
[UNSUPPORTED{message="multiple statements"}];
