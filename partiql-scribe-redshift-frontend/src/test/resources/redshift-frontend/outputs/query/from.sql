--#[comma-join]
SELECT "t"['b'] AS "b", "s"['a'] AS "a" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "s" ON true WHERE "t"['b'] = "s"['b'];

--#[full-outer-join]
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" FULL JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['b'] = "t2"['b'];

--#[cross-join]
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" INNER JOIN "default"."SIMPLE_T" AS "t2" ON true;

--#[right-join]
SELECT "t1"['b'] AS "b", "t2"['c'] AS "c" FROM "default"."T" AS "t1" RIGHT JOIN "default"."SIMPLE_T" AS "t2" ON "t1"['b'] = "t2"['b'];

--#[join-without-condition]
[INVALID_INPUT{message="JOIN without ON or USING"}];

--#[parenthesized-join]
SELECT "t"['b'] AS "b", "u"['col_int32'] AS "col_int32" FROM "default"."T" AS "t" LEFT JOIN "default"."SIMPLE_T" AS "s" INNER JOIN "default"."T_ALL_TYPES" AS "u" ON "s"['b'] = "u"['col_int32'] ON "t"['b'] = "s"['b'];

--#[join-using]
SELECT "t"['b'] AS "b", "t"['c'] AS "c", "simple_t"['a'] AS "a" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

--#[join-using-left]
SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t" LEFT JOIN "default"."SIMPLE_T" AS "s" ON ("t"['b'] = "s"['b']) AND ("t"['c'] = "s"['c']);

--#[join-using-right]
SELECT "simple_t"['b'] AS "b" FROM "default"."T" AS "t" RIGHT JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

--#[join-using-full]
SELECT COALESCE("t"['b'], "simple_t"['b']) AS "b" FROM "default"."T" AS "t" FULL JOIN "default"."SIMPLE_T" AS "simple_t" ON "t"['b'] = "simple_t"['b'];

--#[join-using-unknown-column]
[INVALID_INPUT{message="column `z` of USING is not a column of the joined tables"}];

--#[join-using-star]
[UNSUPPORTED{message="SELECT * with JOIN USING"}];

--#[natural-join]
[UNSUPPORTED{message="NATURAL JOIN"}];

--#[subquery-without-alias]
SELECT "_0"['b'] AS "b" FROM (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") AS "_0";

--#[subquery-column-aliases]
SELECT "s"['x'] AS "x" FROM (SELECT "t"['b'] AS "x", "t"['c'] AS "y" FROM "default"."T" AS "t") AS "s";

--#[subquery-fewer-column-aliases]
SELECT "s"['x'] AS "x", "s"['c'] AS "c" FROM (SELECT "t"['b'] AS "x", "t"['c'] AS "c" FROM "default"."T" AS "t") AS "s";

--#[subquery-column-aliases-set-operation]
SELECT "s"['x'] AS "x" FROM ((SELECT "t"['b'] AS "x" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['b'] AS "x" FROM "default"."SIMPLE_T" AS "simple_t")) AS "s";

--#[subquery-not-lateral]
SELECT "s"['v'] AS "v" FROM "default"."T" AS "t" INNER JOIN (SELECT "simple_t"['b'] AS "v" FROM "default"."SIMPLE_T" AS "simple_t") AS "s" ON true;

--#[table-column-aliases]
[UNSUPPORTED{message="column aliases for table `SIMPLE_T`"}];

--#[table-names]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[table-star]
[UNSUPPORTED{message="`table_name *`"}];

--#[temporary-table]
[UNSUPPORTED{message="temporary table `#tmp`"}];

--#[select-into]
[UNSUPPORTED{message="SELECT INTO"}];

--#[connect-by]
[UNSUPPORTED{message="CONNECT BY"}];

--#[pivot]
[UNSUPPORTED{message="PIVOT"}];

--#[unpivot]
[UNSUPPORTED{message="UNPIVOT"}];

--#[unnest]
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

--#[unnest-path-and-navigation]
SELECT "t"['b'] AS "b", COALESCE("e"['a']) AS "a" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'].items IS LIST THEN @"t"['x'].items ELSE [] END AS "e" ON true;

--#[unnest-nested]
SELECT "el" AS "el", "f" AS "f" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "el" ON true INNER JOIN CASE WHEN @"el" IS LIST THEN @"el" ELSE [] END AS "f" ON true;

--#[unnest-nested-ambiguous]
[UNSUPPORTED{message="column `a`, which can be a column of more than one FROM item with unknown columns"}];

--#[unnest-alias-of-table-name]
SELECT "t1"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t1" INNER JOIN CASE WHEN @"t1"['x'] IS LIST THEN @"t1"['x'] ELSE [] END AS "e" ON true;

--#[unnest-left-join]
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" LEFT JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

--#[unnest-join-without-condition]
SELECT "t"['b'] AS "b", "e" AS "e" FROM "default"."T" AS "t" INNER JOIN CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e" ON true;

--#[unnest-in-subquery]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "e" AS "e" FROM CASE WHEN @"t"['x'] IS LIST THEN @"t"['x'] ELSE [] END AS "e");

--#[unnest-function]
SELECT "o"['b'] AS "b", "up"['product'] AS "product" FROM "default"."T" AS "o" INNER JOIN (SELECT "$e" AS "product" FROM CASE WHEN @"o"['x'] IS LIST THEN @"o"['x'] ELSE [] END AS "$e") AS "up" ON true;

--#[unnest-function-with-offset]
[UNSUPPORTED{message="UNNEST ... WITH OFFSET"}];

--#[unnest-at-index]
[UNSUPPORTED{message="AT index in a FROM item"}];

--#[unnest-star]
[UNSUPPORTED{message="SELECT * with an unnested SUPER value `e`"}];

--#[object-unpivot]
[UNSUPPORTED{message="UNPIVOT of a SUPER object"}];
