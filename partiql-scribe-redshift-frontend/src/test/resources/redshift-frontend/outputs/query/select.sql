--#[select-columns]
SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."T" AS "t";

--#[select-star]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-qualified-star]
SELECT "t"['a'] AS "a", "t"['b'] AS "b", "t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "t";

--#[select-alias-as]
SELECT "simple_t"['a'] AS "x", "simple_t"['b'] AS "y" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-alias-bare]
SELECT "simple_t"['a'] AS "x", "simple_t"['b'] AS "y" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-alias-reserved-after-as]
SELECT "simple_t"['a'] AS "from", "simple_t"['b'] AS "select" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-qualified-columns]
SELECT "t"['a'] AS "a", "t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "t";

--#[select-table-qualified-columns]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-case-folding]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-distinct]
SELECT DISTINCT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-all]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-top]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['a'] ASC NULLS LAST LIMIT 5;

--#[select-exclude-single-table]
SELECT "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" EXCLUDE "simple_t".a FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-exclude-list]
SELECT "simple_t"['c'] AS "c" EXCLUDE "simple_t".b, "simple_t".a FROM "default"."SIMPLE_T" AS "simple_t";

--#[select-exclude-qualified]
SELECT "t"['b'] AS "b", "t"['c'] AS "c", "u"['a'] AS "a", "u"['b'] AS "b", "u"['c'] AS "c", "u"['d'] AS "d", "u"['x'] AS "x", "u"['array'] AS "array", "u"['z'] AS "z", "u"['v'] AS "v", "u"['timestamp_1'] AS "timestamp_1", "u"['timestamp_2'] AS "timestamp_2" EXCLUDE "t".a FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true;

--#[select-exclude-qualified-star]
SELECT "t"['b'] AS "b", "t"['c'] AS "c" EXCLUDE "t".a FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true;

--#[select-exclude-ambiguous]
[UNSUPPORTED_OPERATION{message="EXCLUDE of an unqualified column without a single named FROM item"}];

--#[select-exclude-with-items]
[UNSUPPORTED_OPERATION{message="EXCLUDE with select items other than `*`"}];

--#[select-star-and-items]
[UNSUPPORTED_OPERATION{message="`*` with other select items"}];

--#[select-exclude-unknown-qualifier]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: EXCLUDE t.c: `t` is not a FROM item"}];

--#[select-exclude-other-relation-than-star]
[UNSUPPORTED_OPERATION{message="EXCLUDE y.c of another relation than the `*`"}];

--#[select-exclude-own-relation-of-star]
SELECT "x"['a'] AS "a", "x"['b'] AS "b" EXCLUDE "x".c FROM "default"."SIMPLE_T" AS "x" INNER JOIN "default"."T" AS "y" ON "x"['b'] = "y"['b'];

--#[select-exclude-three-part-name]
SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b" EXCLUDE "simple_t".c FROM "default"."SIMPLE_T" AS "simple_t";
