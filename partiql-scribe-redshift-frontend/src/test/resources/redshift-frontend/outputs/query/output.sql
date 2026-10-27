-- --#[default-name-aggregates] TODO(redshift)
SELECT count(1) AS "count", sum("t"['b']) AS "sum", max("t"['c']) AS "max" FROM "default"."T" AS "t";

--#[default-name-cast-of-aggregate]
SELECT CAST(count(1) AS INT) AS "count" FROM "default"."T" AS "t";

-- --#[default-name-function-and-path] TODO(redshift)
SELECT "upper"("t"['c']) AS "upper", "t"['d']['e'] AS "e", CAST("t"['b'] AS BIGINT) AS "b" FROM "default"."T" AS "t";

--#[default-name-window]
SELECT ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "row_number" FROM "default"."T" AS "t";

--#[default-name-expression]
SELECT "t"['b'] + 1 AS "_1" FROM "default"."T" AS "t";

--#[default-name-set-operation]
(SELECT count(1) AS "count" FROM "default"."T" AS "t") UNION ALL (SELECT count(1) AS "count" FROM "default"."SIMPLE_T" AS "simple_t");

--#[super-navigation-coalesce]
SELECT COALESCE("t"['x']['y']) AS "v", "t"['x'] AS "w" FROM "default"."T" AS "t";

--#[super-navigation-coalesce-grouped]
SELECT COALESCE("t"['x']['y']) AS "v", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['x']['y'];

--#[group-key-correlated]
SELECT "t"['c'] AS "c", count(1) AS "n" FROM "default"."T" AS "t" GROUP BY "t"['c'] AS "$k0" HAVING "$k0" IN (SELECT "u"['col_string'] AS "col_string" FROM "default"."T_ALL_TYPES" AS "u" WHERE "u"['col_string'] <> "$k0");
