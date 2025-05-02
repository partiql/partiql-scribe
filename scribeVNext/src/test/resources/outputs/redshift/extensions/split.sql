--#[split-sfw-00]
SELECT split_to_array("T"."v", '.') AS "chars" FROM "default"."T" AS "T";

--#[split-sfw-01]
SELECT split_to_array("T"."v", '.')[0] AS "chars" FROM "default"."T" AS "T";

-- #[split-sfw-02]
-- ERROR: Redshift path step must an integer literal, e.g. x[0].
-- SELECT split_to_array("T"."v", '.')[0 + 1] AS "chars" FROM "default"."T" AS "T";
