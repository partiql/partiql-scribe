-- --#[lateral-alias] TODO(redshift)
SELECT "t"['b'] + 1 AS "q", ("t"['b'] + 1) * 2 AS "y" FROM "default"."T" AS "t";

-- --#[lateral-alias-column-wins] TODO(redshift)
SELECT "t"['b'] + 1 AS "c", "t"['c'] AS "y" FROM "default"."T" AS "t";

-- --#[lateral-alias-aggregate] TODO(redshift)
SELECT sum("t"['b']) AS "s", sum("t"['b']) * CAST(2 AS BIGINT) AS "d" FROM "default"."T" AS "t";

-- --#[where-alias] TODO(redshift)
SELECT "t"['b'] + 1 AS "q" FROM "default"."T" AS "t" WHERE ("t"['b'] + 1) > 2;

-- --#[where-alias-and-column] TODO(redshift)
[UNSUPPORTED{message="`b` in WHERE, which is both a column and a select-list alias"}];

-- --#[where-alias-aggregate] TODO(redshift)
[INVALID_INPUT{message="aggregate or window function of the alias `n` in WHERE"}];

-- --#[order-by-alias] TODO(redshift)
SELECT "t"['b'] + 1 AS "c" FROM "default"."T" AS "t" ORDER BY "t"['b'] + 1 ASC NULLS LAST;

-- --#[order-by-alias-expression] TODO(redshift)
SELECT "t"['b'] + 1 AS "q" FROM "default"."T" AS "t" ORDER BY ("t"['b'] + 1) * 2 ASC NULLS LAST;

--#[order-by-position]
SELECT "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t" ORDER BY "t"['c'] ASC NULLS LAST, "t"['b'] DESC NULLS FIRST;

-- --#[order-by-position-expression] TODO(redshift)
SELECT "t"['b'] + 1 AS "x" FROM "default"."T" AS "t" ORDER BY "t"['b'] + 1 DESC NULLS FIRST;

--#[order-by-position-star]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['c'] ASC NULLS LAST;

--#[order-by-position-out-of-range]
[INVALID_INPUT{message="ORDER BY position 2 is not in the select list"}];

--#[order-by-not-selected]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" ORDER BY "t"['c'] DESC NULLS LAST;

--#[order-by-ambiguous-name]
[INVALID_INPUT{message="ORDER BY `x` is ambiguous"}];

--#[column-named-like-alias]
SELECT "b"['b'] AS "b" FROM "default"."T" AS "b";

-- --#[column-named-like-outer-alias] TODO(redshift)
SELECT "b"['c'] AS "c" FROM "default"."T" AS "b" WHERE "b"['b'] IN (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t");

--#[column-named-like-earlier-from-item]
SELECT "q"['v'] AS "v" FROM "default"."T" AS "b" INNER JOIN (SELECT "simple_t"['b'] AS "v" FROM "default"."SIMPLE_T" AS "simple_t") AS "q" ON true;

-- --#[correlated-reference-repeated-alias] TODO(redshift)
SELECT "t$1"['b'] AS "b", (SELECT max("t"['b']) AS "max" FROM "default"."SIMPLE_T" AS "t" WHERE "t"['c'] = "t$1"['z']) AS "m" FROM "default"."T" AS "t$1";

--#[unknown-qualified-column]
[INVALID_INPUT{message="column `t.nope` does not exist"}];

--#[ambiguous-column]
[INVALID_INPUT{message="column reference `b` is ambiguous"}];

--#[from-alias-as-value]
[UNSUPPORTED{message="reference to the FROM item `s` as a value"}];

--#[schema-qualified-column]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";

--#[star-and-item]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c", "simple_t"['b'] AS "b2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[item-and-star]
SELECT "simple_t"['b'] AS "b2", "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

-- --#[star-join] TODO(redshift)
SELECT "s1"['a'] AS "a", "s1"['b'] AS "b", "s1"['c'] AS "c", "s2"['a'] AS "a", "s2"['b'] AS "b", "s2"['c'] AS "c" FROM "default"."SIMPLE_T" AS "s1" INNER JOIN "default"."SIMPLE_T" AS "s2" ON "s1"['b'] = "s2"['b'];

--#[qualified-star]
SELECT "s"['a'] AS "a", "s"['b'] AS "b", "s"['c'] AS "c", "t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "s" INNER JOIN "default"."T" AS "t" ON true;

--#[exclude]
SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['x'] AS "x", "t"['array'] AS "array", "t"['z'] AS "z", "t"['v'] AS "v", "t"['timestamp_1'] AS "timestamp_1", "t"['timestamp_2'] AS "timestamp_2" FROM "default"."T" AS "t";

--#[exclude-parenthesized]
SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[exclude-unknown-column]
[INVALID_INPUT{message="column `nope` of EXCLUDE is not a column of the FROM items"}];

--#[exclude-select-list]
[UNSUPPORTED{message="EXCLUDE with a select list other than *"}];

--#[star-repeated-names]
[UNSUPPORTED{message="* over a FROM item with duplicate column name `x`"}];

--#[star-of-super-value]
[UNSUPPORTED{message=".* over a SUPER value"}];
