-- Top-level EXTRACT expressions (extract-01 to extract-48)
-- Trino does not support top-level expressions

-- EXTRACT expressions with SELECT FROM T - TIMESTAMP
--#[extract-49]
SELECT EXTRACT(YEAR FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-50]
SELECT EXTRACT(MONTH FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-51]
SELECT EXTRACT(DAY FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-52]
SELECT EXTRACT(HOUR FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-53]
SELECT EXTRACT(MINUTE FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-54]
SELECT EXTRACT(SECOND FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

-- EXTRACT expressions with SELECT FROM T - DATE
--#[extract-55]
SELECT EXTRACT(YEAR FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

--#[extract-56]
SELECT EXTRACT(MONTH FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

--#[extract-57]
SELECT EXTRACT(DAY FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

-- EXTRACT expressions with SELECT FROM T - TIME
--#[extract-58]
SELECT EXTRACT(HOUR FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-59]
SELECT EXTRACT(MINUTE FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-60]
SELECT EXTRACT(SECOND FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

-- EXTRACT expressions with SELECT FROM T - single INTERVAL types
--#[extract-61]
SELECT EXTRACT(YEAR FROM INTERVAL '5' YEAR) AS "_1" FROM "default"."T" AS "T";

--#[extract-62]
SELECT EXTRACT(MONTH FROM INTERVAL '3' MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-63]
SELECT EXTRACT(DAY FROM INTERVAL '10' DAY) AS "_1" FROM "default"."T" AS "T";

--#[extract-64]
SELECT EXTRACT(HOUR FROM INTERVAL '8' HOUR) AS "_1" FROM "default"."T" AS "T";

--#[extract-65]
SELECT EXTRACT(MINUTE FROM INTERVAL '30' MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[extract-66]
SELECT EXTRACT(SECOND FROM INTERVAL '45' SECOND) AS "_1" FROM "default"."T" AS "T";

-- EXTRACT expressions with SELECT FROM T - compound INTERVAL types
--#[extract-67]
SELECT EXTRACT(YEAR FROM INTERVAL '2-6' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-68]
SELECT EXTRACT(MONTH FROM INTERVAL '2-6' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-69]
SELECT EXTRACT(DAY FROM INTERVAL '5 12' DAY TO HOUR) AS "_1" FROM "default"."T" AS "T";

--#[extract-70]
SELECT EXTRACT(HOUR FROM INTERVAL '5 12' DAY TO HOUR) AS "_1" FROM "default"."T" AS "T";

--#[extract-71]
SELECT EXTRACT(HOUR FROM INTERVAL '14:30' HOUR TO MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[extract-72]
SELECT EXTRACT(MINUTE FROM INTERVAL '14:30' HOUR TO MINUTE) AS "_1" FROM "default"."T" AS "T";

-- EXTRACT expressions with SELECT FROM T - negative INTERVAL types
--#[extract-73]
SELECT EXTRACT(YEAR FROM INTERVAL '-3' YEAR) AS "_1" FROM "default"."T" AS "T";

--#[extract-74]
SELECT EXTRACT(MONTH FROM INTERVAL '-8' MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-75]
SELECT EXTRACT(DAY FROM INTERVAL '-15' DAY) AS "_1" FROM "default"."T" AS "T";

--#[extract-76]
SELECT EXTRACT(YEAR FROM INTERVAL '-1-3' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-77]
SELECT EXTRACT(MONTH FROM INTERVAL '-1-3' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

-- EXTRACT with column references - TIMESTAMP columns
--#[extract-78]
SELECT EXTRACT(YEAR FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-79]
SELECT EXTRACT(MONTH FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-80]
SELECT EXTRACT(DAY FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-81]
SELECT EXTRACT(HOUR FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-82]
SELECT EXTRACT(MINUTE FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-83]
SELECT EXTRACT(SECOND FROM "T_INTERVALS"."col_timestamp") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

-- EXTRACT with column references - DATE columns
--#[extract-84]
SELECT EXTRACT(YEAR FROM "T_INTERVALS"."col_date") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-85]
SELECT EXTRACT(MONTH FROM "T_INTERVALS"."col_date") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-86]
SELECT EXTRACT(DAY FROM "T_INTERVALS"."col_date") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

-- EXTRACT with column references - TIME columns
--#[extract-87]
SELECT EXTRACT(HOUR FROM "T_INTERVALS"."col_time") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-88]
SELECT EXTRACT(MINUTE FROM "T_INTERVALS"."col_time") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-89]
SELECT EXTRACT(SECOND FROM "T_INTERVALS"."col_time") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

-- EXTRACT with column references - INTERVAL columns
--#[extract-90]
SELECT EXTRACT(YEAR FROM "T_INTERVALS"."col_y2mon") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-91]
SELECT EXTRACT(MONTH FROM "T_INTERVALS"."col_y2mon") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-92]
SELECT EXTRACT(DAY FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-93]
SELECT EXTRACT(HOUR FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-94]
SELECT EXTRACT(MINUTE FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-95]
SELECT EXTRACT(SECOND FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";