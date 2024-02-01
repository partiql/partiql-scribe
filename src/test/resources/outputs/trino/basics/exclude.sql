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
