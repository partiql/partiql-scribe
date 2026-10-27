--#[timestamptz-literal-00]
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 04:30:45+00:00' AS "v" FROM "default"."T" AS "t";

--#[timestamptz-literal-01]
SELECT TIMESTAMP WITH TIME ZONE '1997-12-17 15:37:16+00:00' AS "v" FROM "default"."T" AS "t";

--#[timestamptz-literal-02]
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45+00:00' AS "v" FROM "default"."T" AS "t";

--#[timestamptz-literal-03]
SELECT TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45.5+00:00' AS "v1", TIMESTAMP WITH TIME ZONE '2026-10-19 12:30:45+00:00' AS "v2" FROM "default"."T" AS "t";

--#[timestamptz-literal-04]
SELECT TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00:00' AS "v" FROM "default"."T" AS "t";

--#[timestamptz-literal-05]
[UNSUPPORTED{message="TIMESTAMPTZ literal format 'Jun 1,2008 09:59:59 EST'"}];

--#[timestamptz-literal-06]
[UNSUPPORTED{message="TIMESTAMPTZ literal format '2026-10-19 12:30:45+16:00'"}];

--#[timestamptz-literal-07]
SELECT TIMESTAMP WITH TIME ZONE '0000-12-31 16:00:00+00:00' AS "v1", TIMESTAMP WITH TIME ZONE '2027-01-01 00:30:00+00:00' AS "v2" FROM "default"."T" AS "t";

--#[date-literal-00]
SELECT DATE '2008-02-15' AS "v1", DATE '2026-01-09' AS "v2" FROM "default"."T" AS "t";

--#[date-literal-01]
SELECT DATE '2000-01-31' AS "v1", DATE '2000-01-31' AS "v2", DATE '2000-01-31' AS "v3", DATE '1999-01-08' AS "v4" FROM "default"."T" AS "t";

--#[date-literal-02]
SELECT DATE '2022-05-02' AS "v" FROM "default"."T" AS "t";

--#[date-literal-03]
[UNSUPPORTED{message="DATE literal format '1/8/1999'"}];

--#[date-literal-04]
[UNSUPPORTED{message="DATE literal format 'today'"}];

--#[timestamp-literal-01]
SELECT TIMESTAMP '2001-06-06 00:00:00' AS "v1", TIMESTAMP '2008-02-15 04:05:06' AS "v2", TIMESTAMP '2008-02-15 16:05:00' AS "v3" FROM "default"."T" AS "t";

--#[timestamp-literal-02]
SELECT TIMESTAMP '2008-06-01 09:59:59' AS "v" FROM "default"."T" AS "t";

--#[timestamp-literal-03]
SELECT TIMESTAMP '1970-01-01 00:00:00' AS "v" FROM "default"."T" AS "t";

--#[timestamp-literal-04]
[UNSUPPORTED{message="TIMESTAMP literal format '2008-02-15T04:05:06'"}];

--#[timestamp-literal-05]
[UNSUPPORTED{message="TIMESTAMP literal format '2008-02-15 24:00:00'"}];

--#[time-literal-01]
SELECT TIME '04:05:00' AS "v1", TIME '16:05:00' AS "v2", TIME '04:05:06' AS "v3", TIME '00:30:00' AS "v4", TIME '12:30:00' AS "v5" FROM "default"."T" AS "t";

--#[time-literal-02]
[UNSUPPORTED{message="TIME literal format '24:00:00'"}];

--#[time-literal-03]
[UNSUPPORTED{message="TIME literal format '12:30:45+08:00'"}];

--#[timetz-literal-00]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' AS "v1", TIME WITH TIME ZONE '12:30:45+00:00' AS "v2", TIME WITH TIME ZONE '12:30:45+00:00' AS "v3" FROM "default"."T" AS "t";

--#[timetz-literal-01]
SELECT TIME WITH TIME ZONE '12:30:45+00:00' AS "v" FROM "default"."T" AS "t";

--#[timetz-literal-02]
[UNSUPPORTED{message="comparison with a TIMETZ literal whose offset is not UTC"}];

--#[timetz-literal-03]
SELECT "t"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t" WHERE "t"['col_timez'] IN (TIME WITH TIME ZONE '04:30:45+00:00', TIME WITH TIME ZONE '05:30:45+00:00');
