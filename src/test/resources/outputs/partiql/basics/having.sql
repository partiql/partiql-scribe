--#[having-00]
SELECT "b" AS "b" FROM "default"."T" AS "T" GROUP BY "T"['b'] HAVING count("T"['b']) > CAST(1 AS BIGINT);

--#[having-01]
SELECT "b" AS "b" FROM "default"."T" AS "T" WHERE "T"['b'] < 1 GROUP BY "T"['b'] HAVING count("T"['b']) > CAST(1 AS BIGINT);
