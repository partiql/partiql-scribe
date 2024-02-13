--#[exclude-00]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT CAST(ROW((SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, (SELECT "t".flds.c.field_x AS field_x, "t".flds.c.field_y AS field_y) AS c)) AS ROW(flds ROW(a ROW(field_x INTEGER, field_y VARCHAR), b ROW(field_x INTEGER, field_y VARCHAR), c ROW(field_x INTEGER, field_y VARCHAR)))) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-01]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT CAST(ROW("t".foo) AS ROW(foo VARCHAR)) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-02]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.c.field_x AS field_x, "t".flds.c.field_y AS field_y) AS c) AS flds, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-03]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, CAST(ROW("t".flds.c.field_y) AS ROW(field_y VARCHAR)) AS c) AS flds, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-04]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, CAST(ROW("t".flds.c.field_y) AS ROW(field_y VARCHAR)) AS c) AS flds, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-05]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, CAST(ROW("t".flds.c.field_x) AS ROW(field_x INTEGER)) AS c) AS flds, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T" AS "t") AS "$__EXCLUDE_ALIAS__";

-- --#[exclude-06]
-- Exclude all the fields of `t.flds.c`; unsure if Trino supports ROWs with no fields. Asked a question in discussion to see if feasible https://github.com/trinodb/trino/discussions/20558
-- Can just `EXCLUDE t.flds.c` rather than excluding all the fields individually
-- SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT transform("t".a, ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___.field_y) AS ROW(field_y VARCHAR))) AS a, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-08]
SELECT "$__EXCLUDE_ALIAS__".t.* FROM (SELECT (SELECT transform("t".a, ___coll_wildcard___ -> CAST(ROW(___coll_wildcard___.field_x) AS ROW(field_x INTEGER))) AS a, "t".foo AS foo) AS "t" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "$__EXCLUDE_ALIAS__";

--#[exclude-09]
SELECT "$__EXCLUDE_ALIAS__".t1.*, "$__EXCLUDE_ALIAS__".t2.* FROM (SELECT CAST(ROW((SELECT (SELECT "t1".flds.a.field_x AS field_x, "t1".flds.a.field_y AS field_y) AS a, (SELECT "t1".flds.b.field_x AS field_x, "t1".flds.b.field_y AS field_y) AS b, (SELECT "t1".flds.c.field_x AS field_x, "t1".flds.c.field_y AS field_y) AS c)) AS ROW(flds ROW(a ROW(field_x INTEGER, field_y VARCHAR), b ROW(field_x INTEGER, field_y VARCHAR), c ROW(field_x INTEGER, field_y VARCHAR)))) AS "t1", (SELECT (SELECT (SELECT "t2".flds.a.field_x AS field_x, "t2".flds.a.field_y AS field_y) AS a, CAST(ROW("t2".flds.c.field_y) AS ROW(field_y VARCHAR)) AS c) AS flds, "t2".foo AS foo) AS "t2" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true WHERE "t1"."foo" = "t2"."foo") AS "$__EXCLUDE_ALIAS__";

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT "$__EXCLUDE_ALIAS__".t1.*, "$__EXCLUDE_ALIAS__".t2.*, "$__EXCLUDE_ALIAS__".t3.* FROM (SELECT (SELECT (SELECT (SELECT "t1".flds.b.field_x AS field_x, "t1".flds.b.field_y AS field_y) AS b, (SELECT "t1".flds.c.field_x AS field_x, "t1".flds.c.field_y AS field_y) AS c) AS flds, "t1".foo AS foo) AS "t1", (SELECT (SELECT (SELECT "t2".flds.a.field_x AS field_x, "t2".flds.a.field_y AS field_y) AS a, (SELECT "t2".flds.c.field_x AS field_x, "t2".flds.c.field_y AS field_y) AS c) AS flds, "t2".foo AS foo) AS "t2", (SELECT (SELECT (SELECT "t3".flds.a.field_x AS field_x, "t3".flds.a.field_y AS field_y) AS a, (SELECT "t3".flds.b.field_x AS field_x, "t3".flds.b.field_y AS field_y) AS b) AS flds, "t3".foo AS foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"."foo" = "t2"."foo" AND "t2"."foo" = "t3"."foo") AS "$__EXCLUDE_ALIAS__";

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT "$__EXCLUDE_ALIAS__".t1."flds" AS "flds", "$__EXCLUDE_ALIAS__".t2."flds" AS "flds", "$__EXCLUDE_ALIAS__".t3."flds" AS "flds" FROM (SELECT (SELECT (SELECT (SELECT "t1".flds.b.field_x AS field_x, "t1".flds.b.field_y AS field_y) AS b, (SELECT "t1".flds.c.field_x AS field_x, "t1".flds.c.field_y AS field_y) AS c) AS flds, "t1".foo AS foo) AS "t1", (SELECT (SELECT (SELECT "t2".flds.a.field_x AS field_x, "t2".flds.a.field_y AS field_y) AS a, (SELECT "t2".flds.c.field_x AS field_x, "t2".flds.c.field_y AS field_y) AS c) AS flds, "t2".foo AS foo) AS "t2", (SELECT (SELECT (SELECT "t3".flds.a.field_x AS field_x, "t3".flds.a.field_y AS field_y) AS a, (SELECT "t3".flds.b.field_x AS field_x, "t3".flds.b.field_y AS field_y) AS b) AS flds, "t3".foo AS foo) AS "t3" FROM "default"."EXCLUDE_T" AS "t1" INNER JOIN "default"."EXCLUDE_T" AS "t2" ON true INNER JOIN "default"."EXCLUDE_T" AS "t3" ON true WHERE "t1"."foo" = "t2"."foo" AND "t2"."foo" = "t3"."foo") AS "$__EXCLUDE_ALIAS__";
