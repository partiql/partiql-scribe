-- --#[nvl-00] TODO(redshift)
SELECT COALESCE("t"['c'], 'none') AS "v" FROM "default"."T" AS "t";

-- --#[coalesce-00] TODO(redshift)
SELECT COALESCE("t"['c'], "t"['z'], 'none') AS "v" FROM "default"."T" AS "t";

-- --#[nullif-00] TODO(redshift)
SELECT NULLIF("t"['b'], 0) AS "v" FROM "default"."T" AS "t";

-- --#[len-00] TODO(redshift)
SELECT CHAR_LENGTH("t"['c']) AS "v" FROM "default"."T" AS "t";

-- --#[upper-00] TODO(redshift)
SELECT "upper"("t"['c']) AS "v1", "lower"("t"['z']) AS "v2" FROM "default"."T" AS "t";

--#[dateadd-00]
SELECT DATE_ADD(DAY, 5, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[dateadd-01]
SELECT DATE_ADD(MONTH, 1, "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[datediff-00]
SELECT DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_2']) - DATE_DIFF(HOUR, TIMESTAMP '0001-01-01 00:00:00', "t"['timestamp_1']) AS "v" FROM "default"."T" AS "t";

--#[getdate-00]
SELECT DATE_ADD(SECOND, DATE_DIFF(SECOND, CAST(TIMESTAMP '0001-01-01 00:00:00' AS TIMESTAMP WITH TIME ZONE), "utcnow"()), TIMESTAMP '0001-01-01 00:00:00') AS "v" FROM "default"."T" AS "t";

--#[sysdate-00]
SELECT CAST("utcnow"() AS TIMESTAMP) AS "v" FROM "default"."T" AS "t";

--#[current-date-00]
SELECT CAST("utcnow"() AS DATE) AS "v" FROM "default"."T" AS "t";
