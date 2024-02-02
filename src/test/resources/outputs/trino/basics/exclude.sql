--#[exclude-00]
SELECT * FROM (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, (SELECT "t".flds.c.field_x AS field_x, "t".flds.c.field_y AS field_y) AS c) AS "flds" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-01]
SELECT * FROM (SELECT "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-02]
SELECT * FROM (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.c.field_x AS field_x, "t".flds.c.field_y AS field_y) AS c) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-03]
SELECT * FROM (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, CAST(ROW("t".flds.c.field_y) AS ROW(field_y VARCHAR)) AS c) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-04]
SELECT * FROM (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, CAST(ROW("t".flds.c.field_y) AS ROW(field_y VARCHAR)) AS c) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

--#[exclude-05]
SELECT * FROM (SELECT (SELECT (SELECT "t".flds.a.field_x AS field_x, "t".flds.a.field_y AS field_y) AS a, (SELECT "t".flds.b.field_x AS field_x, "t".flds.b.field_y AS field_y) AS b, CAST(ROW("t".flds.c.field_x) AS ROW(field_x INTEGER)) AS c) AS "flds", "t".foo AS "foo" FROM "default"."EXCLUDE_T" AS "t") AS "t";

-- --#[exclude-06]
-- Exclude all the fields of `t.flds.c`; unsure if Trino supports ROWs with no fields. Asked a question in discussion to see if feasible https://github.com/trinodb/trino/discussions/20558
-- SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT * FROM (SELECT "transform"("t".a, coll_wildcard -> CAST(ROW(coll_wildcard.field_y) AS ROW(field_y VARCHAR))) AS "a", "t".foo AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "t";

--#[exclude-08]
SELECT * FROM (SELECT "transform"("t".a, coll_wildcard -> CAST(ROW(coll_wildcard.field_x) AS ROW(field_x INTEGER))) AS "a", "t".foo AS "foo" FROM "default"."EXCLUDE_T_COLL_WILDCARD" AS "t") AS "t";
