--#[time-cast-00]
SELECT CAST("t"['c'] AS TIME (6)) AS "v1", CAST("t"['c'] AS TIME (6) WITH TIME ZONE) AS "v2", CAST("t"['c'] AS TIME (6) WITH TIME ZONE) AS "v3" FROM "default"."T" AS "t";

--#[timestamp-cast-00]
SELECT CAST("t"['c'] AS TIMESTAMP (6) WITH TIME ZONE) AS "v1", CAST("t"['c'] AS TIMESTAMP (6) WITH TIME ZONE) AS "v2", CAST("t"['c'] AS TIMESTAMP) AS "v3" FROM "default"."T" AS "t";

--#[time-literal-00]
SELECT TIME '12:30:45' AS "v1", TIME '12:30:45.123456' AS "v2", TIME WITH TIME ZONE '12:30:45+08:00' AS "v3" FROM "default"."T" AS "t";

--#[timestamp-literal-00]
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45+08:00' AS "v1", TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45-07:00' AS "v2" FROM "default"."T" AS "t";

--#[time-compare-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE TIME '12:30:45' > TIME '10:15:30';

--#[extract-00]
SELECT EXTRACT(YEAR FROM "t"['timestamp_1']) AS "y", EXTRACT(MONTH FROM "t"['timestamp_1']) AS "m", EXTRACT(DAY FROM "t"['timestamp_1']) AS "d" FROM "default"."T" AS "t";

--#[extract-01]
SELECT EXTRACT(HOUR FROM TIME '12:30:45') AS "h", EXTRACT(MINUTE FROM TIME '12:30:45') AS "m" FROM "default"."T" AS "t";

--#[extract-02]
SELECT EXTRACT(SECOND FROM "t"['timestamp_1']) AS "s" FROM "default"."T" AS "t";

--#[extract-03]
SELECT EXTRACT(YEAR FROM "t"['timestamp_1']) AS "y" FROM "default"."T" AS "t";
