--#[split-00]
"split"("default"."STR", "default"."STR");

--#[split-01]
"split"("default"."STR", "default"."STR")[0];

--#[split-02]
"split"("default"."STR", "default"."STR")[0];

--#[split-sfw-00]
SELECT "split"("T"['v'], '.') AS "chars" FROM "default"."T" AS "T";

--#[split-sfw-01]
SELECT "split"("T"['v'], '.')[0] AS "chars" FROM "default"."T" AS "T";

--#[split-sfw-02]
SELECT "split"("T"['v'], '.')[0 + 1] AS "chars" FROM "default"."T" AS "T";
