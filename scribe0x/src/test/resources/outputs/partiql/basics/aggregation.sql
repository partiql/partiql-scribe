--#[rel-aggregation-count-00]
SELECT COUNT(*) AS "_1" FROM "default"."T" AS "T";

--#[rel-aggregation-count-01]
SELECT count(1) AS "_1" FROM "default"."T" AS "T";

--#[rel-aggregation-count-02]
SELECT count("T"['a']) AS "_1" FROM "default"."T" AS "T";

--#[rel-aggregation-count-03]
SELECT COUNT(*) AS "_1" FROM "default"."T" AS "T" GROUP BY "T"['a'];

--#[rel-aggregation-max-00]
SELECT max("T"['b']) AS "_1" FROM "default"."T" AS "T";

--#[rel-aggregation-max-01]
SELECT max("T"['b']) AS "_1" FROM "default"."T" AS "T" GROUP BY "T"['a'];
