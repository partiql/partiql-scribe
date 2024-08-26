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
-- Exclude all the fields of `t.flds.c`; In some targets, creating an empty structure seems impossible? (e.g. Trino).
-- For this case, could consider excluding the outer struct itself (e.g. `EXCLUDE t.flds.c`).
SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT * EXCLUDE t.a[*].field_x FROM EXCLUDE_T_COLL_WILDCARD AS t;

--#[exclude-08]
SELECT * EXCLUDE t.a[*].field_y FROM EXCLUDE_T_COLL_WILDCARD AS t;

--#[exclude-09]
-- EXCLUDE with JOIN and WHERE clause
SELECT * EXCLUDE t1.foo, t2.flds.b, t2.flds.c.field_x FROM EXCLUDE_T AS t1, EXCLUDE_T AS t2 WHERE t1.foo = t2.foo;

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT * EXCLUDE t1.flds.a, t2.flds.b, t3.flds.c FROM EXCLUDE_T AS t1, EXCLUDE_T AS t2, EXCLUDE_T AS t3 WHERE t1.foo = t2.foo AND t2.foo = t3.foo;

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT t1.flds, t2.flds, t3.flds EXCLUDE t1.flds.a, t2.flds.b, t3.flds.c FROM EXCLUDE_T AS t1, EXCLUDE_T AS t2, EXCLUDE_T AS t3 WHERE t1.foo = t2.foo AND t2.foo = t3.foo;

-- EXCLUDE with different types

-- bool
--#[exclude-12]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_BOOL AS t;

-- int16
--#[exclude-13]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT16 AS t;

-- int32
--#[exclude-14]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT32 AS t;

-- int64
--#[exclude-15]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT64 AS t;

-- int (unconstrained)
--#[exclude-16]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT AS t;

-- decimal
--#[exclude-17]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_DECIMAL AS t;

-- float32
--#[exclude-18]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_FLOAT32 AS t;

-- float64
--#[exclude-19]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_FLOAT64 AS t;

-- string
--#[exclude-20]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_STRING AS t;

-- date
--#[exclude-21]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_DATE AS t;

-- time
--#[exclude-22]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_TIME AS t;

-- timestamp
--#[exclude-23]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_TIMESTAMP AS t;

-- null
--#[exclude-24]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_NULL AS t;

-- struct
--#[exclude-25]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_STRUCT AS t;

-- list
--#[exclude-26]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_LIST AS t;

-- decimal(5, 2)
--#[exclude-27]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_DECIMAL_5_2 AS t;

-- varchar(16)
--#[exclude-28]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_STRING_16 AS t;

-- char(16)
--#[exclude-29]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_CHAR_16 AS t;

-- time(6)
--#[exclude-30]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_TIME_6 AS t;

-- timestamp(6)
--#[exclude-31]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_TIMESTAMP_6 AS t;

-- union(string, null)
--#[exclude-32]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_STRING_NULL AS t;

-- union(int32, null)
--#[exclude-33]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_INT32_NULL AS t;

-- union(varchar(16), null)
--#[exclude-34]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_STRING_16_NULL AS t;

-- union(char(16), null)
--#[exclude-35]
SELECT * EXCLUDE t.foo.bar FROM datatypes.T_CHAR_16_NULL AS t;

-- Tests for EXCLUDE on top-level columns only --
-- Baseline query without `EXCLUDE`
--#[exclude-36]
SELECT * FROM T_EXCLUDE_TOP_LEVEL AS t;

-- EXCLUDE single top-level column (no `t.a`)
--#[exclude-37]
SELECT * EXCLUDE t.a FROM T_EXCLUDE_TOP_LEVEL AS t;

-- EXCLUDE multiple top-level columns (no `t.a` through `t.e`)
--#[exclude-38]
SELECT * EXCLUDE t.a, t.b, t.c, t.d, t.e FROM T_EXCLUDE_TOP_LEVEL AS t;

-- EXCLUDE top-level columns with WHERE
--#[exclude-39]
SELECT * EXCLUDE t.a, t.b FROM T_EXCLUDE_TOP_LEVEL AS t WHERE t.a AND t.c = 'remove';

-- EXCLUDE top-level columns with explicit SELECT list
--#[exclude-40]
SELECT t.c, t.d, t.e, t.f, t.g EXCLUDE t.a, t.b FROM T_EXCLUDE_TOP_LEVEL AS t WHERE t.a AND t.c = 'remove';

-- EXCLUDE top-level with subquery
--#[exclude-41]
SELECT subq.* EXCLUDE subq.remove_me FROM (SELECT t.*, 'foo' AS remove_me FROM T_EXCLUDE_TOP_LEVEL AS t WHERE t.a) AS subq;

-- EXCLUDE top-level columns with JOIN
--#[exclude-42]
SELECT * EXCLUDE t1.a, t2.b, t1.c, t2.d, t1.e, t2.f, t1.g FROM T_EXCLUDE_TOP_LEVEL AS t1, T_EXCLUDE_TOP_LEVEL AS t2;

-- EXCLUDE top-level columns with JOIN and WHERE
--#[exclude-43]
SELECT * EXCLUDE t1.a, t2.b, t1.c, t2.d, t1.e, t2.f, t1.g FROM T_EXCLUDE_TOP_LEVEL AS t1, T_EXCLUDE_TOP_LEVEL AS t2 WHERE t1.a AND t2.a;

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT element
--#[exclude-44]
SELECT t1.* EXCLUDE t1.a, t2.b, t1.c, t2.d, t1.e, t2.f, t1.g FROM T_EXCLUDE_TOP_LEVEL AS t1, T_EXCLUDE_TOP_LEVEL AS t2 WHERE t1.a AND t2.a;

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT elements
--#[exclude-45]
SELECT t1.*, t2.a AS special EXCLUDE t1.a, t2.b, t1.c, t2.d, t1.e, t2.f, t1.g FROM T_EXCLUDE_TOP_LEVEL AS t1, T_EXCLUDE_TOP_LEVEL AS t2 WHERE t1.a AND t2.a;

-- EXCLUDE top-level columns with multiple JOINs
--#[exclude-46]
SELECT *
EXCLUDE
          t1.b, t1.c, t1.d, t1.e, t1.f, t1.g,
    t2.a,       t2.c, t2.d, t2.e, t2.f, t2.g,
    t3.a, t3.b,       t3.d, t3.e, t3.f, t3.g,
    t4.a, t4.b, t4.c,       t4.e, t4.f, t4.g,
    t5.a, t5.b, t5.c, t5.d,       t5.f, t5.g,
    t6.a, t6.b, t6.c, t6.d, t6.e,       t6.g,
    t7.a, t7.b, t7.c, t7.d, t7.e, t7.f
FROM T_EXCLUDE_TOP_LEVEL AS t1 JOIN
    T_EXCLUDE_TOP_LEVEL AS t2 ON t2.a LEFT JOIN
    T_EXCLUDE_TOP_LEVEL AS t3 ON t3.a INNER JOIN
    T_EXCLUDE_TOP_LEVEL AS t4 ON t4.a RIGHT JOIN
    T_EXCLUDE_TOP_LEVEL AS t5 ON t5.a FULL JOIN
    T_EXCLUDE_TOP_LEVEL AS t6 ON t6.a,
    T_EXCLUDE_TOP_LEVEL AS t7
WHERE
    t1.a AND t2.a;

-- EXCLUDE all nested fields of a struct
--#[exclude-47]
SELECT * EXCLUDE t.flds.a.field_x, t.flds.a.field_y, t.flds.b.field_x, t.flds.b.field_y, t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t;

-- EXCLUDE all fields of a top-level struct column
--#[exclude-48]
SELECT * EXCLUDE t.flds.a, t.flds.b, t.flds.c FROM EXCLUDE_T AS t;

--#[exclude-49]
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT * EXCLUDE t.flds.b, t.flds.c.field_x FROM EXCLUDE_T_NULLABLE AS t;
