-- Delimited identifiers whose text contains the target's identifier quote character (`"`, or a backtick for Spark).
-- The quote must be doubled in the output so the identifier text cannot end the identifier early.

--#[quoted-identifiers-00]
-- path step with an embedded double-quote
SELECT t."a""b" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-01]
-- path step with an embedded backtick
SELECT t."c`d" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-02]
-- SELECT alias with embedded quote characters
SELECT t."a""b" AS "p""q`r" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-03]
-- FROM alias with embedded quote characters
SELECT "t""1`2"."a""b" FROM T_QUOTED_FIELDS AS "t""1`2";

--#[quoted-identifiers-04]
-- field name with a double-quote followed by other punctuation
SELECT t."e""),f" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-05]
-- nested path steps with embedded quote characters
SELECT t."s""t"."u""v", t."s""t"."w`x" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-06]
-- GROUP BY key alias with embedded quote characters; the planner replaces the alias with the key expression
SELECT "k""1`2" FROM T_QUOTED_FIELDS AS t GROUP BY t."a""b" AS "k""1`2";

--#[quoted-identifiers-07]
-- WITH query name with embedded quote characters
WITH "w""1`2" AS (SELECT t."a""b" FROM T_QUOTED_FIELDS AS t) SELECT * FROM "w""1`2";

--#[quoted-identifiers-08]
-- EXCLUDE of a nested field; the remaining fields have embedded quote characters
SELECT * EXCLUDE t."s""t"."u""v" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-09]
-- EXCLUDE of a nested field; the remaining field has an embedded double-quote
SELECT * EXCLUDE t."s""t"."w`x" FROM T_QUOTED_FIELDS AS t;

--#[quoted-identifiers-10]
-- LEFT JOIN lateral alias with embedded quote characters
SELECT E.a, "x""1`" FROM T AS E LEFT JOIN E."array" AS "x""1`" ON true;

--#[quoted-identifiers-11]
-- INNER JOIN lateral alias with embedded quote characters
SELECT "x""1`" FROM T AS E INNER JOIN E."array" AS "x""1`" ON true;

--#[quoted-identifiers-12]
-- comma (cross join) lateral alias with embedded quote characters
SELECT "x""1`" FROM T AS E, E."array" AS "x""1`";

--#[quoted-identifiers-13]
-- SELECT alias with an embedded backtick
SELECT t.c AS "a`b" FROM T AS t;

--#[quoted-identifiers-14]
-- SELECT alias with an embedded double-quote
SELECT t.c AS "a""b" FROM T AS t;

--#[quoted-identifiers-15]
-- path key with an embedded backtick
SELECT t.x['a`b'] AS v FROM T AS t;

--#[quoted-identifiers-16]
-- path key with an embedded double-quote
SELECT t.x['a"b'] AS v FROM T AS t;

--#[quoted-identifiers-17]
-- subquery alias and an outer reference to a field with an embedded backtick
SELECT v."a`b" FROM (SELECT t.c AS "a`b" FROM T AS t) AS v;
