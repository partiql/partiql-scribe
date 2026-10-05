-- --#[datetime-08] TODO(duckdb)
SELECT current_date() AS "CURRENT_DATE" FROM "default"."T" AS "T";

-- --#[datetime-09] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-10] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-11] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-12] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-13] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-14] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-15] TODO(duckdb)
SELECT date_diff('year', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-16] TODO(duckdb)
SELECT date_diff('month', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-17] TODO(duckdb)
SELECT date_diff('day', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-18] TODO(duckdb)
SELECT date_diff('hour', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-19] TODO(duckdb)
SELECT date_diff('minute', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-20] TODO(duckdb)
SELECT date_diff('second', "T"."timestamp_1", "T"."timestamp_2") AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-21] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-22] TODO(duckdb)
SELECT date_diff('second', TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') AS "_1" FROM "default"."T" AS "T";

-- --#[datetime-41] TODO(duckdb)
SELECT date_diff('second', "T"."col_time", "T"."col_time") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-42] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-43] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-44] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-45] TODO(duckdb)
SELECT date_diff('day', "T"."col_date", "T"."col_date") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-46] TODO(duckdb)
SELECT date_diff('day', CAST("T"."col_date" AS TIMESTAMP), "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-47] TODO(duckdb)
SELECT date_diff('day', CAST("T"."col_date" AS TIMESTAMP WITH TIME ZONE), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-48] TODO(duckdb)
SELECT date_diff('day', "T"."col_timestamp", CAST("T"."col_date" AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-49] TODO(duckdb)
SELECT date_diff('day', "T"."col_timestamp", "T"."col_timestamp") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-50] TODO(duckdb)
SELECT date_diff('day', CAST("T"."col_timestamp" AS TIMESTAMP WITH TIME ZONE), "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-51] TODO(duckdb)
SELECT date_diff('day', "T"."col_timestampz", CAST("T"."col_date" AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-52] TODO(duckdb)
SELECT date_diff('day', "T"."col_timestampz", CAST("T"."col_timestamp" AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-53] TODO(duckdb)
SELECT date_diff('day', "T"."col_timestampz", "T"."col_timestampz") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-54] TODO(duckdb)
SELECT date_diff('second', TIME '12:34:56', TIME '13:45:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-55] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-56] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-57] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION}];

-- --#[datetime-58] TODO(duckdb)
SELECT date_diff('day', DATE '2023-01-15', DATE '2023-12-25') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-59] TODO(duckdb)
SELECT date_diff('day', CAST(DATE '2023-01-15' AS TIMESTAMP), TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-60] TODO(duckdb)
SELECT date_diff('day', CAST(DATE '2023-01-15' AS TIMESTAMP WITH TIME ZONE), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-61] TODO(duckdb)
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00', CAST(DATE '2023-12-25' AS TIMESTAMP)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-62] TODO(duckdb)
SELECT date_diff('day', TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-63] TODO(duckdb)
SELECT date_diff('day', CAST(TIMESTAMP '2023-01-15 08:00:00' AS TIMESTAMP WITH TIME ZONE), TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-64] TODO(duckdb)
SELECT date_diff('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(DATE '2023-12-25' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-65] TODO(duckdb)
SELECT date_diff('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', CAST(TIMESTAMP '2023-12-25 10:30:00' AS TIMESTAMP WITH TIME ZONE)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- --#[datetime-66] TODO(duckdb)
SELECT date_diff('day', TIMESTAMPTZ '2023-01-15 08:00:00+08:00', TIMESTAMPTZ '2023-12-25 10:30:00+08:00') AS "_1" FROM "default"."T_ALL_TYPES" AS "T";
