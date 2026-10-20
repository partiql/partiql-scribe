--#[cast-00]
SELECT b::bigint AS v FROM T;

-- VARCHAR without a length is VARCHAR(256) in Redshift.
--#[cast-01]
SELECT CAST(b AS VARCHAR) AS v FROM T;

-- DECIMAL without parameters is DECIMAL(18, 0) in Redshift.
--#[cast-02]
SELECT b::decimal(10, 2) AS v1, b::numeric AS v2 FROM T;

-- '::' binds more tightly than the unary minus.
--#[cast-03]
SELECT -b::int AS v FROM T;

--#[cast-04]
SELECT b::double precision AS v1, b::float4 AS v2, c::character varying(10) AS v3 FROM T;

--#[cast-05]
SELECT CAST(c AS DATE) AS v1, c::timestamp AS v2 FROM T;

--#[concat-00]
SELECT c || z AS v FROM T;

--#[arith-00]
SELECT b * 2 + 1 AS v1, -1 AS v2, b % 3 AS v3, (b + 1) * 2 AS v4 FROM T;

--#[literal-00]
SELECT DATE '2026-10-19' AS d, TIMESTAMP '2026-10-19 12:30:00' AS ts, 1.5 AS n, TRUE AS t, NULL AS z FROM T;
