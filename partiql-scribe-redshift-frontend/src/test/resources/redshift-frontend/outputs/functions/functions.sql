--#[nvl-00]
SELECT COALESCE("t"['c'], 'none') AS "v" FROM "default"."T" AS "t";

--#[coalesce-00]
SELECT COALESCE("t"['c'], "t"['z'], 'none') AS "v" FROM "default"."T" AS "t";

--#[nullif-00]
SELECT NULLIF("t"['b'], 0) AS "v" FROM "default"."T" AS "t";

--#[len-00]
SELECT CHAR_LENGTH("t"['c']) AS "v" FROM "default"."T" AS "t";

--#[upper-00]
SELECT "upper"("t"['c']) AS "v1", "lower"("t"['z']) AS "v2" FROM "default"."T" AS "t";

--#[dateadd-00]
SELECT DATE_ADD(DAY, 5, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[dateadd-01]
SELECT DATE_ADD(MONTH, 1, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[datediff-00]
SELECT DATE_DIFF(HOUR, "t"['timestamp_1'], "t"['timestamp_2']) AS "v" FROM "default"."T" AS "t";

--#[getdate-00]
SELECT "utcnow"() AS "v" FROM "default"."T" AS "t";

--#[sysdate-00]
SELECT "utcnow"() AS "v" FROM "default"."T" AS "t";

--#[current-date-00]
SELECT CURRENT_DATE AS "v" FROM "default"."T" AS "t";
