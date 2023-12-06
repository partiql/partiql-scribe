-- ----------------------------------------
--  Trino Array Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-01]
-- array navigation with literal (1-indexed)
SELECT "t".x[1] AS "v" FROM "default"."T" AS "t";
