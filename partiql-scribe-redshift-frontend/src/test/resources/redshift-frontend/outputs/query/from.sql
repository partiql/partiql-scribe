-- --#[comma-join] TODO(redshift)
SELECT "t"['b'] AS "b", "s"['a'] AS "a" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "s" ON true WHERE "t"['b'] = "s"['b'];

-- --#[full-outer-join] TODO(redshift)
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" FULL JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['b'] = "t2"['b'];

-- --#[cross-join] TODO(redshift)
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" INNER JOIN "default"."SIMPLE_T" AS "t2" ON true;

-- --#[right-join] TODO(redshift)
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" RIGHT JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['b'] = "t2"['b'];

-- --#[join-without-condition] TODO(redshift)
[INVALID_INPUT{message="JOIN without ON or USING"}];

-- --#[parenthesized-join] TODO(redshift)
SELECT "t"['b'] AS "b", "u"['col_int32'] AS "col_int32" FROM "default"."T" AS "t" LEFT JOIN "default"."SIMPLE_T" AS "s" INNER JOIN "default"."T_ALL_TYPES" AS "u" ON "s"['b'] = "u"['col_int32'] ON "t"['b'] = "s"['b'];

-- --#[join-using] TODO(redshift)
SELECT "t"['b'] AS "b", "t"['c'] AS "c", "simple_t"['a'] AS "a" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

-- --#[join-using-left] TODO(redshift)
SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t" LEFT JOIN "default"."SIMPLE_T" AS "s" ON ("t"['b'] = "s"['b']) AND ("t"['c'] = "s"['c']);

-- --#[join-using-right] TODO(redshift)
SELECT "simple_t"['b'] AS "b" FROM "default"."T" AS "t" RIGHT JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

-- --#[join-using-full] TODO(redshift)
SELECT COALESCE("t"['b'], "simple_t"['b']) AS "b" FROM "default"."T" AS "t" FULL JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

-- --#[join-using-unknown-column] TODO(redshift)
[INVALID_INPUT{message="column `z` of USING is not a column of the joined tables"}];

-- --#[join-using-star] TODO(redshift)
[UNSUPPORTED{message="SELECT * with JOIN USING"}];

-- --#[natural-join] TODO(redshift)
[UNSUPPORTED{message="NATURAL JOIN"}];

-- --#[subquery-without-alias] TODO(redshift)
SELECT "_0"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "_0";

-- --#[subquery-column-aliases] TODO(redshift)
SELECT "s"['x'] AS "x" FROM (SELECT "t"['b'] AS "x", "t"['c'] AS "y" FROM "default"."T" AS "t") AS "s";

-- --#[subquery-fewer-column-aliases] TODO(redshift)
SELECT "s"['x'] AS "x", "s"['c'] AS "c" FROM (SELECT "t"['b'] AS "x", "t"['c'] AS "c" FROM "default"."T" AS "t") AS "s";

-- --#[subquery-column-aliases-set-operation] TODO(redshift)
SELECT "s"['x'] AS "x" FROM ((SELECT "t"['b'] AS "x" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['b'] AS "x" FROM "default"."SIMPLE_T" AS "simple_t")) AS "s";

-- --#[subquery-not-lateral] TODO(redshift)
SELECT "s"['v'] AS "v" FROM "default"."T" AS "t" INNER JOIN (SELECT "simple_t"['b'] AS "v" FROM "default"."SIMPLE_T" AS "simple_t") AS "s" ON true;

-- --#[table-column-aliases] TODO(redshift)
[UNSUPPORTED{message="column aliases for table `SIMPLE_T`"}];

-- --#[table-names] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";

-- --#[table-star] TODO(redshift)
[UNSUPPORTED{message="`table_name *`"}];

-- --#[temporary-table] TODO(redshift)
[UNSUPPORTED{message="temporary table `#tmp`"}];

-- --#[select-into] TODO(redshift)
[UNSUPPORTED{message="SELECT INTO"}];

-- --#[connect-by] TODO(redshift)
[UNSUPPORTED{message="CONNECT BY"}];

-- --#[pivot] TODO(redshift)
[UNSUPPORTED{message="PIVOT"}];

-- --#[unpivot] TODO(redshift)
[UNSUPPORTED{message="UNPIVOT"}];

-- --#[unnest] TODO(redshift)
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

-- --#[unnest-path-and-navigation] TODO(redshift)
SELECT "t"['b'] AS "b", COALESCE("e"['a']) AS "a" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'].items IS LIST THEN @"t"['x'].items ELSE [] END AS "e" ON true;

-- --#[unnest-nested] TODO(redshift)
SELECT "el" AS "el", "f" AS "f" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "el" ON true INNER JOIN CASE WHEN @"el" IS LIST THEN @"el" ELSE [] END AS "f" ON true;

-- --#[unnest-nested-ambiguous] TODO(redshift)
[UNSUPPORTED{message="column `a`, which can be a column of more than one FROM item with unknown columns"}];

-- --#[unnest-alias-of-table-name] TODO(redshift)
SELECT "t1"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t1" INNER JOIN CASE WHEN @"t1"['x'] IS LIST THEN @"t1"['x'] ELSE [] END AS "e" ON true;

-- --#[unnest-left-join] TODO(redshift)
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" LEFT JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

-- --#[unnest-join-without-condition] TODO(redshift)
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

-- --#[unnest-in-subquery] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "e" AS "e" FROM CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e");

-- --#[unnest-function] TODO(redshift)
SELECT "o"['b'] AS "b", "up"['product'] AS "product" FROM "default"."T" AS "o" INNER JOIN (SELECT "$e" AS "product" FROM CASE WHEN @"o"['x'] IS LIST THEN @"o"['x'] ELSE [] END AS "$e") AS "up" ON true;

-- --#[unnest-function-with-offset] TODO(redshift)
[UNSUPPORTED{message="UNNEST ... WITH OFFSET"}];

-- --#[unnest-at-index] TODO(redshift)
[UNSUPPORTED{message="AT index in a FROM item"}];

-- --#[unnest-star] TODO(redshift)
[UNSUPPORTED{message="SELECT * with an unnested SUPER value `e`"}];

-- --#[object-unpivot] TODO(redshift)
[UNSUPPORTED{message="UNPIVOT of a SUPER object"}];
