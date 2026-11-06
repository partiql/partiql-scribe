--#[power]
SELECT "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[power-of-expression]
SELECT "pow"(CAST(CAST("simple_t"['b'] AS DECIMAL(10,0)) * 2.0 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION) / CAST(3 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[power-of-literals]
SELECT "pow"(CAST(1024.0 AS DOUBLE PRECISION), CAST(3 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST(2 AS DOUBLE PRECISION), CAST(10 AS DOUBLE PRECISION)) AS "_2", "pow"(CAST(2 AS DOUBLE PRECISION), CAST(-1 AS DOUBLE PRECISION)) AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";

--#[power-in-where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) > CAST(100 AS DOUBLE PRECISION);

--#[power-of-conditional]
SELECT "pow"(CAST(COALESCE("simple_t"['b'], 0) AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1", COALESCE("pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)), 0) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[power-nested]
SELECT "pow"("pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)), CAST(2 AS DOUBLE PRECISION)) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[caret]
SELECT "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST(2 AS DOUBLE PRECISION), CAST("simple_t"['b'] AS DOUBLE PRECISION)) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[caret-precedence]
SELECT "pow"("pow"(CAST(2 AS DOUBLE PRECISION), CAST(3 AS DOUBLE PRECISION)), CAST(2 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST(-2 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_2", CAST(2 AS DOUBLE PRECISION) * "pow"(CAST(3 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_3", "pow"(CAST(2 * 3 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_4", "pow"(CAST(2 AS DOUBLE PRECISION), "pow"(CAST(3 AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION))) AS "_5" FROM "default"."SIMPLE_T" AS "simple_t";

--#[caret-and-power]
SELECT "pow"("pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)), CAST(2 AS DOUBLE PRECISION)) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[math-functions-mixed-case-names]
SELECT "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1", "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

