-- --#[cast-00] TODO(redshift)
SELECT CAST("t"['b'] AS BIGINT) AS "v" FROM "default"."T" AS "t";

-- --#[cast-01] TODO(redshift)
SELECT CAST("t"['b'] AS STRING) AS "v" FROM "default"."T" AS "t";

-- --#[cast-02] TODO(redshift)
SELECT CAST("t"['b'] AS DECIMAL(10,2)) AS "v1", CAST("t"['b'] AS DECIMAL(18,0)) AS "v2" FROM "default"."T" AS "t";

-- --#[cast-03] TODO(redshift)
SELECT -CAST("t"['b'] AS INT) AS "v" FROM "default"."T" AS "t";

-- --#[cast-04] TODO(redshift)
[UNSUPPORTED{message="CAST to VARCHAR(10) that can truncate a character string"}];

-- --#[cast-05] TODO(redshift)
[UNSUPPORTED{message="CAST of a character string to DATE"}];

-- --#[concat-00] TODO(redshift)
SELECT "t"['c'] || "t"['z'] AS "v" FROM "default"."T" AS "t";

-- --#[arith-00] TODO(redshift)
SELECT ("t"['b'] * 2) + 1 AS "v1", -1 AS "v2", "t"['b'] % 3 AS "v3", ("t"['b'] + 1) * 2 AS "v4" FROM "default"."T" AS "t";

-- --#[literal-00] TODO(redshift)
SELECT DATE '2026-10-19' AS "d", TIMESTAMP '2026-10-19 12:30:00' AS "ts", 1.5 AS "n", true AS "t", NULL AS "z" FROM "default"."T" AS "t";
