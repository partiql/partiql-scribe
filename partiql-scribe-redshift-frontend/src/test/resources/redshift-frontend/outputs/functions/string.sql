--#[upper-lower]
SELECT "upper"("simple_t"['c']) AS "_1", "lower"("simple_t"['c']) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[upper-lower-of-literal]
SELECT "upper"('abc') AS "_1", "lower"('ABC') AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[upper-in-where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "upper"("simple_t"['c']) = 'X';

--#[upper-of-conditional]
SELECT "upper"(COALESCE("simple_t"['c'], 'none')) AS "_1", COALESCE("lower"("simple_t"['c']), 'none') AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[upper-in-group-by]
SELECT "upper"("simple_t"['c']) AS "u", count(1) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t" GROUP BY "upper"("simple_t"['c']);

--#[lower-of-upper]
SELECT "lower"("upper"("simple_t"['c'])) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat]
SELECT "simple_t"['c'] || 'x' AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-nested]
SELECT 'Thursday, ' || ('December 25, ' || '2008') AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-and-operator]
SELECT ("simple_t"['c'] || '-') || "simple_t"['c'] AS "_1", "simple_t"['c'] || ('-' || "simple_t"['c']) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-numeric-literal]
SELECT "simple_t"['c'] || '1' AS "_1", 'x' || '7' AS "_2", 'v' || '1.50' AS "_3", ("simple_t"['c'] || '0.5') || '1' AS "_4" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-signed-literal]
SELECT "simple_t"['c'] || '-1' AS "_1", "simple_t"['c'] || '2' AS "_2", "simple_t"['c'] || '-0.50' AS "_3", '3' || "simple_t"['c'] AS "_4" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-null]
SELECT "simple_t"['c'] || NULL AS "_1", NULL || "simple_t"['c'] AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-in-like]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "simple_t"['c'] LIKE "simple_t"['c'] || '%' ESCAPE '\';

--#[string-functions-mixed-case-names]
SELECT "upper"("simple_t"['c']) || "lower"("simple_t"['c']) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

