-- Stores the input query for basic testing of a single query
-- See resources/outputs/sandbox/sandbox/sandbox.sql for the expected output
--#[sandbox-00]
SELECT "t"."flds".a.field_x AS v FROM EXCLUDE_T AS t;
-- SELECT t."array"[0] AS v FROM t WHERE true;
-- SELECT t.foo, t.foo.keep, t.foo.keep = 1 FROM datatypes.T_TIMESTAMP AS t;
