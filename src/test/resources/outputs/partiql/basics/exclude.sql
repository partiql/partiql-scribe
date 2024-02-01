--#[exclude-00]
-- Exclude a top-level field
SELECT "t".* EXCLUDE "t".foo FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-01]
SELECT "t".* EXCLUDE "t".flds FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-02]
SELECT "t".* EXCLUDE "t".flds.b FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-03]
SELECT "t".* EXCLUDE "t".flds.c.field_x FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-04]
-- Exclude two nested fields
SELECT "t".* EXCLUDE "t".flds.b, "t".flds.c.field_x FROM "default"."EXCLUDE_T" AS "t";

--#[exclude-05]
SELECT "t".* EXCLUDE "t".flds.c.field_y FROM "default"."EXCLUDE_T" AS "t";
