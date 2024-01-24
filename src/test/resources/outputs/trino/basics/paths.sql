-- ----------------------------------------
--  Trino Array Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-16]
-- array navigation with literal (1-indexed)
SELECT "t"."array"[1] AS "v" FROM "default"."T" AS "t";
