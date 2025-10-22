-- NOTE: Trino does not support top-level expression syntax.

-- ABS expressions with SELECT FROM T
--#[abs-select-1]
SELECT ABS(-5) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-2]
SELECT ABS(5) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-3]
SELECT ABS(-3.14) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-4]
SELECT ABS(3.14) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-5]
SELECT ABS(0) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-6]
SELECT ABS(0) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-7]
SELECT ABS(-2147483647) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-8]
SELECT ABS(2147483647) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-9]
SELECT ABS(-9223372036854775807) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-10]
SELECT ABS(9223372036854775807) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-11]
SELECT ABS(-1.7976931348623157E308) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-12]
SELECT ABS(1.7976931348623157E308) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-13]
SELECT ABS(-3.4028235E38) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-14]
SELECT ABS(3.4028235E38) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-15]
SELECT ABS(CAST(-42 AS TINYINT)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-16]
SELECT ABS(CAST(-1000 AS SMALLINT)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-17]
SELECT ABS(CAST(-100000 AS BIGINT)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-18]
SELECT ABS(CAST(-99.99 AS DECIMAL(10,2))) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-19]
SELECT ABS(CAST(-123.456 AS REAL)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-20]
SELECT ABS(NULL) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- ABS with column references
--#[abs-select-21]
SELECT ABS("T"."col_int32") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-22]
SELECT ABS("T"."col_float32") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-23]
SELECT ABS("T"."col_float64") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[abs-select-24]
SELECT ABS("T"."col_decimal") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- ABS with INTERVAL expressions in SELECT FROM T
-- Trino does not support ABS(INTERVAL)