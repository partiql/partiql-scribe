--#[nvl]
SELECT COALESCE("simple_t"['c'], 'none') AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[nvl-several-arguments]
SELECT COALESCE("t"['c'], "t"['z'], 'none') AS "_1" FROM "default"."T" AS "t";

--#[coalesce]
SELECT COALESCE("simple_t"['c'], 'none') AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[coalesce-of-aggregate]
SELECT COALESCE(max("simple_t"['b']), 0) AS "_1", sum(COALESCE("simple_t"['b'], 0)) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[nullif]
SELECT NULLIF("simple_t"['b'], 0) AS "_1", NULLIF("simple_t"['c'], '') AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[nvl2]
SELECT CASE WHEN "simple_t"['c'] IS NOT NULL THEN 'set' ELSE 'unset' END AS "_1", CASE WHEN "simple_t"['b'] IS NOT NULL THEN "simple_t"['b'] + 1 ELSE 0 END AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode]
SELECT CASE WHEN "simple_t"['b'] = 1 THEN 'one' WHEN "simple_t"['b'] = 2 THEN 'two' ELSE 'other' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-without-default]
SELECT CASE WHEN "simple_t"['b'] = 1 THEN 'one' ELSE NULL END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-null-search-value]
SELECT CASE WHEN "simple_t"['c'] IS NULL THEN 'none' ELSE 'some' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-column-search-value]
SELECT CASE WHEN ("t"['b'] = "u"['b']) OR ("t"['b'] IS NULL AND "u"['b'] IS NULL) THEN 'same' ELSE 'different' END AS "_1" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true;

--#[greatest-least]
SELECT CASE WHEN "simple_t"['b'] >= 1 THEN "simple_t"['b'] ELSE 1 END AS "_1", CASE WHEN "simple_t"['b'] <= 1 THEN "simple_t"['b'] ELSE 1 END AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-three-arguments]
SELECT CASE WHEN ("simple_t"['b'] >= COALESCE("simple_t"['b'] + 1, "simple_t"['b'])) AND ("simple_t"['b'] >= 10) THEN "simple_t"['b'] WHEN ("simple_t"['b'] + 1) >= 10 THEN "simple_t"['b'] + 1 ELSE 10 END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-columns]
SELECT CASE WHEN "t"['b'] >= COALESCE("u"['b'], "t"['b']) THEN "t"['b'] ELSE "u"['b'] END AS "_1" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON true;

--#[greatest-one-argument]
SELECT "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-null-arguments]
SELECT "simple_t"['b'] AS "b", NULL AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-timestamps]
SELECT CASE WHEN "t"['timestamp_1'] >= COALESCE("t"['timestamp_2'], "t"['timestamp_1']) THEN "t"['timestamp_1'] ELSE "t"['timestamp_2'] END AS "_1" FROM "default"."T" AS "t";

--#[least-string]
SELECT CASE WHEN "simple_t"['c'] <= 'm' THEN "simple_t"['c'] ELSE 'm' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[conditional-in-where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE (COALESCE("simple_t"['b'], 0) > 0) AND NULLIF("simple_t"['c'], '') IS NOT NULL;

--#[conditional-nested]
SELECT COALESCE(NULLIF("simple_t"['c'], ''), 'empty') AS "_1", CASE WHEN CASE WHEN "simple_t"['c'] IS NOT NULL THEN 1 ELSE 0 END = 1 THEN 'set' ELSE 'unset' END AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[conditional-order-by-alias]
SELECT COALESCE("simple_t"['b'], 0) AS "n" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY COALESCE("simple_t"['b'], 0) ASC NULLS LAST;

--#[conditional-distinct-limit]
SELECT "$distinct"['n'] AS "n" FROM (SELECT DISTINCT COALESCE("simple_t"['b'], 0) AS "n" FROM "default"."SIMPLE_T" AS "simple_t") AS "$distinct" ORDER BY "$distinct"['n'] ASC NULLS LAST LIMIT 5;

--#[decode-group-by]
SELECT CASE WHEN "simple_t"['b'] = 1 THEN 'one' ELSE 'other' END AS "k", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY CASE WHEN "simple_t"['b'] = 1 THEN 'one' ELSE 'other' END;

--#[decode-signed-search-value]
SELECT CASE WHEN "simple_t"['b'] = (-1) THEN 'neg' ELSE 'other' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-null-expression]
SELECT CASE WHEN "simple_t"['b'] IS NULL THEN 'match' ELSE 'no' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-literal-expression]
SELECT CASE WHEN 1 = "simple_t"['b'] THEN 'match' ELSE 'no' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-boolean]
SELECT CASE WHEN "simple_t"['a'] = true THEN 'yes' WHEN "simple_t"['a'] = false THEN 'no' ELSE 'unknown' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-signed-literal]
SELECT CASE WHEN "simple_t"['b'] >= (-1) THEN "simple_t"['b'] ELSE -1 END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[nvl-subquery]
SELECT COALESCE((SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t"), 0) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[nullif-subquery]
SELECT NULLIF("simple_t"['b'], (SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t")) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[greatest-subquery]
SELECT CASE WHEN "simple_t"['b'] >= COALESCE((SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t"), "simple_t"['b']) THEN "simple_t"['b'] ELSE (SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t") END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[decode-subquery-search-value]
SELECT CASE WHEN ("simple_t"['b'] = (SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t")) OR ("simple_t"['b'] IS NULL AND (SELECT max("t"['b']) AS "_1" FROM "default"."T" AS "t") IS NULL) THEN 'max' ELSE 'other' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[conditional-in-having-and-join]
SELECT "t"['c'] AS "c", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "t" INNER JOIN "default"."T" AS "u" ON COALESCE("t"['b'], 0) = COALESCE("u"['b'], 0) GROUP BY "t"['c'] HAVING COALESCE(max("t"['b']), 0) > 1;

--#[conditional-in-between-and-like]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['b'] BETWEEN CASE WHEN 1 <= 2 THEN 1 ELSE 2 END AND CASE WHEN 1 >= 2 THEN 1 ELSE 2 END AND COALESCE("simple_t"['c'], '') LIKE 'a%';

--#[conditional-mixed-case-names]
SELECT COALESCE("simple_t"['c'], 'x') AS "_1", CASE WHEN "simple_t"['b'] = 1 THEN 'a' ELSE NULL END AS "_2", CASE WHEN "simple_t"['b'] >= 1 THEN "simple_t"['b'] ELSE 1 END AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";
