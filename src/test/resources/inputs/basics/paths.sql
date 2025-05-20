-- ----------------------------------------
--  PartiQL Path Navigation
-- ----------------------------------------

-- T1 -> Any

--#[paths-00]
-- tuple navigation
t1.y;

--#[paths-01]
-- array navigation with literal
t1[0];
    
--#[paths-02]
-- tuple navigation with array notation
t1['y'];

--#[paths-03]
-- tuple navigation (2)
t1."y";

--#[paths-04]
-- tuple navigation with explicit cast as string
t1[CAST(str AS STRING)];

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-05]
t1.y[0]['y'];

--#[paths-06]
t1.y[0]."y";

--#[paths-07]
t1.y[0][CAST(str AS STRING)];

--#[paths-08]
t1.y['y']."y";

--#[paths-09]
t1.y['y'][CAST(str AS STRING)];

--#[paths-10]
t1.y."y"[CAST(str AS STRING)];

--#[paths-11]
t1[0]['y']."y";

--#[paths-12]
t1[0]['y'][CAST(str AS STRING)];

--#[paths-13]
t1[0]."y"[CAST(str AS STRING)];

--#[paths-14]
t1['y']."y"[CAST(str AS STRING)];

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-15]
t1[0+1];

--#[paths-16]
-- abs function not supported in the header
t1[ABS(1)];

-- ----------------------------------------
--  PartiQL Path Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-00]
-- tuple navigation
SELECT t.x.y AS v FROM t;

--#[paths-sfw-01]
-- array navigation with literal
SELECT t.x[0] AS v FROM t;

--#[paths-sfw-02]
-- tuple navigation with array notation (1)
SELECT t.x['y'] AS v FROM t;

--#[paths-sfw-03]
-- tuple navigation with array notation (2)
SELECT t.x."y" AS v FROM t;

--#[paths-sfw-04]
-- tuple navigation with explicit cast as string
SELECT t.x[CAST(t.z AS STRING)] AS v FROM t;

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-sfw-05]
SELECT t.x.y[0]['y'] AS v FROM t;

--#[paths-sfw-06]
SELECT t.x.y[0]."y" AS v FROM t;

--#[paths-sfw-07]
SELECT t.x.y[0][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-08]
SELECT t.x.y['y']."y" AS v FROM t;

--#[paths-sfw-09]
SELECT t.x.y['y'][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-10]
SELECT t.x.y."y"[CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-11]
SELECT t.x[0]['y']."y" AS v FROM t;

--#[paths-sfw-12]
SELECT t.x[0]['y'][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-13]
SELECT t.x[0]."y"[CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-14]
SELECT t.x['y']."y"[CAST(t.z AS STRING)] AS v FROM t;

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-sfw-15]
SELECT t.x[0 + 1] AS v FROM t;

-- #[paths-sfw-16]
-- TODO: ABS not implemented yet
-- SELECT t.x[ABS(1)] AS v FROM t;

--#[paths-sfw-16]
SELECT t."array"[0] AS v FROM t;

-- Additional path exprs
--#[paths-sfw-17]
SELECT flds.a AS v FROM EXCLUDE_T AS t;

--#[paths-sfw-18]
SELECT t.flds.a AS v FROM EXCLUDE_T AS t;

--#[paths-sfw-19]
SELECT t.flds.a.field_x AS v FROM EXCLUDE_T AS t;

-- qualified path
--#[paths-sfw-20]
SELECT "t"."flds"."a"."field_x" AS v FROM EXCLUDE_T AS t;

-- nullable fields
--#[paths-sfw-21]
SELECT t.flds.a.field_x AS v FROM EXCLUDE_T AS t;

-- nullable fields + qualified path
--#[paths-sfw-22]
SELECT "t"."flds"."a"."field_x" AS v FROM EXCLUDE_T AS t;

-- nullable fields + mix of qualified and unqualified path components
--#[paths-sfw-23]
SELECT "t".flds."a".field_x AS v FROM EXCLUDE_T AS t;
