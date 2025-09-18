--#[groupby-00]
SELECT "a_alias" AS "a" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias";

--#[groupby-01]
SELECT "a_alias" AS "a", max("T"."b") AS "agg1" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias";

--#[groupby-02]
SELECT "a_alias" AS "a", max("T"."b") AS "agg1", count("T"."c") AS "agg2" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias";

--#[groupby-03]
SELECT "a_alias" AS "a", "b_alias" AS "b", max("T"."b") AS "agg1", count("T"."c") AS "agg2" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias", "T"."b" AS "b_alias";

--#[groupby-04]
SELECT max("T"."b") AS "agg1", count("T"."c") AS "agg2", "a_alias" AS "a", "b_alias" AS "b" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias", "T"."b" AS "b_alias";

--#[groupby-05]
SELECT max("T"."b") AS "agg1", count("T"."c") AS "agg2", "b_alias" AS "b", "a_alias" AS "a" FROM "default"."T" AS "T" GROUP BY "T"."a" AS "a_alias", "T"."b" AS "b_alias";
