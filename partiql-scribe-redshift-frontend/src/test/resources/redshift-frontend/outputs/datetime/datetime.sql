-- --#[time-cast-00] TODO(redshift)
[UNSUPPORTED{message="CAST of a character string to TIME"}];

-- --#[timestamp-cast-00] TODO(redshift)
[UNSUPPORTED{message="CAST of a character string to TIMESTAMPTZ"}];

-- --#[time-literal-00] TODO(redshift)
SELECT TIME '12:30:45' AS "v1", TIME '12:30:45.123456' AS "v2", TIME WITH TIME ZONE '12:30:45+08:00' AS "v3" FROM "default"."T" AS "t";

-- --#[timestamp-literal-00] TODO(redshift)
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 04:30:45+00:00' AS "v1", TIMESTAMP WITH TIME ZONE '2026-10-19 19:30:45+00:00' AS "v2" FROM "default"."T" AS "t";

-- --#[time-compare-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE TIME '12:30:45' > TIME '10:15:30';

-- --#[extract-00] TODO(redshift)
SELECT EXTRACT(YEAR FROM "t"['timestamp_1']) AS "y", EXTRACT(MONTH FROM "t"['timestamp_1']) AS "m", EXTRACT(DAY FROM "t"['timestamp_1']) AS "d" FROM "default"."T" AS "t";

-- --#[extract-01] TODO(redshift)
SELECT EXTRACT(HOUR FROM TIME '12:30:45') AS "h", EXTRACT(MINUTE FROM TIME '12:30:45') AS "m" FROM "default"."T" AS "t";

-- --#[extract-02] TODO(redshift)
SELECT CAST(CAST(EXTRACT(SECOND FROM "t"['timestamp_1']) * CAST(1000000000 AS DECIMAL(10,0)) AS BIGINT) / CAST(1000000000 AS BIGINT) AS INT) AS "s" FROM "default"."T" AS "t";

-- --#[extract-03] TODO(redshift)
SELECT EXTRACT(YEAR FROM "t"['timestamp_1']) AS "y" FROM "default"."T" AS "t";
