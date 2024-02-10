--#[exclude-00]
-- Exclude a top-level field
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x, 'field_y', "t".flds.c.field_y))) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-01]
SELECT "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-02]
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds", "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x, 'field_y', "t".flds.c.field_y)), 'foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-03]
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds", "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_y', "t".flds.c.field_y)), 'foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-04]
-- Exclude two nested fields
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds", "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'c', OBJECT('field_y', "t".flds.c.field_y)), 'foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-05]
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds", "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT('field_x', "t".flds.c.field_x)), 'foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT "$__EXCLUDE_ALIAS__".t.flds AS "flds", "$__EXCLUDE_ALIAS__".t.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t".flds.a.field_x, 'field_y', "t".flds.a.field_y), 'b', OBJECT('field_x', "t".flds.b.field_x, 'field_y', "t".flds.b.field_y), 'c', OBJECT()), 'foo', "t".foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

-- START OF EXCLUDE with COLLECTION WILDCARD
-- COLLECTION WILDCARD is not supported for Redshift target
-- --#[exclude-07]
-- SELECT * EXCLUDE t.a[*].field_x FROM EXCLUDE_T_COLL_WILDCARD AS t;

-- --#[exclude-08]
-- SELECT * EXCLUDE t.a[*].field_y FROM EXCLUDE_T_COLL_WILDCARD AS t;

--#[exclude-09]
-- EXCLUDE with JOIN and WHERE clause
SELECT "$__EXCLUDE_ALIAS__".t1.flds AS "flds", "$__EXCLUDE_ALIAS__".t2.flds AS "flds", "$__EXCLUDE_ALIAS__".t2.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('a', OBJECT('field_x', "t1".flds.a.field_x, 'field_y', "t1".flds.a.field_y), 'b', OBJECT('field_x', "t1".flds.b.field_x, 'field_y', "t1".flds.b.field_y), 'c', OBJECT('field_x', "t1".flds.c.field_x, 'field_y', "t1".flds.c.field_y))) AS "t1", OBJECT('flds', OBJECT('a', OBJECT('field_x', "t2".flds.a.field_x, 'field_y', "t2".flds.a.field_y), 'c', OBJECT('field_y', "t2".flds.c.field_y)), 'foo', "t2".foo) AS "t2" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo") AS "$__EXCLUDE_ALIAS__";

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT "$__EXCLUDE_ALIAS__".t1.flds AS "flds", "$__EXCLUDE_ALIAS__".t1.foo AS "foo", "$__EXCLUDE_ALIAS__".t2.flds AS "flds", "$__EXCLUDE_ALIAS__".t2.foo AS "foo", "$__EXCLUDE_ALIAS__".t3.flds AS "flds", "$__EXCLUDE_ALIAS__".t3.foo AS "foo" FROM (SELECT OBJECT('flds', OBJECT('b', OBJECT('field_x', "t1".flds.b.field_x, 'field_y', "t1".flds.b.field_y), 'c', OBJECT('field_x', "t1".flds.c.field_x, 'field_y', "t1".flds.c.field_y)), 'foo', "t1".foo) AS "t1", OBJECT('flds', OBJECT('a', OBJECT('field_x', "t2".flds.a.field_x, 'field_y', "t2".flds.a.field_y), 'c', OBJECT('field_x', "t2".flds.c.field_x, 'field_y', "t2".flds.c.field_y)), 'foo', "t2".foo) AS "t2", OBJECT('flds', OBJECT('a', OBJECT('field_x', "t3".flds.a.field_x, 'field_y', "t3".flds.a.field_y), 'b', OBJECT('field_x', "t3".flds.b.field_x, 'field_y', "t3".flds.b.field_y)), 'foo', "t3".foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"."foo" = "t2"."foo" AND "t2"."foo" = "t3"."foo") AS "$__EXCLUDE_ALIAS__";

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT "$__EXCLUDE_ALIAS__".t1."flds" AS "flds", "$__EXCLUDE_ALIAS__".t2."flds" AS "flds", "$__EXCLUDE_ALIAS__".t3."flds" AS "flds" FROM (SELECT OBJECT('flds', OBJECT('b', OBJECT('field_x', "t1".flds.b.field_x, 'field_y', "t1".flds.b.field_y), 'c', OBJECT('field_x', "t1".flds.c.field_x, 'field_y', "t1".flds.c.field_y)), 'foo', "t1".foo) AS "t1", OBJECT('flds', OBJECT('a', OBJECT('field_x', "t2".flds.a.field_x, 'field_y', "t2".flds.a.field_y), 'c', OBJECT('field_x', "t2".flds.c.field_x, 'field_y', "t2".flds.c.field_y)), 'foo', "t2".foo) AS "t2", OBJECT('flds', OBJECT('a', OBJECT('field_x', "t3".flds.a.field_x, 'field_y', "t3".flds.a.field_y), 'b', OBJECT('field_x', "t3".flds.b.field_x, 'field_y', "t3".flds.b.field_y)), 'foo', "t3".foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"."foo" = "t2"."foo" AND "t2"."foo" = "t3"."foo") AS "$__EXCLUDE_ALIAS__";
