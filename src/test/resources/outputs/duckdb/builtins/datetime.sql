--#[datetime-08]
SELECT current_date() AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT date_add("T"."timestamp_1", to_seconds(5)) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT date_add("T"."timestamp_1", to_minutes(5)) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT date_add("T"."timestamp_1", to_hours(5)) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT CAST(date_add(current_date(), to_days(5)) AS DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT CAST(date_add(current_date(), to_months(5)) AS DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT CAST(date_add(current_date(), to_years(5)) AS DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT date_sub('year', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-16]
SELECT date_sub('month', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-17]
SELECT date_sub('day', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-18]
SELECT date_sub('hour', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-19]
SELECT date_sub('minute', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-20]
SELECT date_sub('second', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

--#[datetime-21]
SELECT date_add(TIMESTAMP '2017-01-02 03:04:05.006', to_seconds(1)) AS "_1" FROM "default"."T" AS "T";

--#[datetime-22]
SELECT date_sub('second', TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";

--#[datetime-41]
SELECT date_sub('second', "T"."col_time", "T"."col_time") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-42]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-43]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-44]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-45]
SELECT date_sub('day', "T"."col_date", "T"."col_date") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-46]
SELECT date_sub('day', CAST("T"."col_date" AS TIMESTAMP), "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-47]
SELECT date_sub('day', CAST("T"."col_date" AS TIMESTAMP WITH TIME ZONE), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-48]
SELECT date_sub('day', "T"."col_timestamp", CAST("T"."col_date" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-49]
SELECT date_sub('day', "T"."col_timestamp", "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-50]
SELECT date_sub('day', CAST("T"."col_timestamp" AS TIMESTAMP WITH TIME ZONE), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-51]
SELECT date_sub('day', "T"."col_timestampz", CAST("T"."col_date" AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-52]
SELECT date_sub('day', "T"."col_timestampz", CAST("T"."col_timestamp" AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-53]
SELECT date_sub('day', "T"."col_timestampz", "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-54]
SELECT date_sub('second', TIME '12:34:56', TIME '13:45:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-55]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-56]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-57]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[datetime-58]
SELECT date_sub('day', DATE '2023-01-15', DATE '2023-12-25') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-59]
SELECT date_sub('day', CAST(DATE '2023-01-15' AS TIMESTAMP), TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-60]
SELECT date_sub('day', CAST(DATE '2023-01-15' AS TIMESTAMP WITH TIME ZONE), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-61]
SELECT date_sub('day', TIMESTAMP '2023-01-15 08:00:00', CAST(DATE '2023-12-25' AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-62]
SELECT date_sub('day', TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-63]
SELECT date_sub('day', CAST(TIMESTAMP '2023-01-15 08:00:00' AS TIMESTAMP WITH TIME ZONE), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-64]
SELECT date_sub('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(DATE '2023-12-25' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-65]
SELECT date_sub('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(TIMESTAMP '2023-12-25 10:30:00' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-66]
SELECT date_sub('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[datetime-67]
SELECT timezone('UTC', current_timestamp) AS "u" FROM "default"."T" AS "T";

--#[datetime-68]
SELECT "T"."b" AS "b" FROM "default"."T" AS "T" WHERE "T"."timestamp_1" > date_add(CAST(timezone('UTC', current_timestamp) AS TIMESTAMP), to_days(-30));

--#[datetime-69]
SELECT date_add("T"."timestamp_1", to_days("T"."b")) AS "d" FROM "default"."T" AS "T";
