-- --#[qualify-window] TODO(redshift)
SELECT "$q"['b'] AS "b", "$q"['c'] AS "c" FROM (SELECT "t"['b'] AS "b", "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] DESC NULLS FIRST) = 1 AS "$qualify" FROM "default"."T" AS "t") AS "$q" WHERE "$q"['$qualify'];

-- --#[qualify-star] TODO(redshift)
SELECT "$q"['a'] AS "a", "$q"['b'] AS "b", "$q"['c'] AS "c" FROM (SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] DESC NULLS FIRST) = 1 AS "$qualify" FROM "default"."SIMPLE_T" AS "t") AS "$q" WHERE "$q"['$qualify'];

-- --#[qualify-alias] TODO(redshift)
SELECT "$q"['c'] AS "c", "$q"['b'] AS "b", "$q"['rn'] AS "rn" FROM (SELECT "t"['c'] AS "c", "t"['b'] AS "b", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "rn" FROM "default"."T" AS "t") AS "$q" WHERE "$q"['rn'] <= CAST(2 AS BIGINT);

-- --#[qualify-alias-and-column] TODO(redshift)
SELECT "$q"['c'] AS "c", "$q"['rn'] AS "rn" FROM (SELECT "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "rn", "t"['b'] > 0 AS "$qualify" FROM "default"."T" AS "t") AS "$q" WHERE ("$q"['rn'] = 1) AND "$q"['$qualify'];

-- --#[qualify-grouped] TODO(redshift)
SELECT "$q"['c'] AS "c", "$q"['n'] AS "n" FROM (SELECT "t"['c'] AS "c", count(1) AS "n", RANK() OVER (ORDER BY count(1) DESC NULLS FIRST) <= CAST(2 AS BIGINT) AS "$qualify" FROM "default"."T" AS "t" GROUP BY "t"['c']) AS "$q" WHERE "$q"['$qualify'];

-- --#[qualify-where] TODO(redshift)
SELECT "$q"['b'] AS "b", "$q"['c'] AS "c" FROM (SELECT "t"['b'] AS "b", "t"['c'] AS "c", RANK() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) = 1 AS "$qualify" FROM "default"."T" AS "t" WHERE "t"['b'] > 0) AS "$q" WHERE "$q"['$qualify'];

-- --#[qualify-order-by-limit] TODO(redshift)
SELECT "$q"['b'] AS "b", "$q"['c'] AS "c" FROM (SELECT "t"['b'] AS "b", "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) = 1 AS "$qualify" FROM "default"."T" AS "t") AS "$q" WHERE "$q"['$qualify'] ORDER BY "$q"['c'] DESC NULLS FIRST LIMIT 3;

-- --#[qualify-order-by-not-selected] TODO(redshift)
SELECT "$q"['b'] AS "b" FROM (SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) = 1 AS "$qualify", "t"['c'] AS "$o" FROM "default"."T" AS "t") AS "$q" WHERE "$q"['$qualify'] ORDER BY "$q"['$o'] ASC NULLS LAST;

-- --#[qualify-distinct-limit] TODO(redshift)
SELECT "$d"['c'] AS "c" FROM (SELECT DISTINCT "$q"['c'] AS "c" FROM (SELECT "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) = 1 AS "$qualify" FROM "default"."T" AS "t") AS "$q" WHERE "$q"['$qualify']) AS "$d" ORDER BY "$d"['c'] ASC NULLS LAST LIMIT 3;

-- --#[qualify-repeated-names] TODO(redshift)
SELECT "$q"['$c1'] AS "b", "$q"['$c2'] AS "b" FROM (SELECT "t1"['b'] AS "$c1", "t2"['b'] AS "$c2", ROW_NUMBER() OVER (ORDER BY "t1"['b'] ASC NULLS LAST) = 1 AS "$qualify" FROM "default"."T" AS "t1" INNER JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['c'] = "t2"['c']) AS "$q" WHERE "$q"['$qualify'];

-- --#[qualify-without-alias] TODO(redshift)
[INVALID_INPUT{message="the FROM item before QUALIFY needs an alias; Redshift reads QUALIFY as its alias"}];

-- --#[qualify-as-name] TODO(redshift)
SELECT "qualify"['b'] AS "b" FROM "default"."T" AS "qualify";
