-- Top-level ABS expressions
--#[abs-01]
ABS(-5);

--#[abs-02]
ABS(5);

--#[abs-03]
ABS(-3.14);

--#[abs-04]
ABS(3.14);

--#[abs-05]
ABS(0);

--#[abs-06]
ABS(0);

--#[abs-07]
ABS(-2147483647);

--#[abs-08]
ABS(2147483647);

--#[abs-09]
ABS(-9223372036854775807);

--#[abs-10]
ABS(9223372036854775807);

--#[abs-11]
ABS(-1.7976931348623157E308);

--#[abs-12]
ABS(1.7976931348623157E308);

--#[abs-13]
ABS(-3.4028235E38);

--#[abs-14]
ABS(3.4028235E38);

--#[abs-15]
ABS(CAST(-42 AS TINYINT));

--#[abs-16]
ABS(CAST(-1000 AS SMALLINT));

--#[abs-17]
ABS(CAST(-100000 AS BIGINT));

--#[abs-18]
ABS(CAST(-99.99 AS DECIMAL(10,2)));

--#[abs-19]
ABS(CAST(-123.456 AS REAL));

--#[abs-20]
ABS(NULL);

-- ABS expressions with SELECT FROM T
--#[abs-select-1]
SELECT ABS(-5) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-2]
SELECT ABS(5) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-3]
SELECT ABS(-3.14) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-4]
SELECT ABS(3.14) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-5]
SELECT ABS(0) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-6]
SELECT ABS(0) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-7]
SELECT ABS(-2147483647) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-8]
SELECT ABS(2147483647) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-9]
SELECT ABS(-9223372036854775807) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-10]
SELECT ABS(9223372036854775807) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-11]
SELECT ABS(-1.7976931348623157E308) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-12]
SELECT ABS(1.7976931348623157E308) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-13]
SELECT ABS(-3.4028235E38) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-14]
SELECT ABS(3.4028235E38) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-15]

SELECT ABS(CAST(-42 AS TINYINT)) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-16]
SELECT ABS(CAST(-1000 AS SMALLINT)) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-17]
SELECT ABS(CAST(-100000 AS BIGINT)) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-18]
SELECT ABS(CAST(-99.99 AS DECIMAL(10,2))) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-19]
SELECT ABS(CAST(-123.456 AS REAL)) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-20]
SELECT ABS(NULL) AS "_1" FROM "default"."T" AS "T";

-- ABS with column references
--#[abs-select-21]
SELECT ABS("T"['col_int32']) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-22]
SELECT ABS("T"['col_float32']) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-23]
SELECT ABS("T"['col_float64']) AS "_1" FROM "default"."T" AS "T";

--#[abs-select-24]
SELECT ABS("T"['col_decimal']) AS "_1" FROM "default"."T" AS "T";

-- ABS with INTERVAL expressions
--#[abs-interval-1]
ABS(INTERVAL '-1' YEAR);

--#[abs-interval-2]
ABS(INTERVAL '1' YEAR);

--#[abs-interval-3]
ABS(INTERVAL '-5' MONTH);

--#[abs-interval-4]
ABS(INTERVAL '5' MONTH);

--#[abs-interval-5]
ABS(INTERVAL '-10' DAY);

--#[abs-interval-6]
ABS(INTERVAL '10' DAY);

--#[abs-interval-7]
ABS(INTERVAL '-2' HOUR);

--#[abs-interval-8]
ABS(INTERVAL '2' HOUR);

--#[abs-interval-9]
ABS(INTERVAL '-30' MINUTE);

--#[abs-interval-10]
ABS(INTERVAL '30' MINUTE);

--#[abs-interval-11]
ABS(INTERVAL '-45' SECOND);

--#[abs-interval-12]
ABS(INTERVAL '45' SECOND);

--#[abs-interval-13]
ABS(INTERVAL '-1-6' YEAR TO MONTH);

--#[abs-interval-14]
ABS(INTERVAL '1-6' YEAR TO MONTH);

--#[abs-interval-15]
ABS(INTERVAL '-5 10' DAY TO HOUR);

--#[abs-interval-16]
ABS(INTERVAL '5 10' DAY TO HOUR);

--#[abs-interval-17]
ABS(INTERVAL '-5 10:30' DAY TO MINUTE);

--#[abs-interval-18]
ABS(INTERVAL '5 10:30' DAY TO MINUTE);

--#[abs-interval-19]
ABS(INTERVAL '-5 10:30:45' DAY TO SECOND);

--#[abs-interval-20]
ABS(INTERVAL '5 10:30:45' DAY TO SECOND);

--#[abs-interval-21]
ABS(INTERVAL '-10:30' HOUR TO MINUTE);

--#[abs-interval-22]
ABS(INTERVAL '10:30' HOUR TO MINUTE);

--#[abs-interval-23]
ABS(INTERVAL '-10:30:45' HOUR TO SECOND);

--#[abs-interval-24]
ABS(INTERVAL '10:30:45' HOUR TO SECOND);

--#[abs-interval-25]
ABS(INTERVAL '-30:45' MINUTE TO SECOND);

--#[abs-interval-26]
ABS(INTERVAL '30:45' MINUTE TO SECOND);

-- ABS with INTERVAL expressions in SELECT FROM T
--#[abs-interval-select-1]
SELECT ABS(INTERVAL '-1' YEAR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-2]
SELECT ABS(INTERVAL '1' YEAR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-3]
SELECT ABS(INTERVAL '-5' MONTH) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-4]
SELECT ABS(INTERVAL '5' MONTH) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-5]
SELECT ABS(INTERVAL '-10' DAY) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-6]
SELECT ABS(INTERVAL '10' DAY) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-7]
SELECT ABS(INTERVAL '-2' HOUR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-8]
SELECT ABS(INTERVAL '2' HOUR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-9]
SELECT ABS(INTERVAL '-30' MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-10]
SELECT ABS(INTERVAL '30' MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-11]
SELECT ABS(INTERVAL '-45' SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-12]
SELECT ABS(INTERVAL '45' SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-13]
SELECT ABS(INTERVAL '-1-6' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-14]
SELECT ABS(INTERVAL '1-6' YEAR TO MONTH) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-15]
SELECT ABS(INTERVAL '-5 10' DAY TO HOUR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-16]
SELECT ABS(INTERVAL '5 10' DAY TO HOUR) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-17]
SELECT ABS(INTERVAL '-5 10:30' DAY TO MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-18]
SELECT ABS(INTERVAL '5 10:30' DAY TO MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-19]
SELECT ABS(INTERVAL '-5 10:30:45' DAY TO SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-20]
SELECT ABS(INTERVAL '5 10:30:45' DAY TO SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-21]
SELECT ABS(INTERVAL '-10:30' HOUR TO MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-22]
SELECT ABS(INTERVAL '10:30' HOUR TO MINUTE) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-23]
SELECT ABS(INTERVAL '-10:30:45' HOUR TO SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-24]
SELECT ABS(INTERVAL '10:30:45' HOUR TO SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-25]
SELECT ABS(INTERVAL '-30:45' MINUTE TO SECOND) AS "_1" FROM "default"."T" AS "T";

--#[abs-interval-select-26]
SELECT ABS(INTERVAL '30:45' MINUTE TO SECOND) AS "_1" FROM "default"."T" AS "T";

-- ABS with INTERVAL column references from T_INTERVALS
--#[abs-interval-select-27]
SELECT ABS("T"['col_y']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-28]
SELECT ABS("T"['col_mon']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-29]
SELECT ABS("T"['col_d']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-30]
SELECT ABS("T"['col_h']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-31]
SELECT ABS("T"['col_min']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-32]
SELECT ABS("T"['col_s']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-33]
SELECT ABS("T"['col_y2mon']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-34]
SELECT ABS("T"['col_d2h']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-35]
SELECT ABS("T"['col_d2min']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-36]
SELECT ABS("T"['col_d2s']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-37]
SELECT ABS("T"['col_h2min']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-38]
SELECT ABS("T"['col_h2s']) AS "_1" FROM "default"."T_INTERVALS" AS "T";

--#[abs-interval-select-39]
SELECT ABS("T"['col_m2s']) AS "_1" FROM "default"."T_INTERVALS" AS "T";