--#[getdate-00]
SELECT DATE_ADD(SECOND, DATE_DIFF(SECOND, CAST(TIMESTAMP '0001-01-01 00:00:00' AS TIMESTAMP WITH TIME ZONE), "utcnow"()), TIMESTAMP '0001-01-01 00:00:00') AS "v" FROM "default"."T" AS "t";

--#[sysdate-00]
SELECT CAST("utcnow"() AS TIMESTAMP) AS "v" FROM "default"."T" AS "t";

--#[sysdate-01]
SELECT EXTRACT(HOUR FROM CAST("utcnow"() AS TIMESTAMP)) AS "v" FROM "default"."T" AS "t";

--#[current-date-00]
SELECT CAST("utcnow"() AS DATE) AS "v" FROM "default"."T" AS "t";

-- --#[current-date-01] TODO(redshift)
SELECT DATE_ADD(DAY, -1, CAST("utcnow"() AS DATE)) AS "v" FROM "default"."T" AS "t";

--#[current-timestamp-00]
[UNSUPPORTED{message="CURRENT_TIMESTAMP (Redshift leader node-only function)"}];

--#[current-time-00]
[UNSUPPORTED{message="CURRENT_TIME (Redshift leader node-only function)"}];

--#[localtimestamp-00]
[UNSUPPORTED{message="LOCALTIMESTAMP (Redshift leader node-only function)"}];

--#[localtime-00]
[UNSUPPORTED{message="LOCALTIME (Redshift leader node-only function)"}];

--#[now-00]
[UNSUPPORTED{message="function `NOW`"}];

--#[timeofday-00]
[UNSUPPORTED{message="function `TIMEOFDAY`"}];
