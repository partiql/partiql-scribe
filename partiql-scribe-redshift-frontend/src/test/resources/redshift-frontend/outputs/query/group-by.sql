--#[group-key-order-by]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" ORDER BY "$k0" ASC NULLS LAST;

--#[group-key-order-by-desc]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" ORDER BY "$k0" DESC NULLS FIRST, count(1) ASC NULLS LAST;

--#[group-key-having]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" HAVING "$k0" <> 'x';

--#[group-key-having-and-order-by]
SELECT "t"['c'] AS "c", "t"['z'] AS "z", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'], "t"['z'] AS "$k0" HAVING "$k0" IS NOT NULL ORDER BY "$k0" ASC NULLS LAST;

--#[group-key-super-path]
SELECT "t"['d']['e'] AS "e", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['d']['e'] AS "$k0" ORDER BY "$k0" ASC NULLS LAST;

-- --#[group-key-expression-order-by] TODO(redshift)
SELECT "upper"("t"['c']) AS "v", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "upper"("t"['c']) AS "$k0" ORDER BY "$k0" ASC NULLS LAST;

-- --#[group-key-expression-having] TODO(redshift)
SELECT "upper"("t"['c']) AS "v", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "upper"("t"['c']) AS "$k0" HAVING "$k0" <> 'X';

-- --#[group-key-qualification] TODO(redshift)
SELECT "upper"("t"['c']) AS "u", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "upper"("t"['c']);

--#[group-key-subquery]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "t" GROUP BY "t"['b'] AS "$k0" HAVING "$k0" > 1);

--#[group-key-window-partition]
SELECT "t"['c'] AS "c", "t"['z'] AS "z", RANK() OVER (PARTITION BY "$k0" ORDER BY "t"['c'] ASC NULLS LAST) AS "r" FROM "default"."T" AS "t" GROUP BY "t"['c'], "t"['z'] AS "$k0";

--#[group-key-aggregate-order-by]
SELECT "t"['c'] AS "c" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" ORDER BY count(1) DESC NULLS FIRST, "$k0" ASC NULLS LAST LIMIT 5;

--#[group-by-position]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'];

-- --#[group-by-position-expression] TODO(redshift)
SELECT "upper"("t"['c']) AS "u", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "upper"("t"['c']);

--#[group-by-position-out-of-range]
[INVALID_INPUT{message="GROUP BY position 2 is not in the select list"}];

--#[group-by-position-aggregate]
[INVALID_INPUT{message="GROUP BY position 1 refers to an aggregate or window function"}];

--#[order-by-position-aggregate]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] ORDER BY count(1) DESC NULLS FIRST;

--#[group-by-alias]
SELECT "t"['b'] + 1 AS "q", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['b'] + 1;

--#[group-by-alias-order-by]
SELECT "t"['c'] AS "k", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" ORDER BY "$k0" ASC NULLS LAST;

--#[group-by-alias-of-the-same-column]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'];

-- --#[group-by-alias-and-column] TODO(redshift)
[UNSUPPORTED{message="`c` in GROUP BY, which is both a column and a select-list alias"}];

-- --#[having-alias] TODO(redshift)
SELECT "t"['c'] AS "c", sum("t"['b']) AS "pp" FROM "default"."T" AS "t" GROUP BY "t"['c'] HAVING sum("t"['b']) > CAST(10 AS BIGINT);

--#[having-without-group-by]
SELECT count(1) AS "n" FROM "default"."T" AS "t" HAVING count(1) > CAST(1 AS BIGINT);

--#[group-by-all]
SELECT "t"['c'] AS "c", "t"['z'] AS "z", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'], "t"['z'];

-- --#[group-by-all-expression] TODO(redshift)
SELECT "t"['c'] AS "c", "t"['b'] + 1 AS "x", sum("t"['b']) AS "s" FROM "default"."T" AS "t" GROUP BY "t"['c'], "t"['b'] + 1;

--#[group-by-all-constant]
[UNSUPPORTED{message="GROUP BY ALL with a constant select item"}];

--#[select-star-group-by]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "simple_t"['b'], "simple_t"['a'], "simple_t"['c'];

-- --#[group-by-rollup] TODO(redshift)
[UNSUPPORTED{message="GROUP BY ROLLUP"}];

-- --#[group-by-cube] TODO(redshift)
[UNSUPPORTED{message="GROUP BY CUBE"}];

-- --#[group-by-grouping-sets] TODO(redshift)
[UNSUPPORTED{message="GROUP BY GROUPING SETS"}];
