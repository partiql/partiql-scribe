-- Reproduce: UNION ALL with inline struct literal path access
-- The inline struct literal `{'x': t1.b, 'y': t1.c}."x"` should be constant-folded to `t1.b`

-- Basic case: struct literal path access in first leg of UNION ALL
--#[trino-sandbox-00]
SELECT {'x': t1.b, 'y': t1.c}."x" AS a FROM SIMPLE_T AS t1 UNION ALL SELECT t2.b AS a FROM SIMPLE_T AS t2;

-- Selecting the LAST field (not the first) to verify correct field matching
--#[trino-sandbox-01]
SELECT {'x': t1.b, 'y': t1.c}."y" AS a FROM SIMPLE_T AS t1 UNION ALL SELECT t2.c AS a FROM SIMPLE_T AS t2;

-- Struct literal with only literal values (no column references)
--#[trino-sandbox-02]
SELECT {'x': 1, 'y': 'hello'}."y" AS a FROM SIMPLE_T AS t1 UNION ALL SELECT t2.c AS a FROM SIMPLE_T AS t2;

-- Multiple struct literal path accesses in the same SELECT
--#[trino-sandbox-03]
SELECT {'x': t1.b, 'y': t1.c}."x" AS a, {'p': t1.c, 'q': t1.a}."p" AS b FROM SIMPLE_T AS t1 UNION ALL SELECT t2.b AS a, t2.c AS b FROM SIMPLE_T AS t2;

-- Struct literal path access WITHOUT UNION ALL (plain SELECT)
--#[trino-sandbox-04]
SELECT {'x': t1.b, 'y': t1.c}."x" AS a FROM SIMPLE_T AS t1;

-- Nested: struct literal resolves to a struct column, then path into that
--#[trino-sandbox-05]
SELECT {'inner': t1.d}."inner"."e" AS a FROM T AS t1 UNION ALL SELECT t2.c AS a FROM SIMPLE_T AS t2;
