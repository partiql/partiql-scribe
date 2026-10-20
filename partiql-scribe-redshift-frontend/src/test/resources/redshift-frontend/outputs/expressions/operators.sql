--#[concat-00]
SELECT "t"['c'] || "t"['z'] AS "v" FROM "default"."T" AS "t";

--#[concat-01]
SELECT TRIM(TRAILING FROM "t_all_types"['col_char']) || "t_all_types"['col_string'] AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[concat-02]
[UNSUPPORTED{message="`||` of two CHAR values"}];

--#[concat-03]
SELECT ("t"['c'] || ' seats ') || CAST("t"['b'] AS STRING) AS "v" FROM "default"."T" AS "t";

--#[concat-04]
SELECT "t"['foo']['bar'] || CAST("t"['foo']['keep'] AS STRING) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[concat-05]
SELECT "t_all_types"['col_string'] || CAST("t_all_types"['col_date'] AS STRING) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[concat-06]
[UNSUPPORTED{message="implicit conversion of TIMESTAMP to VARCHAR in `||`"}];

--#[concat-07]
[UNSUPPORTED{message="implicit conversion of DOUBLE PRECISION to VARCHAR in `||`"}];

--#[concat-08]
[UNSUPPORTED{message="implicit conversion of a DECIMAL expression to VARCHAR in `||`"}];

--#[concat-09]
[UNSUPPORTED{message="`||` without a character string operand"}];

--#[arithmetic-00]
SELECT ("t"['b'] * 2) + 1 AS "v1", "t"['b'] / 2 AS "v2", "t"['b'] % -3 AS "v3", -7 / 2 AS "v4" FROM "default"."T" AS "t";

--#[arithmetic-01]
SELECT ("t"['foo']['keep'] * CAST(2 AS DECIMAL(10,0))) + CAST(1 AS DECIMAL(10,0)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[arithmetic-02]
SELECT "t"['col_float64'] * CAST("t"['col_decimal'] AS DOUBLE PRECISION) AS "v1", "t"['col_float64'] / CAST("t"['col_int32'] AS DOUBLE PRECISION) AS "v2" FROM "default"."T_ALL_TYPES" AS "t";

--#[arithmetic-03]
SELECT CAST("t"['col_float32'] AS DOUBLE PRECISION) + CAST(1 AS DOUBLE PRECISION) AS "v1", CAST("t"['col_float32'] AS DOUBLE PRECISION) * CAST("t"['col_decimal'] AS DOUBLE PRECISION) AS "v2", CAST("t"['col_float32'] AS DOUBLE PRECISION) - "t"['col_float64'] AS "v3" FROM "default"."T_ALL_TYPES" AS "t";

--#[arithmetic-04]
[UNSUPPORTED{message="arithmetic on two REAL operands"}];

--#[arithmetic-05]
[UNSUPPORTED{message="`%` with a floating-point operand"}];

--#[arithmetic-06]
[UNSUPPORTED{message="DECIMAL multiplication with result scale above 38"}];

--#[arithmetic-07]
[UNSUPPORTED{message="arithmetic with a SUPER operand"}];

--#[arithmetic-08]
[UNSUPPORTED{message="arithmetic with a character string operand"}];

--#[decimal-division-00]
SELECT CAST("t"['b'] AS DECIMAL(10,0)) / 100.0 AS "v" FROM "default"."T" AS "t";

--#[decimal-division-05]
SELECT "t"['foo']['keep'] / CAST(8 AS DECIMAL(10,0)) AS "v1", "t"['foo']['keep'] / 0.04 AS "v2" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[decimal-division-01]
[UNSUPPORTED{message="DECIMAL division"}];

--#[decimal-division-02]
[UNSUPPORTED{message="DECIMAL division"}];

--#[decimal-division-03]
[UNSUPPORTED{message="DECIMAL division"}];

--#[decimal-division-04]
SELECT 5. / 2. AS "v" FROM "default"."T" AS "t";

--#[unary-00]
SELECT CAST(-"t"['col_decimal'] AS DECIMAL(38,0)) + CAST(1 AS DECIMAL(10,0)) AS "v1", "t"['col_decimal'] AS "v2", -"t"['col_int32'] AS "v3" FROM "default"."T_ALL_TYPES" AS "t";

--#[unary-01]
[UNSUPPORTED{message="arithmetic with a SUPER operand"}];

--#[abs-operator-00]
SELECT CAST(ABS(-5.0) AS DECIMAL(2,1)) AS "v1", ABS("t"['b']) AS "v2", ABS(-5 + 1) AS "v3" FROM "default"."T" AS "t";

--#[abs-operator-01]
[UNSUPPORTED{message="arithmetic with a SUPER operand"}];

--#[abs-operator-02]
[UNSUPPORTED{message="`@` on an aggregate expression (PartiQL does not plan it as an argument of ABS)"}];

--#[bitwise-and-00]
SELECT "t"['b'] & 3 AS "v1", ("t"['b'] + 1) & 3 AS "v2" FROM "default"."T" AS "t";

--#[bitwise-and-01]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE ("t"['b'] & 1) = 1;

--#[bitwise-and-02]
[UNSUPPORTED{message="`&` with a non-integer operand"}];

--#[operator-00]
[UNSUPPORTED{message="operator `|`"}];

--#[operator-01]
[UNSUPPORTED{message="operator `<<`"}];

--#[operator-02]
[UNSUPPORTED{message="operator `>>`"}];

--#[operator-03]
[UNSUPPORTED{message="operator `^`"}];

--#[operator-04]
[UNSUPPORTED{message="operator `|/`"}];

--#[operator-05]
[UNSUPPORTED{message="operator `||/`"}];

--#[operator-06]
[UNSUPPORTED{message="operator `~`"}];

--#[operator-07]
SELECT "t"['c'] || "t"['z'] AS "v" FROM "default"."T" AS "t";
