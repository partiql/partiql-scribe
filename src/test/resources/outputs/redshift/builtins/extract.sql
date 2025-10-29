-- NOTE: Redshift does not support top-level expression syntax.

-- EXTRACT expressions with SELECT FROM T
--#[extract-select-1]
SELECT EXTRACT(YEAR FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-2]
SELECT EXTRACT(MONTH FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-3]
SELECT EXTRACT(DAY FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-4]
SELECT EXTRACT(HOUR FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-5]
SELECT EXTRACT(MINUTE FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-6]
SELECT EXTRACT(SECOND FROM TIMESTAMP '2023-10-19 12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-7]
SELECT EXTRACT(YEAR FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-8]
SELECT EXTRACT(MONTH FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-9]
SELECT EXTRACT(DAY FROM DATE '2023-10-19') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-10]
SELECT EXTRACT(HOUR FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-11]
SELECT EXTRACT(MINUTE FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-12]
SELECT EXTRACT(SECOND FROM TIME '12:34:56') AS "_1" FROM "default"."T" AS "T";

--#[extract-select-13]
SELECT EXTRACT(YEAR FROM INTERVAL '5' YEAR) AS "_1" FROM "default"."T" AS "T";

--#[extract-select-14]
SELECT EXTRACT(MONTH FROM INTERVAL '3' MONTH) AS "_1" FROM "default"."T" AS "T";

--#[extract-select-15]
SELECT EXTRACT(DAY FROM INTERVAL '10' DAY) AS "_1" FROM "default"."T" AS "T";

--#[extract-select-16]
SELECT EXTRACT(HOUR FROM INTERVAL '8' HOUR) AS "_1" FROM "default"."T" AS "T";

--#[extract-select-17]
SELECT EXTRACT(MINUTE FROM INTERVAL '30' MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[extract-select-18]
SELECT EXTRACT(SECOND FROM INTERVAL '45' SECOND) AS "_1" FROM "default"."T" AS "T";

-- EXTRACT with column references
--#[extract-select-19]
SELECT EXTRACT(YEAR FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-20]
SELECT EXTRACT(MONTH FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-21]
SELECT EXTRACT(DAY FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-22]
SELECT EXTRACT(HOUR FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-23]
SELECT EXTRACT(MINUTE FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-24]
SELECT EXTRACT(SECOND FROM CAST("T_INTERVALS"."col_timestamp" AS TIMESTAMP)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-25]
SELECT EXTRACT(YEAR FROM CAST("T_INTERVALS"."col_date" AS DATE)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-26]
SELECT EXTRACT(MONTH FROM CAST("T_INTERVALS"."col_date" AS DATE)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-27]
SELECT EXTRACT(DAY FROM CAST("T_INTERVALS"."col_date" AS DATE)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-28]
SELECT EXTRACT(HOUR FROM CAST("T_INTERVALS"."col_time" AS TIME)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-29]
SELECT EXTRACT(MINUTE FROM CAST("T_INTERVALS"."col_time" AS TIME)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-30]
SELECT EXTRACT(SECOND FROM CAST("T_INTERVALS"."col_time" AS TIME)) AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-31]
SELECT EXTRACT(YEAR FROM "T_INTERVALS"."col_y2mon") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-32]
SELECT EXTRACT(MONTH FROM "T_INTERVALS"."col_y2mon") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-33]
SELECT EXTRACT(DAY FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-34]
SELECT EXTRACT(HOUR FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-35]
SELECT EXTRACT(MINUTE FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";

--#[extract-select-36]
SELECT EXTRACT(SECOND FROM "T_INTERVALS"."col_d2s") AS "_1" FROM "default"."T_INTERVALS" AS "T_INTERVALS";