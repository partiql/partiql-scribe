-- ----------------------------------------
--  PartiQL Path Navigation
-- ----------------------------------------

--#[paths-00]
-- tuple navigation
"default"."T1".y;

--#[paths-01]
-- array navigation with literal
"default"."T1"[0];
    
--#[paths-02]
-- tuple navigation with array notation (1)
"default"."T1"['y'];

--#[paths-03]
-- tuple navigation with array notation (2)
"default"."T1"['y'];

--#[paths-04]
-- tuple navigation with explicit cast as string
"default"."T1"[CAST("default"."STR" AS STRING)];

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-05]
"default"."T1".y[0]['y'];

--#[paths-06]
"default"."T1".y[0]['y'];

--#[paths-07]
"default"."T1".y[0][CAST("default"."STR" AS STRING)];

--#[paths-08]
"default"."T1".y['y']['y'];

--#[paths-09]
"default"."T1".y['y'][CAST("default"."STR" AS STRING)];

--#[paths-10]
"default"."T1".y['y'][CAST("default"."STR" AS STRING)];

--#[paths-11]
"default"."T1"[0]['y']['y'];

--#[paths-12]
"default"."T1"[0]['y'][CAST("default"."STR" AS STRING)];

--#[paths-13]
"default"."T1"[0]['y'][CAST("default"."STR" AS STRING)];

--#[paths-14]
"default"."T1"['y']['y'][CAST("default"."STR" AS STRING)];

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-15]
"default"."T1"[0 + 1];

--#[paths-16] TODO FIX ME, ABS is not yet supported in the PartiQL header
-- "default"."T1"[ABS(1)];

-- ----------------------------------------
--  PartiQL Path Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-00]
-- tuple navigation
SELECT "t"['x'].y AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-01]
-- array navigation with literal
SELECT "t"['x'][0] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-02]
-- tuple navigation with array notation (1)
SELECT "t"['x']['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-03]
-- tuple navigation with array notation (2)
SELECT "t"['x']['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-04]
-- tuple navigation with explicit cast as string
SELECT "t"['x'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-sfw-05]
SELECT "t"['x'].y[0]['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-06]
SELECT "t"['x'].y[0]['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-07]
SELECT "t"['x'].y[0][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-08]
SELECT "t"['x'].y['y']['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-09]
SELECT "t"['x'].y['y'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-10]
SELECT "t"['x'].y['y'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-11]
SELECT "t"['x'][0]['y']['y'] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-12]
SELECT "t"['x'][0]['y'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-13]
SELECT "t"['x'][0]['y'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-14]
SELECT "t"['x']['y']['y'][CAST("t"['z'] AS STRING)] AS "v" FROM "default"."T" AS "t";

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-sfw-15]
SELECT "t"['x'][0 + 1] AS "v" FROM "default"."T" AS "t";

--#[paths-sfw-16] TODO: ABS not yet implemented in the header.
-- SELECT "t".x[ABS(1)] AS "v" FROM "default"."T" AS "t";
