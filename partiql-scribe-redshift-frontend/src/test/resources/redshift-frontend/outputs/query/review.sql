--#[review-set-operation-in-00]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "$s"['b'] AS "b" FROM ((SELECT "s"['b'] AS "b" FROM "default"."SIMPLE_T" AS "s") UNION DISTINCT (SELECT "u"['b'] AS "b" FROM "default"."T" AS "u")) AS "$s");

--#[review-set-operation-in-01]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] NOT IN (SELECT "$s"['b'] AS "b" FROM ((SELECT DISTINCT "s"['b'] AS "b" FROM "default"."SIMPLE_T" AS "s") EXCEPT DISTINCT (SELECT "u"['b'] AS "b" FROM "default"."T" AS "u")) AS "$s");

-- --#[review-set-operation-scalar-00] TODO(redshift)
SELECT (SELECT "$s"['m'] AS "m" FROM ((SELECT max("s"['b']) AS "m" FROM "default"."SIMPLE_T" AS "s") UNION DISTINCT (SELECT max("u"['b']) AS "m" FROM "default"."SIMPLE_T" AS "u")) AS "$s") AS "v" FROM "default"."T" AS "t";

--#[review-set-operation-any-00]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "$s"['b'] AS "b" FROM ((SELECT "s"['b'] AS "b" FROM "default"."SIMPLE_T" AS "s") INTERSECT DISTINCT (SELECT "u"['b'] AS "b" FROM "default"."T" AS "u")) AS "$s");

--#[review-set-operation-order-by-00]
SELECT "$s"['t'] AS "t" FROM ((SELECT "t"['b'] AS "t" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "s"['b'] AS "t" FROM "default"."SIMPLE_T" AS "s")) AS "$s" ORDER BY "$s"['t'] DESC NULLS FIRST LIMIT 2;

--#[review-set-operation-order-by-01]
SELECT "$s"['x'] AS "x" FROM ((SELECT "t"['b'] AS "x" FROM "default"."T" AS "t") UNION ALL (SELECT "s"['b'] AS "x" FROM "default"."SIMPLE_T" AS "s")) AS "$s" ORDER BY "$s"['x'] DESC NULLS FIRST;

-- --#[review-default-name-00] TODO(redshift)
SELECT "d"['coalesce'] AS "coalesce", "d"['case'] AS "case", "d"['pgdate_part'] AS "pgdate_part" FROM (SELECT COALESCE("t"['b'], 0) AS "coalesce", CASE WHEN "t"['b'] = 1 THEN 'x' ELSE 'y' END AS "case", CAST(EXTRACT(YEAR FROM "t"['timestamp_1']) AS DOUBLE PRECISION) AS "pgdate_part" FROM "default"."T" AS "t") AS "d";

--#[review-default-name-01]
SELECT "d"['timestamp'] IS NOT NULL AS "v" FROM (SELECT DATE_ADD(SECOND, DATE_DIFF(SECOND, CAST(TIMESTAMP '0001-01-01 00:00:00' AS TIMESTAMP WITH TIME ZONE), "utcnow"()), TIMESTAMP '0001-01-01 00:00:00') AS "timestamp" FROM <<1>> AS "$no_from") AS "d";
