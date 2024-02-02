--#[exclude-00]
-- Exclude a top-level field
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x, 'field_y', "t".flds.c.field_y)) AS "flds" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-01]
SELECT "t".* FROM (SELECT "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-02]
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x, 'field_y', "t".flds.c.field_y)) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-03]
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_y', "t".flds.c.field_y)) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-04]
-- Exclude two nested fields
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'c', OBJECT('field_y', "t".flds.c.field_y)) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-05]
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x)) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT "t".* FROM (SELECT OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT()) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

-- START OF EXCLUDE with COLLECTION WILDCARD
-- --#[exclude-07]
-- SELECT * EXCLUDE t.a[*].field_x FROM EXCLUDE_T_COLL_WILDCARD AS t;

-- --#[exclude-08]
-- SELECT * EXCLUDE t.a[*].field_y FROM EXCLUDE_T_COLL_WILDCARD AS t;
