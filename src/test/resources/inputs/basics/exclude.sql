--#[exclude-00]
-- Exclude a top-level field
SELECT * EXCLUDE t.foo FROM EXCLUDE_T AS t;

--#[exclude-01]
SELECT * EXCLUDE t.flds FROM EXCLUDE_T AS t;

--#[exclude-02]
SELECT * EXCLUDE t.flds.b FROM EXCLUDE_T AS t;

--#[exclude-03]
SELECT * EXCLUDE t.flds.c.field_x FROM EXCLUDE_T AS t;

--#[exclude-04]
-- Exclude two nested fields
SELECT * EXCLUDE t.flds.b, t.flds.c.field_x FROM EXCLUDE_T AS t;

--#[exclude-05]
SELECT * EXCLUDE t.flds.c.field_y FROM EXCLUDE_T AS t;

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`; In some targets, creating an empty structure seems tricky or not possible? (e.g. Trino, Spark).
-- Could consider disallowing empty structs?
SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT * EXCLUDE t.a[*].field_x FROM EXCLUDE_T_COLL_WILDCARD AS t;

--#[exclude-08]
SELECT * EXCLUDE t.a[*].field_y FROM EXCLUDE_T_COLL_WILDCARD AS t;
