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
