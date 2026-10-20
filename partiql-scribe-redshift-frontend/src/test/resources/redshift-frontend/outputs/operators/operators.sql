--#[cast-00]
SELECT CAST("t"['b'] AS BIGINT) AS "v" FROM "default"."T" AS "t";

--#[cast-01]
SELECT CAST("t"['b'] AS VARCHAR(256)) AS "v" FROM "default"."T" AS "t";

--#[cast-02]
SELECT CAST("t"['b'] AS DECIMAL(10,2)) AS "v1", CAST("t"['b'] AS DECIMAL(18,0)) AS "v2" FROM "default"."T" AS "t";

--#[cast-03]
SELECT -CAST("t"['b'] AS INT) AS "v" FROM "default"."T" AS "t";

--#[cast-04]
SELECT CAST("t"['b'] AS DOUBLE PRECISION) AS "v1", CAST("t"['b'] AS REAL) AS "v2", CAST("t"['c'] AS VARCHAR(10)) AS "v3" FROM "default"."T" AS "t";

--#[cast-05]
SELECT CAST("t"['c'] AS DATE) AS "v1", CAST("t"['c'] AS TIMESTAMP) AS "v2" FROM "default"."T" AS "t";

--#[concat-00]
SELECT "t"['c'] || "t"['z'] AS "v" FROM "default"."T" AS "t";

--#[arith-00]
SELECT ("t"['b'] * 2) + 1 AS "v1", -1 AS "v2", "t"['b'] % 3 AS "v3", ("t"['b'] + 1) * 2 AS "v4" FROM "default"."T" AS "t";

--#[literal-00]
SELECT DATE '2026-10-19' AS "d", TIMESTAMP '2026-10-19 12:30:00' AS "ts", 1.5 AS "n", true AS "t", NULL AS "z" FROM "default"."T" AS "t";
