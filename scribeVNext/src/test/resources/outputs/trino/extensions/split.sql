--#[split-sfw-00]
SELECT "split"("T"."v", '.') AS "chars" FROM "default"."T" AS "T";

--#[split-sfw-01]
-- NOTE: 1-indexing
SELECT "split"("T"."v", '.')[1] AS "chars" FROM "default"."T" AS "T";

-- #[split-sfw-02]
-- Trino array indexing only supports integer literals, e.g. x[1]
-- SELECT "split"("T"."v", '.')[0 + 1] AS "chars" FROM "default"."T" AS "T";
