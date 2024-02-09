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

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT "t".* EXCLUDE "t".flds.c.field_x, "t".flds.c.field_y FROM "default"."EXCLUDE_T" AS "t";

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT "t".* EXCLUDE "t".a[*].field_x FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

--#[exclude-08]
SELECT "t".* EXCLUDE "t".a[*].field_y FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t";

--#[exclude-09]
SELECT "t1".*, "t2".* EXCLUDE "t1".foo, "t2".flds.b, "t2".flds.c.field_x FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"['foo'] = "t2"['foo'];

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT "t1".*, "t2".*, "t3".* EXCLUDE "t1".flds.a, "t2".flds.b, "t3".flds.c FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"['foo'] = "t2"['foo'] AND "t2"['foo'] = "t3"['foo'];

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT "t1"['flds'] AS "flds", "t2"['flds'] AS "flds", "t3"['flds'] AS "flds" EXCLUDE "t1".flds.a, "t2".flds.b, "t3".flds.c FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"['foo'] = "t2"['foo'] AND "t2"['foo'] = "t3"['foo'];
