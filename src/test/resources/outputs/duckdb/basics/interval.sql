-- INTERVAL <interval string> <single datetime field>
--#[interval-00]
SELECT INTERVAL '10' YEAR AS "i" FROM "default"."T" AS "T";

--#[interval-01]
SELECT INTERVAL '-10' YEAR AS "i" FROM "default"."T" AS "T";

-- --#[interval-02] TODO(duckdb)
SELECT INTERVAL '10' YEAR AS "i" FROM "default"."T" AS "T";

-- --#[interval-03] TODO(duckdb)
SELECT INTERVAL '-10' YEAR AS "i" FROM "default"."T" AS "T";

--#[interval-04]
SELECT INTERVAL '10' MONTH AS "i" FROM "default"."T" AS "T";

--#[interval-05]
SELECT INTERVAL '-10' MONTH AS "i" FROM "default"."T" AS "T";

-- --#[interval-06] TODO(duckdb)
SELECT INTERVAL '10' MONTH AS "i" FROM "default"."T" AS "T";

-- --#[interval-07] TODO(duckdb)
SELECT INTERVAL '-10' MONTH AS "i" FROM "default"."T" AS "T";

--#[interval-08]
SELECT INTERVAL '10' DAY AS "i" FROM "default"."T" AS "T";

--#[interval-09]
SELECT INTERVAL '-10' DAY AS "i" FROM "default"."T" AS "T";

-- --#[interval-10] TODO(duckdb)
SELECT INTERVAL '10' DAY AS "i" FROM "default"."T" AS "T";

-- --#[interval-11] TODO(duckdb)
SELECT INTERVAL '-10' DAY AS "i" FROM "default"."T" AS "T";

--#[interval-12]
SELECT INTERVAL '10' HOUR AS "i" FROM "default"."T" AS "T";

--#[interval-13]
SELECT INTERVAL '-10' HOUR AS "i" FROM "default"."T" AS "T";

-- --#[interval-14] TODO(duckdb)
SELECT INTERVAL '10' HOUR AS "i" FROM "default"."T" AS "T";

-- --#[interval-15] TODO(duckdb)
SELECT INTERVAL '-10' HOUR AS "i" FROM "default"."T" AS "T";

--#[interval-16]
SELECT INTERVAL '10' MINUTE AS "i" FROM "default"."T" AS "T";

--#[interval-17]
SELECT INTERVAL '-10' MINUTE AS "i" FROM "default"."T" AS "T";

-- --#[interval-18] TODO(duckdb)
SELECT INTERVAL '10' MINUTE AS "i" FROM "default"."T" AS "T";

-- --#[interval-19] TODO(duckdb)
SELECT INTERVAL '-10' MINUTE AS "i" FROM "default"."T" AS "T";

--#[interval-20]
SELECT INTERVAL '10' SECOND AS "i" FROM "default"."T" AS "T";

--#[interval-21]
SELECT INTERVAL '-10' SECOND AS "i" FROM "default"."T" AS "T";

-- --#[interval-22] TODO(duckdb)
SELECT INTERVAL '10' SECOND AS "i" FROM "default"."T" AS "T";

-- --#[interval-23] TODO(duckdb)
SELECT INTERVAL '-10' SECOND AS "i" FROM "default"."T" AS "T";

-- --#[interval-24] TODO(duckdb)
SELECT INTERVAL '10.234' SECOND AS "i" FROM "default"."T" AS "T";

-- --#[interval-25] TODO(duckdb)
SELECT INTERVAL '-10.234' SECOND AS "i" FROM "default"."T" AS "T";

-- <start field> TO <end field>
-- --#[interval-26] TODO(duckdb)
SELECT INTERVAL '10 years 3 months' AS "i" FROM "default"."T" AS "T";

-- --#[interval-27] TODO(duckdb)
SELECT INTERVAL '-10 years -3 months' AS "i" FROM "default"."T" AS "T";

-- --#[interval-28] TODO(duckdb)
SELECT INTERVAL '10 years 3 months' AS "i" FROM "default"."T" AS "T";

-- --#[interval-29] TODO(duckdb)
SELECT INTERVAL '-10 years -3 months' AS "i" FROM "default"."T" AS "T";

-- --#[interval-30] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours' AS "i" FROM "default"."T" AS "T";

-- --#[interval-31] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours' AS "i" FROM "default"."T" AS "T";

-- --#[interval-32] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours' AS "i" FROM "default"."T" AS "T";

-- --#[interval-33] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours' AS "i" FROM "default"."T" AS "T";

-- --#[interval-34] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-35] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-36] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-37] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-38] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes 5 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-39] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes -5 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-40] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes 5 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-41] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes -5 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-42] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes 5.678 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-43] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes -5.678 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-44] TODO(duckdb)
SELECT INTERVAL '10 days 3 hours 4 minutes 5.678 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-45] TODO(duckdb)
SELECT INTERVAL '-10 days -3 hours -4 minutes -5.678 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-46] TODO(duckdb)
SELECT INTERVAL '3 hours 4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-47] TODO(duckdb)
SELECT INTERVAL '-3 hours -4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-48] TODO(duckdb)
SELECT INTERVAL '3 hours 4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-49] TODO(duckdb)
SELECT INTERVAL '-3 hours -4 minutes' AS "i" FROM "default"."T" AS "T";

-- --#[interval-50] TODO(duckdb)
SELECT INTERVAL '2 hours 3 minutes 4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-51] TODO(duckdb)
SELECT INTERVAL '-2 hours -3 minutes -4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-52] TODO(duckdb)
SELECT INTERVAL '2 hours 3 minutes 4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-53] TODO(duckdb)
SELECT INTERVAL '-2 hours -3 minutes -4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-54] TODO(duckdb)
SELECT INTERVAL '2 hours 3 minutes 4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-55] TODO(duckdb)
SELECT INTERVAL '-2 hours -3 minutes -4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-56] TODO(duckdb)
SELECT INTERVAL '2 hours 3 minutes 4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-57] TODO(duckdb)
SELECT INTERVAL '-2 hours -3 minutes -4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-58] TODO(duckdb)
SELECT INTERVAL '3 minutes 4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-59] TODO(duckdb)
SELECT INTERVAL '-3 minutes -4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-60] TODO(duckdb)
SELECT INTERVAL '3 minutes 4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-61] TODO(duckdb)
SELECT INTERVAL '-3 minutes -4 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-62] TODO(duckdb)
SELECT INTERVAL '3 minutes 4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-63] TODO(duckdb)
SELECT INTERVAL '-3 minutes -4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-64] TODO(duckdb)
SELECT INTERVAL '3 minutes 4.567 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-65] TODO(duckdb)
SELECT INTERVAL '-3 minutes -4.567 seconds' AS "i" FROM "default"."T" AS "T";
-- Additional DAY TO SECOND precision tests
-- --#[interval-66] TODO(duckdb)
SELECT INTERVAL '2 days 3 hours 4 minutes 5.000006 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-67] TODO(duckdb)
SELECT INTERVAL '-2 days -3 hours -4 minutes -5.000006 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-68] TODO(duckdb)
SELECT INTERVAL '2 days 3 hours 4 minutes 5.000006000 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-69] TODO(duckdb)
SELECT INTERVAL '-2 days -3 hours -4 minutes -5.000006000 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-70] TODO(duckdb)
SELECT INTERVAL '2 days 3 hours 4 minutes 5.000 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-71] TODO(duckdb)
SELECT INTERVAL '-2 days -3 hours -4 minutes -5.000 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-72] TODO(duckdb)
SELECT INTERVAL '2 days 3 hours 4 minutes 5.000000 seconds' AS "i" FROM "default"."T" AS "T";

-- --#[interval-73] TODO(duckdb)
SELECT INTERVAL '-2 days -3 hours -4 minutes -5.000000 seconds' AS "i" FROM "default"."T" AS "T";

-- Additional large value test cases
--#[interval-74]
SELECT INTERVAL '30' MONTH AS "i" FROM "default"."T" AS "T";

-- --#[interval-75] TODO(duckdb)
SELECT INTERVAL '100' HOUR AS "i" FROM "default"."T" AS "T";

-- --#[interval-76] TODO(duckdb)
SELECT INTERVAL '2000' MINUTE AS "i" FROM "default"."T" AS "T";

-- --#[interval-77] TODO(duckdb)
SELECT INTERVAL '100000' SECOND AS "i" FROM "default"."T" AS "T";